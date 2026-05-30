package com.oinsist.common.redis.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis 基础读写服务 —— 教学型薄封装。
 * <p>
 * 本类通过对 Redisson {@link RBucket} API 的极简包装，帮助开发者快速理解
 * Redisson 操作 Redis 键值对的基本模式（set / get / delete / expire / exists）。
 * </p>
 * <p>
 * <b>设计说明：</b>生产项目中完全可以直接注入 {@link RedissonClient} 使用其原生 API，
 * 此封装不做过度抽象，仅作为学习入口和统一调用口径。如果后续需要批量操作、Pipeline、
 * Lua 脚本等高级特性，建议直接使用 RedissonClient。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class RedisService {

    private final RedissonClient redissonClient;

    /**
     * 写入缓存，无过期时间（对应 Redis 命令：SET key value）。
     *
     * @param key   缓存键
     * @param value 缓存值（Redisson 会自动序列化）
     */
    public <T> void set(String key, T value) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }

    /**
     * 写入缓存并设置过期时间（对应 Redis 命令：SET key value EX/PX ttl）。
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param ttl   过期时长
     * @param unit  时间单位
     */
    public <T> void set(String key, T value, long ttl, TimeUnit unit) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value, Duration.of(ttl, unit.toChronoUnit()));
    }

    /**
     * 读取缓存（对应 Redis 命令：GET key）。
     *
     * @param key 缓存键
     * @return 缓存值，不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    /**
     * 删除缓存（对应 Redis 命令：DEL key）。
     *
     * @param key 缓存键
     * @return true 表示删除成功（key 存在并被删除），false 表示 key 不存在
     */
    public boolean delete(String key) {
        RBucket<?> bucket = redissonClient.getBucket(key);
        return bucket.delete();
    }

    /**
     * 设置 key 的过期时间（对应 Redis 命令：EXPIRE key seconds / PEXPIRE key milliseconds）。
     *
     * @param key  缓存键
     * @param ttl  过期时长
     * @param unit 时间单位
     * @return true 表示设置成功，false 表示 key 不存在
     */
    public boolean expire(String key, long ttl, TimeUnit unit) {
        RBucket<?> bucket = redissonClient.getBucket(key);
        return bucket.expire(Duration.of(ttl, unit.toChronoUnit()));
    }

    /**
     * 判断 key 是否存在（对应 Redis 命令：EXISTS key）。
     *
     * @param key 缓存键
     * @return true 表示存在
     */
    public boolean hasKey(String key) {
        RBucket<?> bucket = redissonClient.getBucket(key);
        return bucket.isExists();
    }

    /**
     * 原子性设置值（仅当 key 不存在时写入）
     * <p>
     * 对应 Redis 的 SET key value NX EX/PX ttl 命令。
     * 适用于幂等性控制、防重复提交等需要原子占位的场景。
     * </p>
     *
     * @param key   缓存 key
     * @param value 缓存值
     * @param ttl   过期时间（必须大于 0，禁止产生永久 key）
     * @param unit  时间单位
     * @return true 表示写入成功（key 不存在），false 表示 key 已存在
     */
    public <T> boolean setIfAbsent(String key, T value, long ttl, TimeUnit unit) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.setIfAbsent(value, Duration.of(ttl, unit.toChronoUnit()));
    }
}
