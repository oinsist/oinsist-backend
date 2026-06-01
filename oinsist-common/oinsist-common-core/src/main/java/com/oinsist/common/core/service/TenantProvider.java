package com.oinsist.common.core.service;

/**
 * 当前租户信息提供者（依赖反转接口）
 * <p>
 * 设计目的：
 * 基础设施模块（如 common-mybatis 的多租户拦截器）需要获取当前租户 ID 来对 SQL 进行拦截改写，
 * 但不应该直接依赖认证框架（如 Sa-Token）的具体实现。
 * 通过在 common-core（最底层无依赖模块）定义本接口，认证模块提供实现，
 * 持久层模块只依赖接口——实现"基础设施层"与"认证层"的完全解耦。
 * </p>
 * <p>
 * 实现约定：
 * - 实现类在用户未登录时（如系统启动初始化、定时任务、后台调度）应返回 null，不应抛出异常
 * - 拦截器侧需根据 null 值自行决定是否放行（超级管理员/免租户表）或抛出异常（越权访问）
 * - 实现类由 common-satoken 模块提供，运行时通过 Spring DI 注入
 * </p>
 */
public interface TenantProvider {

    /**
     * 获取当前租户ID
     *
     * @return 租户ID，未登录或系统后台任务场景下返回 null
     */
    Long getTenantId();
}
