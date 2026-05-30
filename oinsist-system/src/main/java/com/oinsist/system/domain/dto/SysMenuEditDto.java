package com.oinsist.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 编辑菜单请求 DTO
 * <p>
 * 编辑菜单时必须携带 menuId 以定位目标记录，
 * 其余字段与新增一致，menuName 和 menuType 为必填项。
 * </p>
 */
@Data
public class SysMenuEditDto {

    /** 菜单ID（主键，必传） */
    @NotNull(message = "菜单ID不能为空")
    private Long menuId;

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
