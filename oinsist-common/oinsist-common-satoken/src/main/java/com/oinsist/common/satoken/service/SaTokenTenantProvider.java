package com.oinsist.common.satoken.service;

import com.oinsist.common.core.service.TenantProvider;
import com.oinsist.common.satoken.domain.LoginUser;
import com.oinsist.common.satoken.helper.LoginHelper;
import com.oinsist.common.satoken.helper.TenantContextHolder;
import org.springframework.stereotype.Component;

/**
 * 基于 Sa-Token 会话实现的租户提供者
 * <p>
 * 设计说明：
 * - 优先从 ThreadLocal（TenantContextHolder）获取租户 ID，覆盖登录阶段 Session 未建立的场景
 * - 其次从当前 Sa-Token Session 中的 LoginUser 获取 tenantId（正常请求场景）
 * - 两者都为空时返回 null，由租户拦截器的 fail-closed 策略保证安全
 * </p>
 * <p>
 * 架构定位：
 * 本类是 common-core 中 TenantProvider 接口的唯一实现，通过 Spring 的依赖注入机制
 * 在运行时自动装配到 common-mybatis 的多租户拦截器中。
 * 这种"接口在底层、实现在上层"的设计遵循依赖反转原则（DIP），
 * 使得持久层模块不需要直接依赖认证框架。
 * </p>
 */
@Component
public class SaTokenTenantProvider implements TenantProvider {

    /**
     * 获取当前租户 ID
     * <p>
     * 优先级：ThreadLocal > Sa-Token Session
     * <ul>
     *     <li>ThreadLocal：登录阶段由 SysLoginService 设置，Session 建立前的临时租户上下文</li>
     *     <li>Session：正常请求阶段，从已登录用户的 Session 中获取</li>
     * </ul>
     * </p>
     */
    @Override
    public Long getTenantId() {
        // 优先读取 ThreadLocal（登录阶段、系统任务等 Session 未建立的场景）
        Long tenantId = TenantContextHolder.get();
        if (tenantId != null) {
            return tenantId;
        }
        // 其次从 Sa-Token Session 读取（正常已登录请求）
        LoginUser loginUser = LoginHelper.getLoginUserOrNull();
        return loginUser != null ? loginUser.getTenantId() : null;
    }
}
