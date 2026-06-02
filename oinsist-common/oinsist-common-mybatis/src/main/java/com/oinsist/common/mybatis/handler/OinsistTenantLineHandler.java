package com.oinsist.common.mybatis.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.oinsist.common.core.service.TenantProvider;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Arrays;
import java.util.List;

/**
 * 多租户 SQL 改写处理器
 *
 * <p>设计原理：
 * MyBatis-Plus 的 TenantLineInnerInterceptor 在 SQL 执行前，通过本处理器获取租户信息，
 * 自动对 SELECT/INSERT/UPDATE/DELETE 语句追加 tenant_id 条件或字段值。</p>
 *
 * <h3>核心行为：</h3>
 * <ol>
 *     <li>SELECT：WHERE 子句追加 AND tenant_id = #{tenantId}</li>
 *     <li>INSERT：自动填充 tenant_id 字段值</li>
 *     <li>UPDATE/DELETE：WHERE 子句追加 AND tenant_id = #{tenantId}</li>
 * </ol>
 *
 * <h3>安全策略：</h3>
 * <ul>
 *     <li>tenantId 为 null 时不能静默放行（否则跨租户泄漏），需由调用方保证上下文合法
 *         或在拦截器层面配合 @InterceptorIgnore 控制</li>
 *     <li>白名单表（全局共享表）直接跳过，不追加租户条件</li>
 * </ul>
 *
 * @author oinsist
 */
public class OinsistTenantLineHandler implements TenantLineHandler {

    /**
     * 延迟解析策略：持有 ObjectProvider 而非直接持有 TenantProvider 实例。
     * <p>
     * 原因：MybatisPlusConfig 中 mybatisPlusInterceptor() 是在 sqlSessionFactory 创建阶段调用的，
     * 如果此时直接注入 TenantProvider（其实现类依赖 Mapper → sqlSessionFactory），会触发循环依赖。
     * 将 ObjectProvider 下沉到 Handler 内部，在 SQL 实际执行时才通过 getIfAvailable() 获取 Provider，
     * 此时 sqlSessionFactory 早已创建完成，不存在循环依赖问题。
     * </p>
     */
    private final ObjectProvider<TenantProvider> tenantProviderProvider;

    /**
     * 全局共享表白名单
     * 这些表的数据全租户共享，SQL 不追加 tenant_id 条件
     * 包含：菜单定义表、角色-菜单关联表、租户定义表本身
     */
    private static final List<String> IGNORE_TABLES = Arrays.asList(
        "sys_menu",       // 菜单权限定义（全局统一）
        "sys_role_menu",  // 角色-菜单权限关联（跟随菜单全局）
        "sys_tenant"      // 租户表本身（管理所有租户）
    );

    public OinsistTenantLineHandler(ObjectProvider<TenantProvider> tenantProviderProvider) {
        this.tenantProviderProvider = tenantProviderProvider;
    }

    /**
     * 获取当前租户 ID 表达式
     * 由 TenantLineInnerInterceptor 调用，作为 SQL 改写的值
     *
     * <p>重要：此处不做 null 校验抛异常，因为在 ignoreTable 已过滤的全局表查询中，
     * 此方法不会被调用；对于需要租户隔离的表，若 tenantId 为 null，
     * 返回 0 值确保不会匹配到任何真实租户数据（fail-closed 安全策略）</p>
     *
     * <p>登录场景说明：
     * 登录查询用户时 Session 未建立，tenantId 为 null → 返回 0L。
     * 登录流程通过 @InterceptorIgnore 跳过租户拦截器并显式指定 tenant_id，
     * 因此 getTenantId() 返回 0L 不会影响登录链路。</p>
     */
    @Override
    public Expression getTenantId() {
        TenantProvider tenantProvider = tenantProviderProvider.getIfAvailable();
        if (tenantProvider == null) {
            // Provider 未注册（项目未启用多租户），返回 0（fail-closed）
            return new LongValue(0L);
        }
        Long tenantId = tenantProvider.getTenantId();
        // 未登录场景下 tenantId 可能为 null
        // 此时返回 0 值，确保不会匹配到任何真实租户数据（fail-closed）
        return new LongValue(tenantId != null ? tenantId : 0L);
    }

    /**
     * 获取租户字段名
     * 所有租户隔离表统一使用 "tenant_id" 作为租户标识列名
     */
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    /**
     * 判断指定表是否忽略租户条件
     * 返回 true 则该表的 SQL 不追加 tenant_id 条件
     *
     * <p>设计决策：</p>
     * <ul>
     *     <li>sys_menu：菜单是全局功能定义，所有租户共享同一套菜单树</li>
     *     <li>sys_role_menu：角色-菜单绑定跟随菜单全局策略</li>
     *     <li>sys_tenant：租户管理表本身不做租户隔离</li>
     * </ul>
     */
    @Override
    public boolean ignoreTable(String tableName) {
        return IGNORE_TABLES.contains(tableName.toLowerCase());
    }
}
