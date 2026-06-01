package com.oinsist.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色-部门关联实体（用于自定义数据范围）
 */
@Data
@TableName("sys_role_dept")
public class SysRoleDept {

    /** 角色ID */
    private Long roleId;

    /** 部门ID */
    private Long deptId;

    /**
     * 租户 ID（多租户行级隔离标识）
     */
    private Long tenantId;
}
