package com.oinsist.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体
 * <p>
 * 记录每次登录尝试的结果，包括成功和失败。
 * 不继承 BaseEntity，日志本身即审计记录。
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog {

    /** 日志主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long loginId;

    /** 用户ID（登录成功时记录） */
    private Long userId;

    /** 登录账号 */
    private String username;

    /** 登录状态（0=成功 1=失败） */
    private Integer status;

    /** 登录IP */
    private String ip;

    /** 浏览器User-Agent */
    private String userAgent;

    /** 提示消息 */
    private String msg;

    /** 登录时间 */
    private LocalDateTime loginTime;

    /** 逻辑删除标识 */
    @TableLogic
    private Integer deleted;
}
