package com.oinsist.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户角色关联实体（多对多中间表）
 * <p>
 * 不继承 BaseEntity：关联表只保存映射关系，无需审计字段和逻辑删除。
 * </p>
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    /** 用户ID */
    private Long userId;

    /** 角色ID */
    private Long roleId;

    /**
     * 租户 ID（多租户行级隔离标识）
     * 关联表也需要 tenant_id 以确保 TenantLineInnerInterceptor 能正确改写 SQL
     */
    private Long tenantId;
}
