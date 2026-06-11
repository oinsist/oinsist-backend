package com.oinsist.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户列表视图
 */
@Data
public class SysTenantVo {

    private Long tenantId;

    private String tenantName;

    private String contact;

    private String status;

    private LocalDateTime createTime;
}
