package com.oinsist.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增菜单请求 DTO
 * <p>
 * 菜单是前端路由与后端权限的桥梁：
 * - menuType=M 表示目录（不渲染组件，仅做路由分组）
 * - menuType=C 表示菜单（对应一个前端页面组件）
 * - menuType=F 表示按钮（仅做权限标识，不参与路由）
 * </p>
 */
@Data
public class SysMenuAddDto {

    /** 菜单名称 */
    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    /** 父菜单ID（0=顶级） */
    private Long parentId;

    /** 显示顺序 */
    private Integer orderNum;

    /** 路由地址 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 权限标识（如 system:user:list） */
    private String perms;

    /** 菜单类型（M=目录 C=菜单 F=按钮） */
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    /** 是否可见（0=可见 1=隐藏） */
    private String visible;

    /** 状态（0=正常 1=停用） */
    private String status;

    /** 菜单图标 */
    private String icon;
}
