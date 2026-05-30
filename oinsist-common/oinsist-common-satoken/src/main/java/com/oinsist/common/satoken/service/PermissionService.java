package com.oinsist.common.satoken.service;

import java.util.Set;

/**
 * 权限数据提供者接口（依赖反转核心）
 * <p>
 * 设计目的：
 * 1. Sa-Token 的权限判断（{@code @SaCheckPermission}、{@code @SaCheckRole}）需要知道
 *    "当前用户拥有哪些角色和权限"，这是通过实现 Sa-Token 的 {@code StpInterface} 来提供的。
 * 2. 角色和权限数据存储在 sys_role / sys_menu 等业务表中，由 oinsist-system 模块管理。
 * 3. 如果让 common-satoken 直接调用 system 模块的 Service，就会产生"基础设施层反向依赖业务层"的问题。
 * 4. 解决方案：common-satoken 定义本接口，system 模块实现本接口。
 *    运行时 Spring 会将 system 的实现注入到 common-satoken 的 StpInterfaceImpl 中，
 *    编译期 common-satoken 完全不知道 system 的存在——这就是依赖反转（DIP）的经典应用。
 * </p>
 */
public interface PermissionService {

    /**
     * 获取用户角色标识集合
     *
     * @param userId 用户ID
     * @return 角色标识集合（如 ["admin", "common"]）
     */
    Set<String> getRoleKeys(Long userId);

    /**
     * 获取用户权限标识集合
     *
     * @param userId 用户ID
     * @return 权限标识集合（如 ["system:user:list", "system:user:add"]）
     */
    Set<String> getPermissions(Long userId);
}
