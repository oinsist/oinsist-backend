package com.oinsist.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.oinsist.common.core.domain.R;
import com.oinsist.common.mybatis.domain.PageQuery;
import com.oinsist.common.mybatis.domain.PageResult;
import com.oinsist.system.domain.SysLoginLog;
import com.oinsist.system.service.SysLoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 登录日志管理 Controller
 * <p>
 * 提供登录日志的分页查询和批量删除功能。
 * 登录日志由登录流程（SysLoginService）自动记录。
 */
@RestController
@RequestMapping("/system/loginLog")
@RequiredArgsConstructor
public class SysLoginLogController {

    private final SysLoginLogService sysLoginLogService;

    /**
     * 分页查询登录日志
     */
    @SaCheckPermission("system:loginLog:list")
    @GetMapping("/list")
    public R<PageResult<SysLoginLog>> list(PageQuery pageQuery) {
        return R.ok(sysLoginLogService.listLoginLogs(pageQuery));
    }

    /**
     * 批量删除登录日志
     */
    @SaCheckPermission("system:loginLog:remove")
    @DeleteMapping("/{loginIds}")
    public R<Void> remove(@PathVariable List<Long> loginIds) {
        sysLoginLogService.deleteByIds(loginIds);
        return R.ok();
    }
}
