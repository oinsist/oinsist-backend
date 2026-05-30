package com.oinsist.common.log.event;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志事件对象
 * <p>
 * 由 AOP 切面采集并通过 {@link org.springframework.context.ApplicationEventPublisher} 发布，
 * 业务模块（oinsist-system）通过 {@code @EventListener} 监听此事件并持久化到数据库。
 * <p>
 * 架构意义：
 * <ul>
 *   <li>将"日志采集"与"日志存储"完全解耦</li>
 *   <li>common-log 模块无需知道日志最终存到哪里（数据库/ES/文件）</li>
 *   <li>监听器内部做降级处理，落库失败不影响主业务</li>
 * </ul>
 */
@Data
public class OperLogEvent {

    /** 模块标题 */
    private String title;

    /** 业务类型（对应 BusinessType 枚举的 ordinal） */
    private Integer businessType;

    /** 被调用的方法全路径（如 com.oinsist.admin.controller.SysUserController.add） */
    private String method;

    /** HTTP 请求方法（GET/POST/PUT/DELETE） */
    private String requestMethod;

    /** 请求 URL */
    private String requestUrl;

    /** 请求参数（已脱敏的 JSON 字符串） */
    private String requestParam;

    /** 操作状态（0=成功，1=异常） */
    private Integer status;

    /** 错误消息（异常时记录） */
    private String errorMsg;

    /** 操作人 ID */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 请求 IP */
    private String ip;

    /** 方法执行耗时（毫秒） */
    private Long duration;

    /** 操作时间 */
    private LocalDateTime operTime;
}
