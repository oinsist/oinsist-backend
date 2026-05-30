package com.oinsist.common.redis.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 幂等性注解
 * <p>
 * 保证相同请求在指定时间窗口内只执行一次业务逻辑。
 * key 生成策略：优先使用 SpEL 表达式；若 key 为空，则从请求头 Idempotency-Key 获取。
 * 适用于需要客户端保证幂等的接口（如支付回调、消息消费）。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    
    /** 幂等 key，支持 SpEL 表达式；为空时从请求头 Idempotency-Key 获取 */
    String key() default "";
    
    /** key 前缀 */
    String prefix() default "idempotent:";
    
    /** 幂等标记的过期时间，默认 30 分钟 */
    long ttl() default 30;
    
    /** 时间单位，默认分钟 */
    TimeUnit timeUnit() default TimeUnit.MINUTES;
    
    /** 重复请求时的提示消息 */
    String message() default "请勿重复提交";
}
