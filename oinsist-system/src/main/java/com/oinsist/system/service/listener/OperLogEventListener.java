package com.oinsist.system.service.listener;

import com.oinsist.common.log.event.OperLogEvent;
import com.oinsist.system.domain.SysOperLog;
import com.oinsist.system.service.SysOperLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 操作日志事件监听器
 * <p>
 * 消费 oinsist-common-log 模块通过 Spring Event 发布的 {@link OperLogEvent}，
 * 将日志数据转换为实体并持久化到数据库。
 * <p>
 * 降级策略：日志落库失败时仅打印 error 日志，绝不影响主业务流程。
 * 这是因为操作日志属于"辅助审计"而非"核心业务"，其失败不应导致用户请求失败。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperLogEventListener {

    private final SysOperLogService sysOperLogService;

    /**
     * 监听操作日志事件并落库
     */
    @EventListener
    public void onOperLogEvent(OperLogEvent event) {
        try {
            SysOperLog operLog = convertToEntity(event);
            sysOperLogService.insertOperLog(operLog);
        } catch (Exception e) {
            // 降级处理：日志落库失败不影响主业务
            log.error("操作日志落库失败: title={}, method={}", event.getTitle(), event.getMethod(), e);
        }
    }

    /**
     * 将事件对象转换为数据库实体
     */
    private SysOperLog convertToEntity(OperLogEvent event) {
        SysOperLog operLog = new SysOperLog();
        operLog.setTitle(event.getTitle());
        operLog.setBusinessType(event.getBusinessType());
        operLog.setMethod(event.getMethod());
        operLog.setRequestMethod(event.getRequestMethod());
        operLog.setRequestUrl(event.getRequestUrl());
        operLog.setRequestParam(event.getRequestParam());
        operLog.setStatus(event.getStatus());
        operLog.setErrorMsg(event.getErrorMsg());
        operLog.setUserId(event.getUserId());
        operLog.setUsername(event.getUsername());
        operLog.setIp(event.getIp());
        operLog.setDuration(event.getDuration());
        operLog.setOperTime(event.getOperTime());
        return operLog;
    }
}
