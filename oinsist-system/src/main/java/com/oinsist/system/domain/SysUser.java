package com.oinsist.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oinsist.common.crypto.annotation.EncryptField;
import com.oinsist.common.crypto.annotation.Sensitive;
import com.oinsist.common.crypto.enums.SensitiveType;
import com.oinsist.common.mybatis.domain.BaseEntity;
import com.oinsist.common.mybatis.handler.EncryptTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_user", autoResultMap = true)
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

    /** 部门ID */
    private Long deptId;

    /**
     * 邮箱（加密存储 + 脱敏输出）
     * <p>
     * - @EncryptField：语义标记，表示该字段入库加密
     * - @Sensitive(EMAIL)：接口输出时自动脱敏为 t***@domain.com
     * - @TableField(typeHandler)：MyBatis-Plus 读写时自动加解密
     */
    @EncryptField
    @Sensitive(type = SensitiveType.EMAIL)
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String email;

    /**
     * 手机号码（加密存储 + 脱敏输出）
     * <p>
     * - 入库时通过 EncryptTypeHandler 加密为密文
     * - 查询时自动解密为明文（Service 层使用明文）
     * - 接口输出时通过 @Sensitive 脱敏为 138****5678
     */
    @EncryptField
    @Sensitive(type = SensitiveType.PHONE)
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String phonenumber;
}
