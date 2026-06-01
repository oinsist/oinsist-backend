package com.oinsist.common.satoken.helper;

/**
 * 租户上下文持有者（线程级）
 * <p>
 * 解决的核心问题：
 * 登录流程中，Session 尚未建立时 SaTokenTenantProvider 无法从 Session 获取 tenantId，
 * 导致租户拦截器使用 fail-closed 值（0），使后续的角色/权限查询返回空结果。
 * </p>
 * <p>
 * 设计原理：
 * 使用 ThreadLocal 在当前线程中临时存储租户 ID，作为 Session 的"前置替代"。
 * SaTokenTenantProvider 优先从 ThreadLocal 读取，其次从 Session 读取。
 * 登录完成后必须清除 ThreadLocal，避免线程复用导致的租户泄漏。
 * </p>
 * <p>
 * 使用场景（严格限定）：
 * <ul>
 *     <li>登录认证流程：用户身份验证通过后、Session 建立前的角色/权限查询阶段</li>
 *     <li>系统后台任务：无 Session 但需要以特定租户身份执行数据库操作</li>
 * </ul>
 * </p>
 * <p>
 * 安全约束：
 * 必须在 try-finally 中使用，确保 clear() 一定被调用，防止线程池复用时租户泄漏。
 * </p>
 */
public final class TenantContextHolder {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    /**
     * 设置当前线程的租户 ID
     *
     * @param tenantId 租户 ID
     */
    public static void set(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * 获取当前线程的租户 ID
     *
     * @return 租户 ID，未设置时返回 null
     */
    public static Long get() {
        return TENANT_ID.get();
    }

    /**
     * 清除当前线程的租户 ID
     * <p>
     * 必须在 finally 块中调用，防止线程池场景下的租户泄漏
     * </p>
     */
    public static void clear() {
        TENANT_ID.remove();
    }
}
