package com.oinsist.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.oinsist.common.core.domain.R;
import com.oinsist.common.mybatis.domain.PageQuery;
import com.oinsist.common.mybatis.domain.PageResult;
import com.oinsist.system.domain.SysOperLog;
import com.oinsist.system.service.SysOperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志管理 Controller
 * <p>
 * 提供操作日志的分页查询和批量删除功能。
 * 操作日志由 AOP 切面自动采集，此处仅负责查询展示。
 */
@RestController
@RequestMapping("/system/operLog")
@RequiredArgsConstructor
public class SysOperLogController {

    private final SysOperLogService sysOperLogService;

    /**
     * 分页查询操作日志
     */
    @SaCheckPermission("system:operLog:list")
    @GetMapping("/list")
    public R<PageResult<SysOperLog>> list(PageQuery pageQuery) {
        return R.ok(sysOperLogService.listOperLogs(pageQuery));
    }

    /**
     * 批量删除操作日志
     */
    @SaCheckPermission("system:operLog:remove")
    @DeleteMapping("/{operIds}")
    public R<Void> remove(@PathVariable List<Long> operIds) {
        sysOperLogService.deleteByIds(operIds);
        return R.ok();
    }
}
