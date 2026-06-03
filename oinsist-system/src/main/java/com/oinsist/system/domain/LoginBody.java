package com.oinsist.system.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数
 */
@Data
public class LoginBody {

    /**
     * 用户账号
     */
    @NotBlank(message = "用户账号不能为空")
    private String username;

    /**
     * 用户密码
     */
    @NotBlank(message = "用户密码不能为空")
    private String password;


    /**
     * 租户ID
     * <p>
     * 登录时标识用户所属租户，前端不传时默认使用 1（默认租户）。
     * 多租户场景下，前端需在登录页面选择租户后传入对应值。
     * </p>
     */
    private Long tenantId = 1L;
}
