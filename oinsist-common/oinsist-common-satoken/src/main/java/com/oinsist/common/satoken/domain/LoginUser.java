package com.oinsist.common.satoken.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 登录用户上下文信息
 * <p>
 * 登录成功后存入 Sa-Token Session，贯穿整个请求生命周期。
 * 只保留最小必要字段，避免 Session 过于膨胀。
 * 后续如需扩展（如角色列表、权限集合），在此类追加即可。
 * </p>
 * <p>
 * 实现 Serializable 是因为 Sa-Token 使用 Redis 持久化 Session，
 * Session 中存储的对象需要支持序列化/反序列化。
 * </p>
 */
@Data
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID（主键，雪花算法生成）
     */
    private Long userId;

    /**
     * 用户账号（登录名）
     */
    private String username;

    /**
     * 用户昵称（展示名）
     */
    private String nickname;

    /**
     * 部门 ID
     */
    private Long deptId;

    /**
     * 角色标识集合（如 ["admin"]）
     * <p>
     * 登录时一次性加载并存入 Session，避免每次鉴权都查库。
     * Sa-Token 的 StpInterface 实现也从此字段读取（通过 Session）。
     * </p>
     */
    private Set<String> roleKeys;

    /**
     * 权限标识集合（如 ["system:user:list", "system:user:add"]）
     * <p>
     * 与 roleKeys 相同的缓存策略。
     * admin 角色用户该集合固定为 ["*:*:*"]。
     * </p>
     */
    private Set<String> permissions;
}
