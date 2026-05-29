package com.oinsist.admin.controller;

import com.oinsist.admin.service.RedisTestService;
import com.oinsist.common.core.domain.R;
import com.oinsist.common.redis.service.RedisLockService;
import com.oinsist.common.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * P04 Redis 能力验证接口。
 * <p>
 * 本 Controller 用于验证 oinsist-common-redis 模块提供的三大核心能力：
 * <ul>
 *     <li>基础读写（RedisService：set / get / delete）</li>
 *     <li>Spring Cache 集成（@Cacheable + RedissonSpringCacheManager）</li>
 *     <li>分布式锁（RedisLockService：tryLock + executeWithLock）</li>
 * </ul>
 * 仅在 dev 环境可用（通过 @Profile("dev") 控制），生产环境不会注册此 Bean。
 * </p>
 */
@RestController
@RequestMapping("/test/redis")
@Profile("dev")
@RequiredArgsConstructor
public class RedisTestController {

    private final RedisService redisService;
    private final RedisLockService redisLockService;
    private final RedisTestService redisTestService;

    /**
     * 验证 Redis 基础写入能力。
     * <p>调用 RedisService.set() 将键值对写入 Redis。</p>
     */
    @PostMapping("/set")
    public R<String> set(@RequestParam String key, @RequestParam String value) {
        redisService.set(key, value);
        return R.ok("写入成功");
    }

    /**
     * 验证 Redis 基础读取能力。
     * <p>调用 RedisService.get() 从 Redis 中读取指定 key 的值。</p>
     */
    @GetMapping("/get")
    public R<Object> get(@RequestParam String key) {
        Object value = redisService.get(key);
        return R.ok(value);
    }

    /**
     * 验证 Redis 基础删除能力。
     * <p>调用 RedisService.delete() 删除 Redis 中指定的 key。</p>
     */
    @DeleteMapping("/delete")
    public R<String> delete(@RequestParam String key) {
        redisService.delete(key);
        return R.ok("删除成功");
    }

    /**
     * 验证 Spring Cache + Redisson 缓存集成。
     * <p>
     * 多次调用此接口，若返回的时间戳保持不变，说明 @Cacheable 缓存命中生效，
     * 方法体未被重复执行（未回源）。
     * </p>
     */
    @GetMapping("/cache")
    public R<String> cache() {
        String result = redisTestService.getCachedData();
        return R.ok(result);
    }

    /**
     * 验证 Redisson 分布式锁能力。
     * <p>
     * 使用 executeWithLock 模板方法获取锁后模拟持有 3 秒，
     * 可通过并发请求同一 key 来观察锁的互斥效果。
     * 获取锁失败时返回失败响应。
     * </p>
     */
    @PostMapping("/lock")
    public R<String> lock(@RequestParam String key) {
        try {
            String result = redisLockService.executeWithLock(key, 5, 10, TimeUnit.SECONDS, () -> {
                try {
                    // 模拟持有锁期间的业务处理耗时
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "获取锁成功并执行完毕";
            });
            return R.ok(result);
        } catch (RuntimeException e) {
            return R.fail("获取锁失败");
        }
    }
}
