package com.oinsist.common.redis.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 * <p>
 * 标注在方法上，通过 AOP 切面自动获取/释放 Redisson 分布式锁。
 * 适用于需要互斥执行的临界区方法，如库存扣减、订单创建等并发场景。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisLock {
    
    /** 锁的 key，支持 SpEL 表达式（如 "#userId"） */
    String key();
    
    /** key 前缀，最终 Redis key = prefix + 解析后的 key */
    String prefix() default "lock:";
    
    /** 获取锁的最大等待时间，默认 3 秒 */
    long waitTime() default 3;
    
    /** 锁的自动释放时间（租约），默认 30 秒，防止死锁 */
    long leaseTime() default 30;
    
    /** 时间单位，默认秒 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /** 获取锁失败时的提示消息 */
    String message() default "请求过于频繁，请稍后重试";
}
