package com.oinsist.system.service;

import com.oinsist.common.satoken.service.PermissionService;
import com.oinsist.system.mapper.SysMenuMapper;
import com.oinsist.system.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 权限查询服务
 * <p>
 * 实现 common-satoken 定义的 {@link PermissionService} 接口，
 * 完成"业务模块实现基础模块接口"的依赖反转。
 * </p>
 * <p>
 * 核心规则：
 * - 拥有 admin 角色的用户自动获得所有权限（"*:*:*"），无需逐条查询 sys_role_menu。
 * - 普通角色用户通过 sys_user_role + sys_role_menu 关联查询获取实际权限。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SysPermissionService implements PermissionService {

    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    /**
     * 获取用户角色标识集合
     */
    @Override
    public Set<String> getRoleKeys(Long userId) {
        return sysRoleMapper.selectRoleKeysByUserId(userId);
    }

    /**
     * 获取用户权限标识集合
     * <p>
     * admin 角色直接返回通配权限 "*:*:*"，表示拥有所有操作权限。
     * 这是 RuoYi 系列项目的经典设计：超级管理员不走细粒度校验，
     * 避免每次新增权限都要手动给 admin 角色勾选。
     * </p>
     */
    @Override
    public Set<String> getPermissions(Long userId) {
        Set<String> roleKeys = getRoleKeys(userId);
        if (roleKeys.contains("admin")) {
            // 超级管理员拥有所有权限
            Set<String> perms = new HashSet<>();
            perms.add("*:*:*");
            return perms;
        }
        return sysMenuMapper.selectPermsByUserId(userId);
    }
}
