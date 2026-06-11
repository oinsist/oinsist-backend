package com.oinsist.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改租户请求
 */
@Data
public class SysTenantEditDto {

    /** 租户ID */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    /** 租户名称 */
    @NotBlank(message = "租户名称不能为空")
    private String tenantName;

    /** 联系人 */
    private String contact;

    /** 状态（0正常 1停用） */
    private String status;
}
