package com.oinsist.common.redis.aspect;

import com.oinsist.common.core.enums.ResultCode;
import com.oinsist.common.core.exception.ServiceException;
import com.oinsist.common.redis.annotation.RedisLock;
import com.oinsist.common.redis.service.RedisLockService;
import com.oinsist.common.redis.support.RedisKeyBuilder;
import com.oinsist.common.redis.support.SpelKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 分布式锁切面
 * <p>
 * 拦截 @RedisLock 标注的方法，在执行前自动获取 Redisson 分布式锁，
 * 执行后在 finally 中安全释放。适用于保护临界区资源的并发访问控制。
 * </p>
 * <p>
 * 设计要点：
 * 1. @Order(10) 保证在幂等/防重切面之后执行，只保护真正需要互斥的临界区
 * 2. finally 中释放锁，确保即使业务异常也不会死锁
 * 3. 获取锁失败不重试，直接拒绝并抛出 429 语义异常
 * </p>
 */
@Slf4j
@Aspect
@Component
@Order(10)
@RequiredArgsConstructor
public class RedisLockAspect implements Ordered {
    
    private final RedisLockService redisLockService;
    private final SpelKeyResolver spelKeyResolver;
    
    @Around("@annotation(redisLock)")
    public Object around(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
        // 解析 SpEL 表达式得到实际 key 值
        String resolvedKey = spelKeyResolver.resolve(redisLock.key(), joinPoint);
        // 构建完整 Redis key：prefix + 解析后的 key
        String lockKey = RedisKeyBuilder.buildKey(redisLock.prefix(), resolvedKey);
        
        // 尝试获取分布式锁，在 waitTime 内等待
        boolean locked = redisLockService.tryLock(
                lockKey, redisLock.waitTime(), redisLock.leaseTime(), redisLock.timeUnit());
        
        if (!locked) {
            log.warn("获取分布式锁失败, key={}", lockKey);
            throw new ServiceException(ResultCode.TOO_MANY_REQUESTS, redisLock.message());
        }
        
        try {
            return joinPoint.proceed();
        } finally {
            // 释放锁（RedisLockService.unlock 内部已包含 isHeldByCurrentThread 校验）
            redisLockService.unlock(lockKey);
        }
    }
    
    @Override
    public int getOrder() {
        return 10;
    }
}
