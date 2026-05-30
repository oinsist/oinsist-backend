package com.oinsist.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息展示 VO
 * <p>
 * 用于列表查询和详情返回，刻意不暴露 password 字段，
 * 遵循"最小暴露原则"防止敏感信息泄漏到前端。
 * </p>
 */
@Data
public class SysUserVo {

    /** 用户ID */
    private Long userId;

    /** 用户账号 */
    private String username;

    /** 用户昵称 */
    private String nickname;

    /** 状态（0=正常，1=停用） */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
