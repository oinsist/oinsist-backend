package com.oinsist.common.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 基础配置扩展点。
 *
 * <p>这里实现 {@link WebMvcConfigurer}，但当前阶段暂时不重写任何方法，是有意为之：</p>
 *
 * <p>1. P01/P02 阶段的目标是先建立清晰的工程骨架和 Web 层边界，避免一开始就堆叠跨域、
 * 拦截器、消息转换器等配置，导致学习主线被细节淹没。</p>
 *
 * <p>2. Spring Boot 已经为 Spring MVC 提供了足够完善的自动配置，例如 JSON 序列化、
 * 静态资源处理、参数绑定、异常基础处理等。我们只保留扩展入口，不主动覆盖默认行为，
 * 可以最大限度复用 Spring Boot 的约定式配置。</p>
 *
 * <p>3. 后续专题需要注册 Sa-Token 拦截器、统一跨域、接口日志拦截器、数据脱敏转换器时，
 * 都可以集中收敛到这个配置类中。这样 Web 基础设施不会散落在各个业务模块，
 * 也能保持 RuoYi-Vue-Plus 式的“启动模块轻、公共配置集中”的分层思想。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 全局跨域配置
     * <p>
     * 通过 WebMvcConfigurer#addCorsMappings 实现全局 CORS，
     * 相比 @CrossOrigin 注解更集中、可维护；相比 CorsFilter 更轻量。
     * Spring Boot 3.5 推荐此方式作为标准跨域配置方案。
     * </p>
     * <p>
     * 安全说明：
     * - 开发环境允许所有来源（allowedOriginPatterns = "*"），方便调试
     * - 生产环境应通过配置文件或环境变量限制为具体前端域名
     * - allowCredentials(true) 与 allowedOrigins("*") 互斥，
     *   因此使用 allowedOriginPatterns("*") 替代
     * </p>
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            // 允许所有来源（开发环境），生产环境应替换为具体域名
            .allowedOriginPatterns("*")
            // 允许所有 HTTP 方法（含 PATCH、HEAD），避免遗漏导致前端报错
            .allowedMethods("*")
            // 允许的请求头
            .allowedHeaders("*")
            // 暴露 Authorization 响应头：跨域下浏览器默认仅暴露 6 个 simple header，
            // 若未来登录接口改为通过响应头返回 Token，前端 JS 才能读取到该字段
            .exposedHeaders("Authorization")
            // 允许携带凭证（Cookie、Authorization Header）
            .allowCredentials(true)
            // 预检请求缓存时间（秒），减少 OPTIONS 请求频率
            .maxAge(3600);
    }
}
