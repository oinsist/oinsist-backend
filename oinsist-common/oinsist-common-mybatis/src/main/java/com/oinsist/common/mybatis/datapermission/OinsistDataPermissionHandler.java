package com.oinsist.common.mybatis.datapermission;

import com.oinsist.common.mybatis.annotation.DataPermission;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据权限 SQL 条件处理器
 * <p>
 * 核心职责：根据当前用户的角色数据范围，动态生成 SQL WHERE 条件表达式。
 * 实现 MyBatis-Plus 的 {@link DataPermissionHandler} 接口，
 * 由 {@link com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor}
 * 在每次 SQL 执行前调用。
 * </p>
 *
 * <h3>为什么用拦截器统一处理而不是在 Service 层手写条件</h3>
 * <ol>
 *     <li><b>集中管控</b>：权限逻辑集中在一处，避免分散到各 Service 导致遗漏或不一致</li>
 *     <li><b>透明无侵入</b>：业务代码无需感知数据权限的存在，只需在 Mapper 方法上加 @DataPermission 注解</li>
 *     <li><b>可维护性</b>：修改权限规则时只需改此处理器，不用逐个排查 Service</li>
 *     <li><b>SQL 级过滤</b>：在数据库层面完成过滤，比查出全量数据后内存过滤性能好得多</li>
 * </ol>
 *
 * <h3>执行时序</h3>
 * <pre>
 * Mapper 方法调用
 *   → MybatisPlusInterceptor 拦截
 *     → DataPermissionInterceptor.beforeQuery()
 *       → OinsistDataPermissionHandler.getSqlSegment()  ← 本类
 *         → 读取 @DataPermission 注解
 *         → 调用 DataPermissionProvider 获取用户权限上下文
 *         → 构建并返回 WHERE 条件表达式
 *     → 将条件表达式拼入原始 SQL
 *   → 执行改写后的 SQL
 * </pre>
 *
 * @author oinsist
 */
public class OinsistDataPermissionHandler implements DataPermissionHandler {

    private final DataPermissionProvider provider;

    public OinsistDataPermissionHandler(DataPermissionProvider provider) {
        this.provider = provider;
    }

    /**
     * 获取数据权限 SQL 条件
     * <p>
     * MyBatis-Plus DataPermissionInterceptor 在每条 SQL 执行前回调此方法，
     * 返回的 Expression 会被追加到原始 SQL 的 WHERE 子句中。
     * </p>
     *
     * @param where             原始 WHERE 条件表达式（可能为 null，表示无 WHERE 子句）
     * @param mappedStatementId Mapper 方法的全限定名（如 com.oinsist.system.mapper.SysUserMapper.selectPage）
     * @return 追加数据权限条件后的 WHERE 表达式；返回原始 where 表示不追加任何条件
     */
    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        // 1. 获取 Mapper 方法上的 @DataPermission 注解，未标注的方法不做数据权限过滤
        DataPermission annotation = getAnnotation(mappedStatementId);
        if (annotation == null) {
            return where;
        }

        // 2. 如果用户拥有全部数据权限（ALL），直接放行不追加条件
        if (provider.hasAllDataScope()) {
            return where;
        }

        // 3. 构建数据权限条件表达式
        Expression dataPermissionExpr = buildPermissionExpression(annotation);

        // ★ fail-closed 安全策略：注解命中但无法生成有效权限条件时，返回恒假条件 1=0
        // 场景：用户未登录、Token 过期、无有效角色等异常情况
        // 设计理由：宁可拒绝访问也不泄露全量数据，符合最小权限原则
        if (dataPermissionExpr == null) {
            Expression falseExpr = new EqualsTo(new LongValue(1), new LongValue(0));
            if (where == null) {
                return falseExpr;
            }
            return new AndExpression(where, falseExpr);
        }

        // 4. 与原有 WHERE 条件用 AND 合并
        if (where == null) {
            return dataPermissionExpr;
        }
        return new AndExpression(where, dataPermissionExpr);
    }

    /**
     * 构建数据权限过滤表达式
     * <p>
     * 根据用户角色的数据范围，生成对应的 SQL 条件：
     * - 有可访问部门 + SELF → (dept_id IN (...) OR create_by = ?)  用括号包裹
     * - 仅有部门条件 → dept_id IN (...)
     * - 仅 SELF → create_by = ?
     * - 无有效用户上下文 → 返回 null（由上层 getSqlSegment 生成 1=0）
     * </p>
     *
     * <h3>SELF 并集修复说明</h3>
     * <p>
     * 当用户同时拥有 DEPT 和 SELF 两个角色时，SELF 带来的"本人创建数据可见"权限
     * 不能被丢弃。通过 hasSelfScope()（至少一个角色是 SELF）触发 SELF 条件追加，
     * 最终生成 (dept_id IN (...) OR create_by = ?) 的并集语义。
     * </p>
     */
    private Expression buildPermissionExpression(DataPermission annotation) {
        // ★ fail-closed 前置检查：无有效用户上下文时返回 null（由上层生成 1=0）
        Long currentUserId = provider.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }

        Set<Long> deptIds = provider.getAccessibleDeptIds();
        // ★ 使用 hasSelfScope() 而非 isSelfScopeOnly()，确保 SELF 权限参与并集
        boolean hasSelf = provider.hasSelfScope();

        // 根据注解配置构建带别名前缀的完整列名
        String fullDeptColumn = buildFullColumnName(annotation.deptAlias(), annotation.deptIdColumn());
        String fullUserColumn = buildFullColumnName(annotation.userAlias(), annotation.userIdColumn());

        Expression deptExpr = null;
        Expression selfExpr = null;

        // 构建部门范围条件：dept_id = ? 或 dept_id IN (?, ?, ...)
        if (deptIds != null && !deptIds.isEmpty()) {
            deptExpr = buildDeptExpression(fullDeptColumn, deptIds);
        }

        // ★ 构建 SELF 条件：只要用户拥有 SELF 角色就追加（参与并集）
        // 修复前仅在 selfOnly 或部门集为空时追加，导致 DEPT+SELF 并存时丢失 SELF 权限
        if (hasSelf) {
            selfExpr = buildSelfExpression(fullUserColumn, currentUserId);
        }

        // 组合条件：如果同时有部门条件和 SELF 条件，用 OR 连接并加括号
        // 语义：能看到指定部门的数据 OR 能看到自己创建的数据
        if (deptExpr != null && selfExpr != null) {
            // 用括号包裹 OR 表达式，确保与外层 AND 条件的优先级正确
            Parenthesis parenthesis = new Parenthesis();
            parenthesis.setExpression(new OrExpression(deptExpr, selfExpr));
            return parenthesis;
        }
        if (deptExpr != null) {
            return deptExpr;
        }
        return selfExpr;
    }

    /**
     * 构建部门过滤表达式
     * <p>
     * 单部门时用 = 运算符（数据库走索引更友好），多部门时用 IN
     * </p>
     */
    private Expression buildDeptExpression(String fullDeptColumn, Set<Long> deptIds) {
        Column column = new Column(fullDeptColumn);
        if (deptIds.size() == 1) {
            // 单部门优化：dept_id = ? 比 dept_id IN (?) 对优化器更友好
            return new EqualsTo(column, new LongValue(deptIds.iterator().next()));
        }
        // 多部门：dept_id IN (1, 2, 3, ...)
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(column);
        ParenthesedExpressionList<LongValue> valueList = new ParenthesedExpressionList<>(
            deptIds.stream().map(LongValue::new).collect(Collectors.toList())
        );
        inExpression.setRightExpression(valueList);
        return inExpression;
    }

    /**
     * 构建仅本人数据过滤表达式：create_by = 当前用户ID
     *
     * @param fullUserColumn 用户ID列名（可能含表别名）
     * @param currentUserId  当前登录用户ID（已由上层保证非 null）
     */
    private Expression buildSelfExpression(String fullUserColumn, Long currentUserId) {
        return new EqualsTo(new Column(fullUserColumn), new LongValue(currentUserId));
    }

    /**
     * 构建带表别名的完整列名
     * <p>例如 alias="u", column="dept_id" → "u.dept_id"</p>
     */
    private String buildFullColumnName(String alias, String column) {
        if (alias == null || alias.isEmpty()) {
            return column;
        }
        return alias + "." + column;
    }

    /**
     * 通过反射获取 Mapper 方法上的 @DataPermission 注解
     * <p>
     * mappedStatementId 格式为 "全限定类名.方法名"，
     * 通过拆分类名和方法名，反射获取对应方法上的注解。
     * 使用方法名匹配（不做参数类型精确匹配），因为 MyBatis Mapper 方法通常不存在重载。
     * </p>
     *
     * @param mappedStatementId Mapper 方法全限定ID
     * @return DataPermission 注解实例，未标注时返回 null
     */
    private DataPermission getAnnotation(String mappedStatementId) {
        try {
            int lastDot = mappedStatementId.lastIndexOf(".");
            String className = mappedStatementId.substring(0, lastDot);
            String methodName = mappedStatementId.substring(lastDot + 1);

            Class<?> clazz = Class.forName(className);
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName) && method.isAnnotationPresent(DataPermission.class)) {
                    return method.getAnnotation(DataPermission.class);
                }
            }
        } catch (ClassNotFoundException e) {
            // 类不存在时（理论上不会发生）不做权限过滤，静默放行
        }
        return null;
    }
}
