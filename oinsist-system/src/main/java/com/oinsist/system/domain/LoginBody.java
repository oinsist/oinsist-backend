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
}
