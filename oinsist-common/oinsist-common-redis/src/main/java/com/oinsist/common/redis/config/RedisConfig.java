package com.oinsist.common.redis.config;

import com.oinsist.common.redis.constant.CacheNames;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis + Spring Cache 统一配置入口。
 * <p>
 * 本类通过 {@link EnableCaching} 开启 Spring 缓存抽象，并注册 {@link RedissonSpringCacheManager}
 * 作为全局唯一的 {@link CacheManager} 实现。
 * </p>
 *
 * <h3>为什么选择 RedissonSpringCacheManager 而非 Spring 默认的 RedisCacheManager？</h3>
 * <ul>
 *   <li>Redisson 的 CacheManager 原生支持分布式锁防缓存击穿（get 操作自动加锁，避免并发回源）</li>
 *   <li>与 {@link RedissonClient} 共享连接池，不需要额外的 RedisConnectionFactory，减少连接资源开销</li>
 *   <li>支持更丰富的缓存策略：TTL（存活时间）+ maxIdleTime（最大空闲时间）双重控制，
 *       可以更精细地淘汰"写入后长时间无人访问"的冷数据</li>
 * </ul>
 *
 * <h3>属性外部化设计</h3>
 * <p>
 * TTL 与 maxIdleTime 通过 {@link RedisCacheProperties} 从 application.yml 读取，
 * 使得 dev/prod 环境可独立调整缓存策略，无需修改代码重新编译。
 * </p>
 *
 * @author oinsist
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(RedisCacheProperties.class)
public class RedisConfig {

    /**
     * 注册基于 Redisson 的 Spring CacheManager。
     * <p>
     * 配置说明：
     * <ul>
     *   <li><b>TTL（Time To Live）</b>：缓存条目自创建/更新后的最大存活时间，到期后无论是否被访问都会被移除。</li>
     *   <li><b>maxIdleTime（最大空闲时间）</b>：缓存条目自上次被访问后的最大空闲时间，
     *       超过此时间未被读取则提前淘汰，可快速回收无人关注的冷数据，降低内存占用。</li>
     * </ul>
     * 两者共同作用：即使 TTL 未到期，若空闲时间超限也会被淘汰；
     * 反之，即使持续被访问，TTL 到期后也会强制失效以保证数据新鲜度。
     * </p>
     * <p>
     * 关键修复：为所有已知的 {@link CacheNames} 显式注册 CacheConfig，
     * 因为 Redisson 不会将 "default" 配置作为通配默认值——未注册的缓存名将不设 TTL，导致缓存永不过期。
     * </p>
     *
     * @param redissonClient 由 redisson-spring-boot-starter 自动配置注入的 Redisson 客户端
     * @param properties     从 application.yml 读取的缓存配置属性（TTL、maxIdleTime）
     * @return 基于 Redisson 的 CacheManager 实例
     */
    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient, RedisCacheProperties properties) {
        long ttl = properties.getTtl().toMillis();
        long maxIdleTime = properties.getMaxIdleTime().toMillis();

        // 统一配置：所有缓存名共享相同的 TTL 和 maxIdleTime
        CacheConfig defaultConfig = new CacheConfig(ttl, maxIdleTime);

        // 为所有已知缓存名显式注册配置，确保每个 @Cacheable 使用的缓存名都有 TTL 约束
        Map<String, CacheConfig> configMap = new HashMap<>();
        configMap.put(CacheNames.DEFAULT, defaultConfig);
        configMap.put(CacheNames.SYS_CONFIG, defaultConfig);
        configMap.put(CacheNames.SYS_DICT, defaultConfig);

        return new RedissonSpringCacheManager(redissonClient, configMap);
    }
}
