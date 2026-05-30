package com.oinsist.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 编辑用户请求 DTO
 * <p>
 * 编辑时不允许修改用户名和密码：
 * - 用户名作为登录唯一标识，变更会影响审计追溯；
 * - 密码修改应走独立的"重置密码"流程，确保安全校验完备。
 * </p>
 */
@Data
public class SysUserEditDto {

    /** 用户ID（主键，必传） */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 用户昵称（显示名） */
    @NotBlank(message = "用户昵称不能为空")
    private String nickname;

    /** 状态（0=正常，1=停用） */
    private String status;
}
