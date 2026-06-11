package com.oinsist.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oinsist.common.mybatis.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class SysTenant extends BaseEntity {

    /** 租户ID（雪花算法，默认租户固定为 1） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long tenantId;

    /** 租户名称 */
    private String tenantName;

    /** 联系人 */
    private String contact;

    /** 状态（0正常 1停用） */
    private String status;
}
