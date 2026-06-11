package com.oinsist.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.oinsist.common.core.domain.R;
import com.oinsist.common.log.annotation.OperLog;
import com.oinsist.common.log.enums.BusinessType;
import com.oinsist.common.mybatis.domain.PageQuery;
import com.oinsist.common.mybatis.domain.PageResult;
import com.oinsist.system.domain.dto.SysTenantAddDto;
import com.oinsist.system.domain.dto.SysTenantEditDto;
import com.oinsist.system.domain.vo.SysTenantVo;
import com.oinsist.system.service.SysTenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户管理 Controller
 */
@RestController
@RequestMapping("/system/tenant")
@RequiredArgsConstructor
public class SysTenantController {

    private final SysTenantService sysTenantService;

    /** 租户分页列表 */
    @SaCheckPermission("system:tenant:list")
    @GetMapping("/list")
    public R<PageResult<SysTenantVo>> list(PageQuery pageQuery) {
        return R.ok(sysTenantService.listTenants(pageQuery));
    }

    /** 租户详情 */
    @SaCheckPermission("system:tenant:query")
    @GetMapping("/{tenantId}")
    public R<SysTenantVo> getInfo(@PathVariable Long tenantId) {
        return R.ok(sysTenantService.selectById(tenantId));
    }

    /** 新增租户 */
    @SaCheckPermission("system:tenant:add")
    @OperLog(title = "租户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Valid @RequestBody SysTenantAddDto dto) {
        sysTenantService.addTenant(dto);
        return R.ok();
    }

    /** 修改租户 */
    @SaCheckPermission("system:tenant:edit")
    @OperLog(title = "租户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Valid @RequestBody SysTenantEditDto dto) {
        sysTenantService.editTenant(dto);
        return R.ok();
    }

    /** 删除租户 */
    @SaCheckPermission("system:tenant:remove")
    @OperLog(title = "租户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tenantId}")
    public R<Void> remove(@PathVariable Long tenantId) {
        sysTenantService.deleteTenant(tenantId);
        return R.ok();
    }
}
