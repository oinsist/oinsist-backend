package com.oinsist.common.web.config;

import org.springframework.context.annotation.Configuration;
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
}
