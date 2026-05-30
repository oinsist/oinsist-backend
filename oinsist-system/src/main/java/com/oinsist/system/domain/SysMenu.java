package com.oinsist.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oinsist.common.mybatis.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 菜单权限实体
 * <p>
 * 菜单类型约定：
 * - M：目录（一级导航，不对应具体页面组件）
 * - C：菜单（对应一个具体页面组件，有 path 和 component）
 * - F：按钮（不在侧边栏展示，仅作为权限标识用于细粒度控制）
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    /** 菜单ID（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long menuId;

    /** 菜单名称 */
    private String menuName;

    /** 父菜单ID（0表示顶级） */
    private Long parentId;

    /** 显示顺序 */
    private Integer orderNum;

    /** 路由地址 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 权限标识（如 system:user:list） */
    private String perms;

    /** 菜单类型（M目录 C菜单 F按钮） */
    private String menuType;

    /** 是否可见（0可见 1隐藏） */
    private String visible;

    /** 状态（0正常 1停用） */
    private String status;

    /** 菜单图标 */
    private String icon;

    /** 子菜单（非数据库字段，用于构建树形结构） */
    @TableField(exist = false)
    private List<SysMenu> children;
}
