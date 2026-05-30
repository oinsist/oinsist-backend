package com.oinsist.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oinsist.common.mybatis.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 用户ID（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long userId;

    /** 用户账号 */
    private String username;

    /** 用户昵称 */
    private String nickname;

    /** 密码（BCrypt加密存储） */
    private String password;

    /** 状态（0正常 1停用） */
    private String status;
}
