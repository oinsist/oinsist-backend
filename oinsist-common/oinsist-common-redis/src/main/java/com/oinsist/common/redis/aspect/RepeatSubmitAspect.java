package com.oinsist.common.redis.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oinsist.common.core.enums.ResultCode;
import com.oinsist.common.core.exception.ServiceException;
import com.oinsist.common.core.service.CurrentUserProvider;
import com.oinsist.common.redis.annotation.RepeatSubmit;
import com.oinsist.common.redis.service.RedisService;
import com.oinsist.common.redis.support.RedisKeyBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 防重复提交切面
 * <p>
 * 拦截 @RepeatSubmit 标注的 Controller 方法，在短时间窗口内阻止同一用户
 * 对同一接口提交相同参数的重复请求。
 * </p>
 * <p>
 * 设计要点：
 * 1. 唯一标识 = 用户ID(或anonymous) + 请求URI + 请求方法 + 参数MD5摘要
 * 2. 参数使用 Jackson 序列化为稳定 JSON 后取 MD5，避免 toString() 不稳定问题
 * 3. 序列化时排除 HttpServletRequest/Response、MultipartFile 等不可序列化对象
 * 4. 短 TTL（默认5秒）自动过期，无需手动清理
 * 5. @Order(2) 与幂等切面同优先级层次，在分布式锁之前执行
 * 6. 本注解仅适用于防抖场景，不适用于支付/下单等强一致性场景
 * 7. 通过 ObjectProvider 延迟获取 CurrentUserProvider，不强制要求认证模块存在
 * </p>
 */
@Slf4j
@Aspect
@Component
@Order(2)
public class RepeatSubmitAspect implements Ordered {
    
    private final RedisService redisService;
    private final ObjectProvider<CurrentUserProvider> currentUserProviderProvider;
    private final ObjectMapper objectMapper;
    
    public RepeatSubmitAspect(RedisService redisService,
                              ObjectProvider<CurrentUserProvider> currentUserProviderProvider,
                              ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.currentUserProviderProvider = currentUserProviderProvider;
        this.objectMapper = objectMapper;
    }
    
    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, RepeatSubmit repeatSubmit) throws Throwable {
        // 获取当前 HTTP 请求上下文
        ServletRequestAttributes attributes = (ServletRequestAttributes) 
                RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            // 非 Web 请求环境，跳过防重检查
            return joinPoint.proceed();
        }
        
        HttpServletRequest request = attributes.getRequest();
        
        // 通过 ObjectProvider 延迟获取用户标识，无认证模块时降级为 "anonymous"
        String userIdentifier = resolveUserIdentifier();
        
        // 构建唯一标识：用户标识 + URI + Method + 参数摘要
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String argsDigest = buildArgsDigest(joinPoint.getArgs());
        
        // 组装 Redis key
        String redisKey = RedisKeyBuilder.buildKey(
                RedisKeyBuilder.REPEAT_SUBMIT_PREFIX,
                userIdentifier, uri, method, argsDigest);
        
        // 原子写入防重标记（SET NX + TTL），保证并发安全
        boolean success = redisService.setIfAbsent(redisKey, "1", repeatSubmit.interval(), repeatSubmit.timeUnit());
        if (!success) {
            log.info("防重复提交拦截：用户={}, URI={}, Method={}", userIdentifier, uri, method);
            throw new ServiceException(ResultCode.TOO_MANY_REQUESTS, repeatSubmit.message());
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 获取用户标识
     * <p>
     * 通过 ObjectProvider 安全获取 CurrentUserProvider：
     * - 若认证模块存在且用户已登录：返回用户ID
     * - 若认证模块不存在或用户未登录：降级为 "anonymous"
     * 这样保证 common-redis 单独使用时不会因缺少 Bean 启动失败。
     * </p>
     */
    private String resolveUserIdentifier() {
        CurrentUserProvider provider = currentUserProviderProvider.getIfAvailable();
        if (provider != null) {
            Long userId = provider.getUserId();
            if (userId != null) {
                return String.valueOf(userId);
            }
        }
        return "anonymous";
    }
    
    /**
     * 构建参数摘要
     * <p>
     * 使用 Jackson ObjectMapper 将参数序列化为稳定的 JSON 字符串，再取 MD5 摘要。
     * 相比 toString()，JSON 序列化不依赖 DTO 是否重写了 toString() 方法，
     * 保证同一内容的请求始终生成相同的摘要值。
     * </p>
     * <p>
     * 过滤规则：排除 HttpServletRequest/Response、MultipartFile 等不可序列化且不稳定的对象。
     * </p>
     */
    private String buildArgsDigest(Object[] args) {
        if (args == null || args.length == 0) {
            return "empty";
        }
        
        List<Object> stableArgs = Arrays.stream(args)
                .filter(arg -> arg != null)
                .filter(arg -> !(arg instanceof HttpServletRequest))
                .filter(arg -> !(arg instanceof HttpServletResponse))
                .filter(arg -> !(arg instanceof MultipartFile))
                .toList();
        
        if (stableArgs.isEmpty()) {
            return "empty";
        }
        
        try {
            // 使用 Jackson 序列化为稳定 JSON，保证同一内容生成相同字符串
            String json = objectMapper.writeValueAsString(stableArgs);
            return RedisKeyBuilder.md5Digest(json);
        } catch (JsonProcessingException e) {
            // 序列化失败时降级为类名+hashCode组合，仍保持一定防重能力
            log.warn("参数 JSON 序列化失败，降级为 hashCode 摘要", e);
            String fallback = stableArgs.stream()
                    .map(arg -> arg.getClass().getSimpleName() + ":" + arg.hashCode())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("unknown");
            return RedisKeyBuilder.md5Digest(fallback);
        }
    }
    
    @Override
    public int getOrder() {
        return 2;
    }
}
