package com.oinsist.common.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动字段填充处理器
 * <p>
 * MetaObjectHandler 是 MyBatis-Plus 提供的元数据对象处理器接口，
 * 用于在执行 INSERT / UPDATE 操作时，自动填充公共字段（如创建时间、更新时间、操作人等），
 * 避免在每个业务 Service 中重复编写赋值代码，实现"一次配置，全局生效"。
 * <p>
 * 工作原理：MP 在执行 SQL 前会检查实体中带有 {@code @TableField(fill = ...)} 注解的字段，
 * 若满足填充策略条件，则回调本处理器的 insertFill / updateFill 方法进行自动赋值。
 */
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充
     * <p>
     * 使用 strictInsertFill（严格模式）：仅当实体属性值为 null 时才执行填充。
     * 与普通 insertFill 的区别：
     * - strictInsertFill：属性有值则跳过，不会覆盖业务层手动设置的值
     * - 非 strict 模式（setFieldValByName）：无论属性是否有值都会强制覆盖
     * 严格模式更安全，避免意外覆盖业务层的定制赋值。
     *
     * @param metaObject 元数据对象，封装了当前待插入的实体信息
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);

        // createBy / updateBy 为预留字段，当前阶段不做空值填充，避免误导读者以为已完成用户字段填充。
        // 待后续 Sa-Token 认证模块（P04）接入后，通过登录上下文（StpUtil.getLoginIdAsLong()）获取当前用户并填充。
    }

    /**
     * 更新时自动填充
     *
     * @param metaObject 元数据对象，封装了当前待更新的实体信息
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);

        // updateBy 为预留字段，当前阶段不做空值填充，避免误导读者以为已完成用户字段填充。
        // 待后续 Sa-Token 认证模块（P04）接入后，通过登录上下文获取当前用户并填充。
    }
}
