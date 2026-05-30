package com.oinsist.common.satoken.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 认证拦截器全局配置
 * <p>
 * 设计思想：
 * 1. 采用"默认拦截一切、白名单放行"策略（Deny by Default），
 *    这是企业安全最佳实践——任何新增接口都自动受到保护，不会因为开发者遗忘而裸奔上线。
 * 2. 拦截器注册在独立的 satoken 模块中而非 common-web，
 *    保持模块职责单一：认证逻辑内聚在 satoken 模块，Web 通用能力（全局异常、跨域等）留在 common-web。
 * 3. Sa-Token 的 SaInterceptor 本质是一个 Spring MVC HandlerInterceptor，
 *    通过 preHandle 阶段执行认证校验，比 Filter 更贴合 Spring 生态且能获取 Handler 信息。
 * </p>
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册 Sa-Token 路由拦截器
     * <p>
     * 核心逻辑：对所有路径执行 {@code StpUtil.checkLogin()} 校验，
     * 未携带有效 Token 的请求会被 Sa-Token 自动抛出 NotLoginException，
     * 由全局异常处理器统一返回 401 响应。
     * </p>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                // 默认拦截所有路径
                .addPathPatterns("/**")
                // 放行白名单：不需要登录即可访问的路径
                .excludePathPatterns(
                        // ---------- 认证相关 ----------
                        "/auth/login",      // 登录接口（未登录才需要调用）
                        "/auth/logout",     // 退出接口（放行让前端无论如何都能调通，避免 Token 过期后无法退出）

                        // ---------- 接口文档相关 ----------
                        "/doc.html",            // Knife4j 接口文档页面
                        "/webjars/**",          // 文档页面所需的静态资源（CSS/JS）
                        "/v3/api-docs/**",      // OpenAPI 3 规范的 JSON 描述文件
                        "/swagger-resources/**", // Swagger 兼容性放行（部分工具仍会请求此路径）

                        // ---------- 其他静态资源 ----------
                        "/favicon.ico"          // 浏览器自动请求的图标，无需认证
                );
    }
}
