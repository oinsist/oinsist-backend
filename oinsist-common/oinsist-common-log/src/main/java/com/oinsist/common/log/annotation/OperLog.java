package com.oinsist.common.log.annotation;

import com.oinsist.common.log.enums.BusinessType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * <p>
 * 标注在 Controller 方法上，AOP 切面会自动拦截并记录：
 * 请求路径、方法名、请求参数、执行耗时、异常信息、当前操作人等上下文。
 * <p>
 * 设计参考 RuoYi-Vue-Plus 的 {@code @Log} 注解，精简为最核心的三个属性，
 * 避免过度配置增加使用负担。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /**
     * 模块标题（如："用户管理"、"角色管理"）
     */
    String title() default "";

    /**
     * 业务操作类型
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * 是否保存请求参数
     * <p>
     * 对于参数体积过大或包含文件上传的接口，可设为 false 避免存储压力
     */
    boolean isSaveRequestData() default true;
}
