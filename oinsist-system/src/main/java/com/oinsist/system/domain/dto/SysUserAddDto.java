package com.oinsist.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增用户请求 DTO
 * <p>
 * 仅包含创建用户时必要的字段，通过 jakarta.validation 注解在 Controller 层自动完成参数校验，
 * 避免校验逻辑散落在 Service 层中。
 * </p>
 */
@Data
public class SysUserAddDto {

    /** 用户账号（登录名） */
    @NotBlank(message = "用户账号不能为空")
    private String username;

    /** 用户昵称（显示名） */
    @NotBlank(message = "用户昵称不能为空")
    private String nickname;

    /** 用户密码（明文传入，Service 层负责加密存储） */
    @NotBlank(message = "用户密码不能为空")
    private String password;

    /** 状态（0=正常，1=停用） */
    private String status;
}
