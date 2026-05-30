package com.oinsist.common.web.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.oinsist.common.web.jackson.SensitiveSerializerModifier;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局配置
 * <p>
 * 为什么使用 Jackson2ObjectMapperBuilderCustomizer 而非直接注册 ObjectMapper Bean：
 * 1. Spring Boot 自动配置已创建了 ObjectMapper，直接注册会覆盖其默认行为
 * 2. Customizer 是 Spring Boot 推荐的扩展方式，可叠加多个 Customizer
 * 3. 不影响 Spring Boot 自带的 JSR-310 日期序列化等默认配置
 */
@Configuration
public class JacksonConfig {

    /**
     * 注册脱敏序列化模块
     * <p>
     * 通过 SimpleModule + BeanSerializerModifier 实现：
     * Jackson 序列化 Bean 时自动检测 @Sensitive 注解，动态替换字段序列化器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer sensitiveCustomizer() {
        return builder -> {
            SimpleModule sensitiveModule = new SimpleModule("SensitiveModule");
            sensitiveModule.setSerializerModifier(new SensitiveSerializerModifier());
            builder.modules(sensitiveModule);
        };
    }
}
