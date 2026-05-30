package com.oinsist.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色菜单关联实体（多对多中间表）
 * <p>
 * 不继承 BaseEntity：关联表只保存映射关系，无需审计字段和逻辑删除。
 * </p>
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu {

    /** 角色ID */
    private Long roleId;

    /** 菜单ID */
    private Long menuId;
}
