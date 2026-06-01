package com.oinsist.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oinsist.common.mybatis.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /** 角色ID（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long roleId;

    /** 角色名称 */
    private String roleName;

    /** 角色标识（如 admin、common） */
    private String roleKey;

    /** 状态（0正常 1停用） */
    private String status;

    /** 数据范围（ALL=全部, CUSTOM=自定义, DEPT=本部门, DEPT_AND_CHILD=本部门及以下, SELF=仅本人） */
    private String dataScope;

    /**
     * 租户 ID（多租户行级隔离标识）
     */
    private Long tenantId;
}
