package com.oinsist.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.oinsist.common.core.domain.R;
import com.oinsist.common.log.annotation.OperLog;
import com.oinsist.common.log.enums.BusinessType;
import com.oinsist.common.mybatis.domain.PageQuery;
import com.oinsist.common.mybatis.domain.PageResult;
import com.oinsist.system.domain.dto.SysUserAddDto;
import com.oinsist.system.domain.dto.SysUserEditDto;
import com.oinsist.system.domain.vo.SysUserVo;
import com.oinsist.system.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 用户管理 Controller
 * 提供用户的分页查询、新增、修改、删除及角色分配功能
 * 所有接口均通过 @SaCheckPermission 进行细粒度权限控制
 */
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    /** 用户分页列表 */
    @SaCheckPermission("system:user:list")
    @GetMapping("/list")
    public R<PageResult<SysUserVo>> list(PageQuery pageQuery) {
        return R.ok(sysUserService.listUsers(pageQuery));
    }

    /** 用户详情 */
    @SaCheckPermission("system:user:query")
    @GetMapping("/{userId}")
    public R<SysUserVo> getInfo(@PathVariable Long userId) {
        return R.ok(sysUserService.selectById(userId));
    }

    /** 新增用户 */
    @SaCheckPermission("system:user:add")
    @OperLog(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Valid @RequestBody SysUserAddDto dto) {
        sysUserService.addUser(dto);
        return R.ok();
    }

    /** 修改用户 */
    @SaCheckPermission("system:user:edit")
    @OperLog(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Valid @RequestBody SysUserEditDto dto) {
        sysUserService.editUser(dto);
        return R.ok();
    }

    /** 删除用户 */
    @SaCheckPermission("system:user:remove")
    @OperLog(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userId}")
    public R<Void> remove(@PathVariable Long userId) {
        sysUserService.deleteUser(userId);
        return R.ok();
    }

    /** 查询用户当前角色ID集合，用于分配角色弹窗预勾选 */
    @SaCheckPermission("system:user:query")
    @GetMapping("/{userId}/roleIds")
    public R<List<Long>> roleIds(@PathVariable Long userId) {
        return R.ok(sysUserService.listRoleIdsByUserId(userId));
    }

    /** 分配角色 */
    @SaCheckPermission("system:user:assignRole")
    @OperLog(title = "用户管理", businessType = BusinessType.GRANT)
    @PutMapping("/{userId}/assignRole")
    public R<Void> assignRole(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        sysUserService.assignRoles(userId, roleIds);
        return R.ok();
    }
}
