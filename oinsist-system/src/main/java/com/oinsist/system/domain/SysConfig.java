package com.oinsist.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oinsist.common.mybatis.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置表实体。
 * <p>
 * 用于 P03 阶段验证 MyBatis-Plus 分页、自动填充、逻辑删除等基础能力。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    /** 配置ID（雪花算法生成） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long configId;

    /**
     * 租户 ID（多租户行级隔离标识）
     */
    private Long tenantId;

    /** 配置名称 */
    private String configName;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;
}
