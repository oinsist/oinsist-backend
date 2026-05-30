package com.oinsist.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.oinsist.common.core.domain.R;
import com.oinsist.common.log.annotation.OperLog;
import com.oinsist.common.log.enums.BusinessType;
import com.oinsist.common.mybatis.domain.PageQuery;
import com.oinsist.common.mybatis.domain.PageResult;
import com.oinsist.system.domain.dto.SysRoleAddDto;
import com.oinsist.system.domain.dto.SysRoleEditDto;
import com.oinsist.system.domain.vo.SysRoleVo;
import com.oinsist.system.service.SysRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 角色管理 Controller
 * 提供角色的分页查询、新增、修改、删除及菜单分配功能
 * 所有接口均通过 @SaCheckPermission 进行细粒度权限控制
 */
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    /** 角色分页列表 */
    @SaCheckPermission("system:role:list")
    @GetMapping("/list")
    public R<PageResult<SysRoleVo>> list(PageQuery pageQuery) {
        return R.ok(sysRoleService.listRoles(pageQuery));
    }

    /** 角色详情 */
    @SaCheckPermission("system:role:query")
    @GetMapping("/{roleId}")
    public R<SysRoleVo> getInfo(@PathVariable Long roleId) {
        return R.ok(sysRoleService.selectById(roleId));
    }

    /** 新增角色 */
    @SaCheckPermission("system:role:add")
    @OperLog(title = "角色管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Valid @RequestBody SysRoleAddDto dto) {
        sysRoleService.addRole(dto);
        return R.ok();
    }

    /** 修改角色 */
    @SaCheckPermission("system:role:edit")
    @OperLog(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Valid @RequestBody SysRoleEditDto dto) {
        sysRoleService.editRole(dto);
        return R.ok();
    }

    /** 删除角色 */
    @SaCheckPermission("system:role:remove")
    @OperLog(title = "角色管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roleId}")
    public R<Void> remove(@PathVariable Long roleId) {
        sysRoleService.deleteRole(roleId);
        return R.ok();
    }

    /** 分配菜单 */
    @SaCheckPermission("system:role:assignMenu")
    @OperLog(title = "角色管理", businessType = BusinessType.GRANT)
    @PutMapping("/{roleId}/assignMenu")
    public R<Void> assignMenu(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        sysRoleService.assignMenus(roleId, menuIds);
        return R.ok();
    }
}
