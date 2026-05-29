package com.oinsist.common.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Redis 缓存配置属性。
 * <p>
 * 通过 application.yml 中 oinsist.cache 前缀配置，
 * 让缓存 TTL 等参数可在不同环境（dev/prod）独立调整，避免硬编码。
 * </p>
 *
 * @author oinsist
 */
@Data
@ConfigurationProperties(prefix = "oinsist.cache")
public class RedisCacheProperties {

    /** 默认缓存存活时间（TTL），默认 30 分钟 */
    private Duration ttl = Duration.ofMinutes(30);

    /** 默认缓存最大空闲时间，默认 15 分钟（超时未访问则淘汰） */
    private Duration maxIdleTime = Duration.ofMinutes(15);
}
