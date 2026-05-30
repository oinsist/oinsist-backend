package com.oinsist.common.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.oinsist.common.core.service.CurrentUserProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Autowired;
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
 * <p>
 * 解耦设计：通过注入 {@link CurrentUserProvider} 接口获取当前用户ID，
 * 而非直接依赖具体认证框架（如 Sa-Token 的 LoginHelper），
 * 使持久层基础能力与认证实现完全解耦。
 */
@Slf4j
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    /**
     * 当前用户提供者（可选注入）
     * <p>
     * 使用 @Autowired(required = false)：
     * 当项目未引入认证模块时（如纯单元测试），不会因为缺少实现而启动失败。
     * </p>
     */
    @Autowired(required = false)
    private CurrentUserProvider currentUserProvider;

    /**
     * 插入时自动填充
     * <p>
     * 使用 strictInsertFill（严格模式）：仅当实体属性值为 null 时才执行填充。
     * 严格模式更安全，避免意外覆盖业务层的定制赋值。
     *
     * @param metaObject 元数据对象，封装了当前待插入的实体信息
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);

        // 填充创建者/更新者
        Long userId = getCurrentUserId();
        if (userId != null) {
            this.strictInsertFill(metaObject, "createBy", () -> userId, Long.class);
            this.strictInsertFill(metaObject, "updateBy", () -> userId, Long.class);
        }
    }

    /**
     * 更新时自动填充
     *
     * @param metaObject 元数据对象，封装了当前待更新的实体信息
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);

        Long userId = getCurrentUserId();
        if (userId != null) {
            this.strictUpdateFill(metaObject, "updateBy", () -> userId, Long.class);
        }
    }

    /**
     * 安全获取当前登录用户ID
     *
     * @return 当前登录用户ID，未登录或未配置 Provider 时返回 null
     */
    private Long getCurrentUserId() {
        if (currentUserProvider == null) {
            return null;
        }
        return currentUserProvider.getUserId();
    }
}
