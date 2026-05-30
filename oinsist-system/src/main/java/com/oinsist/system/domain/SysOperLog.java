package com.oinsist.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 * <p>
 * 不继承 BaseEntity，因为日志表本身就是审计记录，
 * 不需要 createBy/updateBy 等审计字段，避免语义重复。
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog {

    /** 日志主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long operId;

    /** 模块标题 */
    private String title;

    /** 业务类型（0=其他 1=新增 2=修改 3=删除 4=导出 5=导入 6=授权 7=强退） */
    private Integer businessType;

    /** 方法名称 */
    private String method;

    /** 请求方式 */
    private String requestMethod;

    /** 请求URL */
    private String requestUrl;

    /** 请求参数 */
    private String requestParam;

    /** 操作状态（0=成功 1=异常） */
    private Integer status;

    /** 错误消息 */
    private String errorMsg;

    /** 操作人ID */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 操作IP */
    private String ip;

    /** 执行耗时（毫秒） */
    private Long duration;

    /** 操作时间 */
    private LocalDateTime operTime;

    /** 逻辑删除标识 */
    @TableLogic
    private Integer deleted;
}
