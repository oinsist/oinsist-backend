package com.oinsist.common.redis.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交注解
 * <p>
 * 在指定时间间隔内，同一用户对同一接口的相同参数请求仅放行一次。
 * 默认按 用户ID + URI + Method + 参数摘要（MD5）生成唯一标识。
 * 适用于表单提交、按钮防抖等场景（非强一致性场景）。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {
    
    /** 防重间隔时间，默认 5 秒 */
    int interval() default 5;
    
    /** 时间单位，默认秒 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /** 重复提交时的提示消息 */
    String message() default "请勿重复提交，请稍后再试";
}
