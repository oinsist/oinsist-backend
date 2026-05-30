package com.oinsist.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色信息展示 VO
 * <p>
 * 用于角色列表查询和详情返回，包含角色的核心标识信息和状态。
 * </p>
 */
@Data
public class SysRoleVo {

    /** 角色ID */
    private Long roleId;

    /** 角色名称 */
    private String roleName;

    /** 角色标识（权限编码） */
    private String roleKey;

    /** 状态（0=正常，1=停用） */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
