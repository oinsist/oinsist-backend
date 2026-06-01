package com.oinsist.system.service;

import com.oinsist.common.core.exception.ServiceException;
import com.oinsist.common.satoken.domain.LoginUser;
import com.oinsist.common.satoken.domain.TokenInfo;
import com.oinsist.common.satoken.helper.LoginHelper;
import com.oinsist.common.satoken.helper.TenantContextHolder;
import com.oinsist.system.domain.LoginVo;
import com.oinsist.system.domain.SysUser;
import com.oinsist.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 登录校验服务
 * <p>
 * 负责用户身份认证的核心逻辑：
 * 1. 根据用户名查询 sys_user 表
 * 2. 校验用户状态（是否停用）
 * 3. BCrypt 密码比对
 * 4. 加载角色标识和权限标识
 * 5. 构建 LoginUser 并写入 Sa-Token Session
 * 6. 返回 Token 信息
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLoginService {

    private final SysUserService sysUserService;
    private final SysUserMapper sysUserMapper;
    private final SysPermissionService sysPermissionService;
    private final SysLoginLogService sysLoginLogService;

    /**
     * BCrypt 密码编码器
     * <p>
     * 使用 Spring Security Crypto 模块提供的 BCryptPasswordEncoder，
     * 仅引入密码加密能力，不引入完整 Spring Security 框架。
     * </p>
     */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 执行登录校验
     * <p>
     * IP 和 UserAgent 由调用方（Controller 层）从 HTTP 请求中提取后传入，
     * 保持 Service 层不依赖 HTTP 上下文，遵循模块边界分离原则。
     * </p>
     *
     * @param username  用户账号
     * @param password  用户密码（明文）
     * @param tenantId  租户ID（登录前 Session 未建立，需显式传入）
     * @param ip        客户端 IP（由 Controller 提取）
     * @param userAgent 客户端 User-Agent（由 Controller 提取）
     * @return LoginVo 包含 tokenName 和 tokenValue
     * @throws ServiceException 认证失败时抛出
     */
    public LoginVo login(String username, String password, Long tenantId, String ip, String userAgent) {
        // 1. 登录前 Session 未建立，租户拦截器无法工作
        // 使用登录专用方法显式指定租户ID查询用户
        SysUser user = sysUserMapper.selectByUsernameAndTenantId(username, tenantId);
        if (user == null) {
            recordLoginLog(username, null, 1, "用户账号或密码错误", ip, userAgent);
            throw new ServiceException("用户账号或密码错误");
        }

        // 2. 校验用户状态
        if ("1".equals(user.getStatus())) {
            recordLoginLog(username, user.getUserId(), 1, "该用户已被停用", ip, userAgent);
            throw new ServiceException("该用户已被停用，请联系管理员");
        }

        // 3. BCrypt 密码校验
        if (!PASSWORD_ENCODER.matches(password, user.getPassword())) {
            recordLoginLog(username, user.getUserId(), 1, "用户账号或密码错误", ip, userAgent);
            throw new ServiceException("用户账号或密码错误");
        }

        // 4. 设置登录阶段的临时租户上下文
        // Session 尚未建立，SaTokenTenantProvider 无法从 Session 获取 tenantId，
        // 通过 ThreadLocal 提供临时租户上下文，确保后续的角色/权限查询能正确隔离
        TenantContextHolder.set(user.getTenantId());
        try {
            // 5. 加载角色和权限（此时 TenantContextHolder 生效，拦截器使用正确的 tenantId）
            Set<String> roleKeys = sysPermissionService.getRoleKeys(user.getUserId());
            Set<String> permissions = sysPermissionService.getPermissions(user.getUserId());

            // 6. 构建登录用户上下文
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(user.getUserId());
            loginUser.setUsername(user.getUsername());
            loginUser.setNickname(user.getNickname());
            loginUser.setDeptId(user.getDeptId());
            loginUser.setTenantId(user.getTenantId());
            loginUser.setRoleKeys(roleKeys);
            loginUser.setPermissions(permissions);

            // 7. 执行登录，生成 Token 并将 LoginUser 写入 Session
            // LoginHelper.login() 建立 Session 后，SaTokenTenantProvider 可从 Session 获取 tenantId
            TokenInfo tokenInfo = LoginHelper.login(loginUser);

            // 8. 记录登录成功日志
            recordLoginLog(username, loginUser.getUserId(), 0, "登录成功", ip, userAgent);

            // 9. 构建登录响应
            LoginVo loginVo = new LoginVo();
            loginVo.setTokenName(tokenInfo.getTokenName());
            loginVo.setTokenValue(tokenInfo.getTokenValue());
            return loginVo;
        } finally {
            // 必须清除 ThreadLocal，防止线程池复用时租户泄漏
            TenantContextHolder.clear();
        }
    }

    /**
     * 记录登录日志（失败不影响主流程）
     *
     * @param username  用户账号
     * @param userId    用户ID（可能为 null）
     * @param status    状态：0-成功，1-失败
     * @param msg       消息
     * @param ip        客户端 IP
     * @param userAgent 客户端 User-Agent
     */
    private void recordLoginLog(String username, Long userId, Integer status, String msg,
                                String ip, String userAgent) {
        try {
            sysLoginLogService.recordLoginLog(username, userId, status, msg, ip, userAgent);
        } catch (Exception e) {
            // 登录日志记录失败不应影响主登录流程
            log.warn("记录登录日志失败: {}", e.getMessage());
        }
    }
}
