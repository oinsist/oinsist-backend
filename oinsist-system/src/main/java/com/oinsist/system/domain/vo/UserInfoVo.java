package com.oinsist.system.domain.vo;

import lombok.Data;

import java.util.Set;

/**
 * 用户信息 VO（/auth/userInfo 接口返回）
 * <p>
 * 包含用户基础信息、角色标识集合、权限标识集合。
 * 前端据此判断按钮显示/隐藏、菜单权限等。
 * </p>
 */
@Data
public class UserInfoVo {

    /** 用户ID */
    private Long userId;

    /** 用户账号 */
    private String username;

    /** 用户昵称 */
    private String nickname;

    /** 角色标识集合 */
    private Set<String> roles;

    /** 权限标识集合 */
    private Set<String> permissions;
}
