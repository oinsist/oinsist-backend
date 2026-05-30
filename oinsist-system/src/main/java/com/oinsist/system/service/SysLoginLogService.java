package com.oinsist.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oinsist.common.mybatis.domain.PageQuery;
import com.oinsist.common.mybatis.domain.PageResult;
import com.oinsist.system.domain.SysLoginLog;
import com.oinsist.system.mapper.SysLoginLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志 Service
 */
@Service
@RequiredArgsConstructor
public class SysLoginLogService {

    private final SysLoginLogMapper sysLoginLogMapper;

    /**
     * 记录登录日志
     *
     * @param username  登录账号
     * @param userId    用户ID（成功时非 null）
     * @param status    状态（0=成功 1=失败）
     * @param msg       提示消息
     * @param ip        登录IP
     * @param userAgent 浏览器UA
     */
    public void recordLoginLog(String username, Long userId, Integer status,
                               String msg, String ip, String userAgent) {
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUsername(username);
        loginLog.setUserId(userId);
        loginLog.setStatus(status);
        loginLog.setMsg(msg);
        loginLog.setIp(ip);
        loginLog.setUserAgent(userAgent);
        loginLog.setLoginTime(LocalDateTime.now());
        sysLoginLogMapper.insert(loginLog);
    }

    /**
     * 分页查询登录日志（按登录时间倒序）
     */
    public PageResult<SysLoginLog> listLoginLogs(PageQuery pageQuery) {
        Page<SysLoginLog> page = sysLoginLogMapper.selectPage(
                pageQuery.buildPage(),
                new LambdaQueryWrapper<SysLoginLog>()
                        .orderByDesc(SysLoginLog::getLoginTime)
        );
        return PageResult.build(page);
    }

    /**
     * 批量删除登录日志
     */
    public void deleteByIds(List<Long> loginIds) {
        sysLoginLogMapper.deleteByIds(loginIds);
    }
}
