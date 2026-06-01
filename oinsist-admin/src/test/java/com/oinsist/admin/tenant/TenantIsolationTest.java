package com.oinsist.admin.tenant;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.oinsist.common.core.service.TenantProvider;
import com.oinsist.common.mybatis.handler.OinsistTenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多租户隔离最小验证测试
 * 
 * 验证目标：
 * 1. TenantLineHandler 能正确返回租户 ID
 * 2. 全局共享表被正确跳过
 * 3. 未登录时 fail-closed（返回 0，不匹配任何真实租户）
 */
class TenantIsolationTest {

    @Test
    @DisplayName("租户处理器：正常获取租户ID")
    void testGetTenantId_withValidTenant() {
        // 模拟已登录用户，tenantId = 100
        TenantProvider mockProvider = () -> 100L;
        OinsistTenantLineHandler handler = new OinsistTenantLineHandler(mockProvider);
        
        Expression expression = handler.getTenantId();
        assertInstanceOf(LongValue.class, expression);
        assertEquals(100L, ((LongValue) expression).getValue());
    }

    @Test
    @DisplayName("租户处理器：未登录时 fail-closed 返回 0")
    void testGetTenantId_whenNotLoggedIn() {
        // 模拟未登录场景
        TenantProvider mockProvider = () -> null;
        OinsistTenantLineHandler handler = new OinsistTenantLineHandler(mockProvider);
        
        Expression expression = handler.getTenantId();
        assertInstanceOf(LongValue.class, expression);
        assertEquals(0L, ((LongValue) expression).getValue(),
            "未登录时应返回 0，确保不匹配任何真实租户数据（fail-closed）");
    }

    @Test
    @DisplayName("租户处理器：全局共享表被跳过")
    void testIgnoreTable_globalTables() {
        TenantProvider mockProvider = () -> 1L;
        OinsistTenantLineHandler handler = new OinsistTenantLineHandler(mockProvider);
        
        // 全局共享表应被忽略
        assertTrue(handler.ignoreTable("sys_menu"), "sys_menu 应为全局共享表");
        assertTrue(handler.ignoreTable("sys_role_menu"), "sys_role_menu 应为全局共享表");
        assertTrue(handler.ignoreTable("sys_tenant"), "sys_tenant 应为全局共享表");
        
        // 租户隔离表不应被忽略
        assertFalse(handler.ignoreTable("sys_user"), "sys_user 应为租户隔离表");
        assertFalse(handler.ignoreTable("sys_role"), "sys_role 应为租户隔离表");
        assertFalse(handler.ignoreTable("sys_dept"), "sys_dept 应为租户隔离表");
        assertFalse(handler.ignoreTable("sys_config"), "sys_config 应为租户隔离表");
        assertFalse(handler.ignoreTable("sys_oper_log"), "sys_oper_log 应为租户隔离表");
    }

    @Test
    @DisplayName("租户处理器：租户字段名为 tenant_id")
    void testGetTenantIdColumn() {
        TenantProvider mockProvider = () -> 1L;
        OinsistTenantLineHandler handler = new OinsistTenantLineHandler(mockProvider);
        
        assertEquals("tenant_id", handler.getTenantIdColumn());
    }

    @Test
    @DisplayName("登录专用查询：@InterceptorIgnore 跳过租户拦截器验证")
    void testLoginQueryBypassesTenantInterceptor() {
        // 验证设计意图：登录方法使用 @InterceptorIgnore 注解
        // 确保 SysUserMapper.selectByUsernameAndTenantId 方法上存在正确的注解
        try {
            var method = Class.forName("com.oinsist.system.mapper.SysUserMapper")
                .getMethod("selectByUsernameAndTenantId", String.class, Long.class);

            var annotation = method.getAnnotation(InterceptorIgnore.class);

            assertNotNull(annotation, "登录专用方法必须有 @InterceptorIgnore 注解");
            assertEquals("1", annotation.tenantLine(),
                "@InterceptorIgnore(tenantLine = \"1\") 必须跳过租户拦截器");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            fail("SysUserMapper.selectByUsernameAndTenantId 方法不存在: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("LoginBody 默认租户策略：tenantId 默认为 1")
    void testLoginBodyDefaultTenantId() throws Exception {
        var loginBodyClass = Class.forName("com.oinsist.system.domain.LoginBody");
        var instance = loginBodyClass.getDeclaredConstructor().newInstance();

        var getTenantId = loginBodyClass.getMethod("getTenantId");
        Object tenantId = getTenantId.invoke(instance);

        assertEquals(1L, tenantId, "LoginBody 的 tenantId 默认值应为 1（默认租户）");
    }

    @Test
    @DisplayName("租户处理器：ignoreTable 大小写不敏感验证")
    void testIgnoreTable_caseInsensitive() {
        TenantProvider mockProvider = () -> 1L;
        OinsistTenantLineHandler handler = new OinsistTenantLineHandler(mockProvider);

        // 验证大小写不敏感（MyBatis-Plus 传入的表名可能有大小写差异）
        assertTrue(handler.ignoreTable("SYS_MENU"), "大写表名应被忽略");
        assertTrue(handler.ignoreTable("Sys_Tenant"), "混合大小写应被忽略");
    }

    @Test
    @DisplayName("租户隔离表完整性：所有应隔离的表都不在白名单中")
    void testTenantIsolatedTables_notIgnored() {
        TenantProvider mockProvider = () -> 1L;
        OinsistTenantLineHandler handler = new OinsistTenantLineHandler(mockProvider);

        // 所有租户隔离表必须被拦截器改写
        String[] isolatedTables = {
            "sys_user", "sys_role", "sys_dept", "sys_config",
            "sys_user_role", "sys_role_dept",
            "sys_oper_log", "sys_login_log"
        };

        for (String table : isolatedTables) {
            assertFalse(handler.ignoreTable(table),
                table + " 应为租户隔离表，不应在白名单中");
        }
    }

    // ==================== ThreadLocal 租户上下文验证 ====================

    @Test
    @DisplayName("TenantContextHolder：ThreadLocal 基本功能验证")
    void testTenantContextHolder_basicFlow() throws Exception {
        var holderClass = Class.forName("com.oinsist.common.satoken.helper.TenantContextHolder");
        var setMethod = holderClass.getMethod("set", Long.class);
        var getMethod = holderClass.getMethod("get");
        var clearMethod = holderClass.getMethod("clear");

        // 初始状态为 null
        assertNull(getMethod.invoke(null), "初始时 ThreadLocal 应为 null");

        // 设置后可获取
        setMethod.invoke(null, 100L);
        assertEquals(100L, getMethod.invoke(null), "设置后应能获取到 tenantId");

        // 清除后恢复 null
        clearMethod.invoke(null);
        assertNull(getMethod.invoke(null), "清除后应恢复为 null");
    }

    @Test
    @DisplayName("TenantContextHolder：线程隔离验证")
    void testTenantContextHolder_threadIsolation() throws Exception {
        var holderClass = Class.forName("com.oinsist.common.satoken.helper.TenantContextHolder");
        var setMethod = holderClass.getMethod("set", Long.class);
        var getMethod = holderClass.getMethod("get");
        var clearMethod = holderClass.getMethod("clear");

        // 主线程设置 tenantId = 1
        setMethod.invoke(null, 1L);

        // 子线程获取应为 null（线程隔离）
        var result = new java.util.concurrent.atomic.AtomicReference<Object>();
        Thread t = new Thread(() -> {
            try {
                result.set(getMethod.invoke(null));
            } catch (Exception e) {
                result.set("ERROR");
            }
        });
        t.start();
        t.join();

        assertNull(result.get(), "子线程不应看到主线程的 tenantId（线程隔离）");

        // 清理
        clearMethod.invoke(null);
    }

    @Test
    @DisplayName("SaTokenTenantProvider：ThreadLocal 优先于 Session")
    void testSaTokenTenantProvider_threadLocalPriority() throws Exception {
        var holderClass = Class.forName("com.oinsist.common.satoken.helper.TenantContextHolder");
        var setMethod = holderClass.getMethod("set", Long.class);
        var clearMethod = holderClass.getMethod("clear");

        // 当 ThreadLocal 设置了值时，TenantProvider 应返回 ThreadLocal 的值
        // （即使 Session 未建立）
        setMethod.invoke(null, 99L);
        try {
            var providerClass = Class.forName("com.oinsist.common.satoken.service.SaTokenTenantProvider");
            var provider = providerClass.getDeclaredConstructor().newInstance();
            var getTenantIdMethod = providerClass.getMethod("getTenantId");

            Object tenantId = getTenantIdMethod.invoke(provider);
            assertEquals(99L, tenantId, "ThreadLocal 设置时应优先返回 ThreadLocal 中的值");
        } finally {
            clearMethod.invoke(null);
        }
    }

    @Test
    @DisplayName("SaTokenTenantProvider：ThreadLocal 为空时回退到 Session（返回 null 未登录）")
    void testSaTokenTenantProvider_fallbackToSession_notLoggedIn() throws Exception {
        var holderClass = Class.forName("com.oinsist.common.satoken.helper.TenantContextHolder");
        var getMethod = holderClass.getMethod("get");

        // 确保 ThreadLocal 为空
        assertNull(getMethod.invoke(null));

        // 未登录状态下（无 Session），应返回 null
        var providerClass = Class.forName("com.oinsist.common.satoken.service.SaTokenTenantProvider");
        var provider = providerClass.getDeclaredConstructor().newInstance();
        var getTenantIdMethod = providerClass.getMethod("getTenantId");

        Object tenantId = getTenantIdMethod.invoke(provider);
        assertNull(tenantId, "ThreadLocal 为空且未登录时应返回 null");
    }
}
