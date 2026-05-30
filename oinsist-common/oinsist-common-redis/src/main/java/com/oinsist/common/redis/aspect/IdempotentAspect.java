package com.oinsist.common.redis.aspect;

import com.oinsist.common.core.enums.ResultCode;
import com.oinsist.common.core.exception.ServiceException;
import com.oinsist.common.redis.annotation.Idempotent;
import com.oinsist.common.redis.service.RedisService;
import com.oinsist.common.redis.support.RedisKeyBuilder;
import com.oinsist.common.redis.support.SpelKeyResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 幂等性切面
 * <p>
 * 拦截 @Idempotent 标注的方法，通过 Redis 占位 key 实现请求幂等。
 * 在业务执行前写入 Redis 标记（setIfAbsent 语义），若 key 已存在
 * 则说明是重复请求，直接拒绝。
 * </p>
 * <p>
 * 设计要点：
 * 1. key 生成策略：优先 SpEL，其次请求头 Idempotency-Key（适配 RFC 草案标准）
 * 2. 使用 Redis 的原子性 SET NX 操作保证并发安全
 * 3. TTL 强制存在，禁止产生永久 key 污染 Redis
 * 4. @Order(1) 保证在分布式锁之前执行，先过滤重复请求再进入临界区
 * </p>
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class IdempotentAspect implements Ordered {
    
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    
    private final RedisService redisService;
    private final SpelKeyResolver spelKeyResolver;
    
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 生成幂等 key：优先 SpEL 表达式，其次请求头
        String idempotentKey = resolveIdempotentKey(idempotent, joinPoint);
        
        if (!StringUtils.hasText(idempotentKey)) {
            throw new ServiceException(ResultCode.BAD_REQUEST, "幂等 key 不能为空，请通过 SpEL 或请求头 Idempotency-Key 提供");
        }
        
        // 构建完整 Redis key
        String redisKey = RedisKeyBuilder.buildKey(idempotent.prefix(), idempotentKey);
        
        // 原子写入占位 key（SET NX + TTL），保证并发安全
        // 返回 true 表示首次写入成功，返回 false 表示 key 已存在（重复请求）
        boolean success = redisService.setIfAbsent(redisKey, "1", idempotent.ttl(), idempotent.timeUnit());
        if (!success) {
            log.info("幂等拦截：重复请求, key={}", redisKey);
            throw new ServiceException(ResultCode.TOO_MANY_REQUESTS, idempotent.message());
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 解析幂等 key
     * 优先级：SpEL 表达式 > 请求头 Idempotency-Key
     */
    private String resolveIdempotentKey(Idempotent idempotent, ProceedingJoinPoint joinPoint) {
        // 优先使用 SpEL 表达式
        if (StringUtils.hasText(idempotent.key())) {
            return spelKeyResolver.resolve(idempotent.key(), joinPoint);
        }
        
        // 降级到请求头 Idempotency-Key
        ServletRequestAttributes attributes = (ServletRequestAttributes) 
                RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return request.getHeader(IDEMPOTENCY_HEADER);
        }
        
        return null;
    }
    
    @Override
    public int getOrder() {
        return 1;
    }
}
