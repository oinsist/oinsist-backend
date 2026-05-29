package com.oinsist.admin.service;

import com.oinsist.common.redis.constant.CacheNames;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Redis 缓存测试服务。
 * <p>
 * 用于验证 @Cacheable 注解与 RedissonSpringCacheManager 的集成效果。
 * 仅供 P04 开发阶段测试使用。
 * </p>
 */
@Profile("dev")
@Service
public class RedisTestService {

    /**
     * 模拟一个耗时查询，使用 @Cacheable 缓存结果。
     * <p>
     * 第一次调用会执行方法体（回源），后续调用直接命中缓存。
     * 通过观察返回的时间戳是否变化，可判断缓存是否生效。
     * </p>
     */
    @Cacheable(cacheNames = CacheNames.SYS_CONFIG, key = "'test_cache_key'")
    public String getCachedData() {
        // 模拟回源查询，返回当前时间戳作为标识
        return "数据生成时间: " + LocalDateTime.now();
    }
}
