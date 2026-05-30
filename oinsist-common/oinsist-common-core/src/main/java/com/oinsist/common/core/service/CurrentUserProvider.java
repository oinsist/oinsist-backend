package com.oinsist.common.core.service;

/**
 * 当前登录用户信息提供者（依赖反转接口）
 * <p>
 * 设计目的：
 * 基础设施模块（如 common-mybatis 的 MetaObjectHandler）需要获取当前操作人 ID 来自动填充审计字段，
 * 但不应该直接依赖认证框架（如 Sa-Token）的具体实现。
 * 通过在 common-core（最底层无依赖模块）定义本接口，认证模块提供实现，
 * 持久层模块只依赖接口——实现"基础设施层"与"认证层"的完全解耦。
 * </p>
 * <p>
 * 实现约定：
 * - 实现类在用户未登录时（如系统启动初始化、定时任务）应返回 null，不应抛出异常
 * - 实现类由 common-satoken 模块提供，运行时通过 Spring DI 注入
 * </p>
 */
public interface CurrentUserProvider {

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID，未登录时返回 null
     */
    Long getUserId();
}
