package com.oinsist.common.mybatis.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 所有业务实体的公共基类
 * <p>
 * 为什么放在 common-mybatis 而非 common-core：
 * 本类使用了 MyBatis-Plus 的 {@code @TableField}、{@code @TableLogic} 等 ORM 注解，
 * 若放置在 common-core 会引入 MyBatis-Plus 依赖，污染无技术依赖的核心公共层，
 * 违反"common-core 不得依赖具体技术栈"的模块职责边界。
 * <p>
 * 所有业务实体继承此类即可自动获得审计字段（创建/更新时间与操作人）和逻辑删除能力，
 * 配合 {@link com.oinsist.common.mybatis.handler.MybatisMetaObjectHandler} 实现字段自动填充。
 */
@Data
public abstract class BaseEntity {

    /**
     * 创建者用户ID
     * <p>
     * 预留字段，待 Sa-Token 认证模块接入后由 MetaObjectHandler 自动填充当前登录用户ID。
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     * <p>
     * 由 MetaObjectHandler 在 INSERT 时自动填充为当前时间，业务层无需手动赋值。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者用户ID
     * <p>
     * 预留字段，待 Sa-Token 认证模块接入后由 MetaObjectHandler 自动填充当前登录用户ID。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 更新时间
     * <p>
     * 由 MetaObjectHandler 在 INSERT 和 UPDATE 时自动填充为当前时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标识（0=未删除，1=已删除）
     * <p>
     * 配合 MyBatis-Plus 全局逻辑删除配置，查询时自动追加 WHERE deleted = 0 条件，
     * 删除操作转化为 UPDATE SET deleted = 1，实现数据软删除。
     */
    @TableLogic
    private Integer deleted;
}
