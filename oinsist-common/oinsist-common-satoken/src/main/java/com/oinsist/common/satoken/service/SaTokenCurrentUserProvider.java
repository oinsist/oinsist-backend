package com.oinsist.common.satoken.service;

import com.oinsist.common.core.service.CurrentUserProvider;
import com.oinsist.common.satoken.helper.LoginHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 基于 Sa-Token 的当前用户提供者实现
 * <p>
 * 将 Sa-Token 的登录上下文能力适配到通用的 {@link CurrentUserProvider} 接口，
 * 使 common-mybatis 等基础模块无需直接感知 Sa-Token 的存在。
 * </p>
 */
@Slf4j
@Component
public class SaTokenCurrentUserProvider implements CurrentUserProvider {

    /**
     * 通过 Sa-Token 获取当前登录用户ID
     * <p>
     * 未登录场景（如系统初始化、定时任务）下 LoginHelper 会抛出异常，
     * 此处捕获并返回 null，让调用方优雅降级。
     * </p>
     */
    @Override
    public Long getUserId() {
        try {
            return LoginHelper.getUserId();
        } catch (Exception e) {
            log.debug("获取登录用户ID失败（可能是未登录场景），返回 null");
            return null;
        }
    }
}
