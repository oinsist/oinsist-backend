package com.oinsist.common.satoken.service;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限/角色数据提供实现
 * <p>
 * Sa-Token 在执行权限校验（如 {@code @SaCheckPermission("system:user:list")}）时，
 * 会回调 {@link StpInterface} 的方法来获取当前用户的权限列表和角色列表。
 * 本类作为 Sa-Token 与业务层之间的桥梁：
 * - 接收 Sa-Token 的回调请求
 * - 委托给 {@link PermissionService}（由 system 模块实现）获取真实数据
 * - 转换为 Sa-Token 要求的 List 格式返回
 * </p>
 * <p>
 * 通过 {@code @Component} 注册到 Spring 容器后，Sa-Token 会自动发现并使用此实现，
 * 无需额外配置（Sa-Token 内部通过 Spring Bean 机制查找 StpInterface 实现类）。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final PermissionService permissionService;

    /**
     * 获取用户权限列表
     * <p>
     * Sa-Token 在执行 {@code StpUtil.checkPermission("xxx")} 或
     * {@code @SaCheckPermission("xxx")} 时调用此方法。
     * </p>
     *
     * @param loginId   当前登录用户的 ID（Sa-Token 中为 Object 类型，实际是 Long）
     * @param loginType 登录类型（多账号体系时用于区分，单体系统固定为 "login"）
     * @return 权限标识列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        return new ArrayList<>(permissionService.getPermissions(userId));
    }

    /**
     * 获取用户角色列表
     * <p>
     * Sa-Token 在执行 {@code StpUtil.checkRole("xxx")} 或
     * {@code @SaCheckRole("xxx")} 时调用此方法。
     * </p>
     *
     * @param loginId   当前登录用户的 ID
     * @param loginType 登录类型
     * @return 角色标识列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        return new ArrayList<>(permissionService.getRoleKeys(userId));
    }
}
