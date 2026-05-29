package com.oinsist.common.redis.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务 —— 基于 Redisson {@link RLock} 的轻量封装。
 * <p>
 * <b>核心场景：</b>
 * <ul>
 *     <li>防止并发重复操作（如订单重复支付、接口幂等控制）</li>
 *     <li>保护共享资源的互斥访问（如库存扣减、余额变更）</li>
 *     <li>分布式定时任务防重复执行</li>
 * </ul>
 * </p>
 * <p>
 * Redisson 的 RLock 底层基于 Redis 的 HASH + Lua 脚本实现可重入锁，
 * 内置 Watch Dog 机制可自动续期（当 leaseTime 设为 -1 时），
 * 本封装要求显式传入 leaseTime 以便学习者理解锁的生命周期管理。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class RedisLockService {

    private final RedissonClient redissonClient;

    /**
     * 尝试获取分布式锁（非阻塞模式）。
     * <p>
     * <b>参数说明：</b>
     * <ul>
     *     <li>{@code waitTime}：等待获取锁的最大时间。如果在此时间内未能获取到锁，则返回 false。
     *         设为 0 表示立即返回（不等待）。</li>
     *     <li>{@code leaseTime}：持有锁的最大时间，到期后 Redisson 自动释放锁，
     *         从根本上防止因程序异常/宕机导致的死锁问题。</li>
     * </ul>
     * </p>
     *
     * @param key       锁的唯一标识（通常为业务维度拼接，如 "order:pay:" + orderId）
     * @param waitTime  等待获取锁的最大时间
     * @param leaseTime 持有锁的最大时间（到期自动释放，防止死锁）
     * @param unit      时间单位
     * @return true 表示成功获取锁，false 表示获取失败
     */
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(key);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            // 恢复中断状态，让上层调用者能感知到线程被中断
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放分布式锁。
     * <p>
     * <b>为什么必须判断 {@code isHeldByCurrentThread()}：</b>
     * Redisson 的锁是可重入的，并且绑定到特定线程。如果当前线程并未持有该锁
     * （可能是锁已超时被自动释放，或者是其他线程持有），直接调用 unlock() 会抛出
     * {@link IllegalMonitorStateException}。因此必须先判断当前线程是否持锁。
     * </p>
     * <p>
     * <b>最佳实践：</b>unlock 应放在 finally 块中调用，确保即使业务逻辑抛异常也能释放锁。
     * </p>
     *
     * @param key 锁的唯一标识
     */
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        // 只有当前线程确实持有该锁时才释放，避免释放别人的锁
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 分布式锁安全模板方法 —— 推荐的使用方式。
     * <p>
     * 相比手动编写 tryLock + try-finally + unlock，本方法将加锁/执行/释放三步
     * 封装为一个原子化模板，避免开发者遗漏 finally 释放锁的常见错误。
     * </p>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * String result = redisLockService.executeWithLock(
     *     "order:pay:" + orderId, 3, 10, TimeUnit.SECONDS,
     *     () -> orderService.doPay(orderId)
     * );
     * }</pre>
     * </p>
     *
     * @param key       锁的唯一标识
     * @param waitTime  等待获取锁的最大时间
     * @param leaseTime 持有锁的最大时间
     * @param unit      时间单位
     * @param task      获取锁成功后要执行的业务逻辑
     * @param <T>       业务返回值类型
     * @return 业务逻辑的返回值
     * @throws RuntimeException 获取锁失败时抛出
     */
    public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> task) {
        boolean locked = tryLock(key, waitTime, leaseTime, unit);
        if (!locked) {
            throw new RuntimeException("获取分布式锁失败: " + key);
        }
        try {
            return task.get();
        } finally {
            unlock(key);
        }
    }
}
