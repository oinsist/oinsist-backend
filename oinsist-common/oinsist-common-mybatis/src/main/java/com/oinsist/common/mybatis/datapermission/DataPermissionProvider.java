package com.oinsist.common.mybatis.datapermission;

import java.util.Set;

/**
 * 数据权限上下文提供者接口
 * <p>
 * 设计原因：common-mybatis 是基础设施模块，不能反向依赖业务模块 oinsist-system。
 * 通过接口反转（DIP），业务模块实现此接口并注册为 Spring Bean，
 * 基础设施层通过接口调用获取当前用户的数据权限上下文。
 * 这与 {@link com.oinsist.common.core.service.CurrentUserProvider} 的依赖反转策略保持一致。
 * </p>
 *
 * <h3>为什么不直接复用 CurrentUserProvider？</h3>
 * <ul>
 *     <li>CurrentUserProvider 仅提供用户ID，职责单一，供字段自动填充使用</li>
 *     <li>数据权限需要更丰富的上下文（部门ID、角色数据范围、可访问部门集合），
 *         单独定义接口符合接口隔离原则（ISP）</li>
 *     <li>未来数据权限策略可能独立演进（如引入数据标签权限），
 *         独立接口有更好的扩展空间</li>
 * </ul>
 *
 * <h3>实现约定</h3>
 * <ul>
 *     <li>实现类由 oinsist-system 模块提供，运行时通过 Spring DI 注入</li>
 *     <li>Handler 中使用 {@code @Autowired(required = false)} 注入，
 *         未提供实现时数据权限功能自动跳过</li>
 *     <li>实现类应当做好缓存策略，避免每次 SQL 执行都触发数据库查询</li>
 * </ul>
 *
 * @author oinsist
 */
public interface DataPermissionProvider {

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID，未登录时返回 null
     */
    Long getCurrentUserId();

    /**
     * 获取当前用户所属部门ID
     *
     * @return 部门ID，未登录时返回 null
     */
    Long getCurrentDeptId();

    /**
     * 获取当前用户是否拥有全部数据权限
     * <p>
     * 判断逻辑：遍历用户所有角色，任一角色的 data_scope 为 ALL 即返回 true。
     * 当返回 true 时，数据权限拦截器直接放行，不追加任何 WHERE 条件。
     * </p>
     *
     * @return true 表示拥有全部数据权限
     */
    boolean hasAllDataScope();

    /**
     * 获取当前用户可访问的部门ID集合
     * <p>
     * 合并逻辑：遍历用户所有角色的 data_scope，
     * <ul>
     *     <li>ALL：直接放行（hasAllDataScope 返回 true，本方法不会被调用）</li>
     *     <li>DEPT：加入用户本部门</li>
     *     <li>DEPT_AND_CHILD：加入用户本部门及所有子部门</li>
     *     <li>CUSTOM：加入角色关联的自定义部门集合</li>
     *     <li>SELF：不加入任何部门（仅按 create_by 过滤）</li>
     * </ul>
     * 最终返回多角色的部门集合并集。
     * </p>
     *
     * @return 可访问的部门ID集合，可能为空集（当所有角色均为 SELF 时）
     */
    Set<Long> getAccessibleDeptIds();

    /**
     * 当前用户是否仅有 SELF 数据权限
     * <p>
     * 判断逻辑：所有角色的 data_scope 均为 SELF 时返回 true。
     * 此时仅按 create_by = 当前用户ID 进行过滤。
     * </p>
     *
     * @return true 表示仅本人数据权限
     */
    boolean isSelfScopeOnly();

    /**
     * 当前用户是否拥有 SELF 数据范围（至少一个角色为 SELF）
     * <p>
     * 与 isSelfScopeOnly() 的区别：
     * <ul>
     *     <li>isSelfScopeOnly()：所有角色都是 SELF（纯粹仅看本人数据）</li>
     *     <li>hasSelfScope()：至少一个角色是 SELF（SELF 权限参与并集合并）</li>
     * </ul>
     * 设计原因：当用户同时拥有 DEPT 和 SELF 两个角色时，
     * 最终条件应为 (dept_id IN (...) OR create_by = ?)，
     * 即部门数据 + 本人创建数据的并集，不能丢失 SELF 角色带来的权限。
     * </p>
     *
     * @return true 表示至少一个角色拥有 SELF 数据范围
     */
    boolean hasSelfScope();
}
