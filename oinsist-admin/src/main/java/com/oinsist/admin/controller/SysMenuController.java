package com.oinsist.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.oinsist.common.core.domain.R;
import com.oinsist.system.domain.SysMenu;
import com.oinsist.system.domain.dto.SysMenuAddDto;
import com.oinsist.system.domain.dto.SysMenuEditDto;
import com.oinsist.system.service.SysMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 菜单管理 Controller
 * 提供菜单的树形查询、新增、修改、删除功能
 * 所有接口均通过 @SaCheckPermission 进行细粒度权限控制
 */
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    /** 菜单树列表（全量） */
    @SaCheckPermission("system:menu:list")
    @GetMapping("/list")
    public R<List<SysMenu>> list() {
        return R.ok(sysMenuService.listMenuTree());
    }

    /** 菜单详情 */
    @SaCheckPermission("system:menu:query")
    @GetMapping("/{menuId}")
    public R<SysMenu> getInfo(@PathVariable Long menuId) {
        return R.ok(sysMenuService.selectById(menuId));
    }

    /** 新增菜单 */
    @SaCheckPermission("system:menu:add")
    @PostMapping
    public R<Void> add(@Valid @RequestBody SysMenuAddDto dto) {
        sysMenuService.addMenu(dto);
        return R.ok();
    }

    /** 修改菜单 */
    @SaCheckPermission("system:menu:edit")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody SysMenuEditDto dto) {
        sysMenuService.editMenu(dto);
        return R.ok();
    }

    /** 删除菜单 */
    @SaCheckPermission("system:menu:remove")
    @DeleteMapping("/{menuId}")
    public R<Void> remove(@PathVariable Long menuId) {
        sysMenuService.deleteMenu(menuId);
        return R.ok();
    }
}
