package com.oinsist.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增角色请求 DTO
 * <p>
 * 角色是 RBAC 模型的核心枢纽，通过 roleKey 唯一标识进行权限编码匹配，
 * roleName 用于管理界面的友好展示。
 * </p>
 */
@Data
public class SysRoleAddDto {

    /** 角色名称（界面展示用） */
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    /** 角色标识（权限编码，如 admin、editor） */
    @NotBlank(message = "角色标识不能为空")
    private String roleKey;

    /** 状态（0=正常，1=停用） */
    private String status;
}
