package com.oinsist.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 编辑角色请求 DTO
 * <p>
 * 编辑角色时必须携带 roleId 以定位目标记录，
 * 同时允许修改 roleName、roleKey 和 status。
 * </p>
 */
@Data
public class SysRoleEditDto {

    /** 角色ID（主键，必传） */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /** 角色名称（界面展示用） */
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    /** 角色标识（权限编码，如 admin、editor） */
    @NotBlank(message = "角色标识不能为空")
    private String roleKey;

    /** 状态（0=正常，1=停用） */
    private String status;
}
