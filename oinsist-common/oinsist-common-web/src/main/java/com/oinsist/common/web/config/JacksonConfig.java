package com.oinsist.common.web.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oinsist.common.web.jackson.BigNumberSerializer;
import com.oinsist.common.web.jackson.SensitiveSerializerModifier;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;

/**
 * Jackson 全局配置
 * <p>
 * 为什么使用 Jackson2ObjectMapperBuilderCustomizer 而非直接注册 ObjectMapper Bean：
 * 1. Spring Boot 自动配置已创建了 ObjectMapper，直接注册会覆盖其默认行为
 * 2. Customizer 是 Spring Boot 推荐的扩展方式，可叠加多个 Customizer
 * 3. 统一维护全局 JSON 规则，避免各 Controller/VO 分散处理序列化细节
 */
@Configuration
public class JacksonConfig {

    /**
     * 注册脱敏序列化模块 + 大整数序列化模块
     * <p>
     * 脱敏模块：通过 SimpleModule + BeanSerializerModifier 实现，
     * Jackson 序列化 Bean 时自动检测 @Sensitive 注解，动态替换字段序列化器。
     * </p>
     * <p>
     * 大整数模块：把 Long / long / BigInteger 交给 {@link BigNumberSerializer} 处理，
     * 解决雪花算法主键（19 位 Long）超出 JS 安全整数范围导致前端丢精度的问题。
     * 注意：一旦调用 builder.modules(...)，就要把 JavaTimeModule 一并放进去。
     * 这样 LocalDateTime 等 Java 8 时间类型才能正常输出 ISO 字符串，
     * 避免用户列表这类包含 createTime 的响应在写 JSON 时抛 500。
     * </p>
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            SimpleModule sensitiveModule = new SimpleModule("SensitiveModule");
            sensitiveModule.setSerializerModifier(new SensitiveSerializerModifier());

            SimpleModule bigNumberModule = new SimpleModule("BigNumberModule");
            bigNumberModule.addSerializer(Long.class, BigNumberSerializer.INSTANCE);
            bigNumberModule.addSerializer(Long.TYPE, BigNumberSerializer.INSTANCE);
            bigNumberModule.addSerializer(BigInteger.class, BigNumberSerializer.INSTANCE);

            builder
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .modules(new JavaTimeModule(), sensitiveModule, bigNumberModule);
        };
    }
}
