package com.oinsist.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oinsist.common.mybatis.domain.PageQuery;
import com.oinsist.common.mybatis.domain.PageResult;
import com.oinsist.system.domain.SysOperLog;
import com.oinsist.system.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志 Service
 */
@Service
@RequiredArgsConstructor
public class SysOperLogService {

    private final SysOperLogMapper sysOperLogMapper;

    /**
     * 新增操作日志
     */
    public void insertOperLog(SysOperLog operLog) {
        sysOperLogMapper.insert(operLog);
    }

    /**
     * 分页查询操作日志（按操作时间倒序）
     */
    public PageResult<SysOperLog> listOperLogs(PageQuery pageQuery) {
        Page<SysOperLog> page = sysOperLogMapper.selectPage(
                pageQuery.buildPage(),
                new LambdaQueryWrapper<SysOperLog>()
                        .orderByDesc(SysOperLog::getOperTime)
        );
        return PageResult.build(page);
    }

    /**
     * 批量删除操作日志
     */
    public void deleteByIds(List<Long> operIds) {
        sysOperLogMapper.deleteByIds(operIds);
    }
}
