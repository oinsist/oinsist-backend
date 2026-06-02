package com.oinsist.common.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.oinsist.common.core.service.TenantProvider;
import com.oinsist.common.mybatis.datapermission.DataPermissionProvider;
import com.oinsist.common.mybatis.datapermission.OinsistDataPermissionHandler;
import com.oinsist.common.mybatis.handler.OinsistTenantLineHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件注册中心。
 *
 * <p>本类是整个项目持久层插件的集中配置点，所有 MyBatis-Plus 的 InnerInterceptor
 * （分页、租户、乐观锁、数据权限等）都应在此统一注册，确保插件执行顺序可控。</p>
 *
 * <h3>为什么使用拦截器统一处理数据权限：</h3>
 * <ol>
 *     <li>集中管控 —— 数据权限逻辑集中在拦截器中，避免散落到各个 Service 方法</li>
 *     <li>无侵入 —— 业务查询代码无需修改，只需在 Mapper 方法上标注 @DataPermission</li>
 *     <li>SQL 级过滤 —— 在数据库层面追加 WHERE 条件，比应用层内存过滤性能更优</li>
 *     <li>与分页协同 —— 数据权限改写在分页统计之前执行，确保 COUNT 与数据结果一致</li>
 * </ol>
 *
 * <h3>插件注册顺序说明（重要）：</h3>
 * <p>MyBatis-Plus 多插件场景下，InnerInterceptor 按添加顺序依次执行。
 * 正确顺序为：多租户 → 数据权限 → 分页，原因：</p>
 * <ul>
 *     <li>多租户拦截器最先执行，确保所有 SQL 都先加上 tenant_id 隔离条件</li>
 *     <li>数据权限拦截器在租户隔离基础上，进一步按用户角色/部门缩小可见范围</li>
 *     <li>分页拦截器最后执行，基于已被租户+权限双重过滤的结果集计算 COUNT</li>
 * </ul>
 * <p>若顺序错误（如分页先于租户），COUNT 将统计未隔离的全量数据，导致分页总数异常。</p>
 *
 * <h3>为什么放在 oinsist-common-mybatis 而不是各业务模块？</h3>
 * <ul>
 *     <li>分页和数据权限是所有业务模块共享的基础能力，属于"技术基础设施"而非"业务逻辑"，
 *         应当由公共持久层统一提供，业务模块只需关注 SQL 本身。</li>
 *     <li>数据库方言（DbType）是全局统一的配置，放在公共层可以避免各模块重复声明，
 *         也防止出现方言不一致的隐患。</li>
 *     <li>参考 RuoYi-Vue-Plus 的设计思路：将 MP 拦截器集中管理于 common-mybatis 模块，
 *         业务模块通过 Maven 依赖自动获得分页与数据权限能力，做到"零配置即可用"。</li>
 * </ul>
 *
 * @author oinsist
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 使用 ObjectProvider 延迟获取，避免循环依赖：
     * <p>
     * DataPermissionProvider / TenantProvider 的实现类依赖 Mapper，
     * 而 Mapper 又依赖 sqlSessionFactory，sqlSessionFactory 依赖本配置类创建的拦截器。
     * </p>
     * <p>
     * 延迟解析策略：
     * ObjectProvider 引用传递给 Handler，Handler 在 SQL 实际执行时才调用 getIfAvailable()。
     * 此时 sqlSessionFactory 早已创建完成，不会触发循环依赖。
     * 同时实现优雅降级：当没有模块实现 Provider 接口时，Handler 内部处理 null 情况。
     * </p>
     */
    private final ObjectProvider<TenantProvider> tenantProviderProvider;
    private final ObjectProvider<DataPermissionProvider> dataPermissionProviderProvider;

    public MybatisPlusConfig(
            ObjectProvider<TenantProvider> tenantProviderProvider,
            ObjectProvider<DataPermissionProvider> dataPermissionProviderProvider) {
        this.tenantProviderProvider = tenantProviderProvider;
        this.dataPermissionProviderProvider = dataPermissionProviderProvider;
    }

    /**
     * 注册 MyBatis-Plus 核心拦截器。
     *
     * <p>{@link MybatisPlusInterceptor} 是 MP 3.4+ 引入的"插件主体"，
     * 它内部维护了一个 InnerInterceptor 链表，按添加顺序依次执行。</p>
     *
     * <p>当前注册顺序：TenantLineInnerInterceptor → DataPermissionInterceptor → PaginationInnerInterceptor</p>
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        /*
         * ===== 拦截器注册顺序说明 =====
         * MyBatis-Plus 的 InnerInterceptor 按注册顺序依次执行 SQL 改写。
         * 正确顺序为：多租户 → 数据权限 → 分页
         *
         * 原因：
         * 1. 多租户拦截器最先执行，确保所有 SQL 都先加上 tenant_id 隔离条件，
         *    后续拦截器（数据权限、分页）都在已隔离的租户数据范围内工作。
         * 2. 数据权限拦截器在租户隔离基础上，进一步按用户角色/部门缩小可见范围。
         * 3. 分页拦截器最后执行，其 COUNT 查询基于已被租户+权限双重过滤的结果集，
         *    保证分页总数正确。
         *
         * 若顺序错误（如分页先于租户），COUNT 将统计未隔离的全量数据，导致分页总数异常。
         *
         * 延迟解析策略：
         * Handler 持有 ObjectProvider 而非直接持有 Provider 实例，
         * 在 SQL 实际执行时才通过 getIfAvailable() 获取 Provider。
         * 这样避免了 @Bean 创建阶段触发 Provider → Mapper → sqlSessionFactory 循环依赖。
         */

        // ===== 注册顺序 1: 多租户拦截器（无条件注册，Handler 内部处理 Provider 缺失） =====
        // 工作原理 —— 拦截所有 SQL，通过 OinsistTenantLineHandler 获取当前租户 ID，
        // 自动在 WHERE 条件追加 tenant_id = ? 实现租户级数据隔离。
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(new OinsistTenantLineHandler(tenantProviderProvider));
        interceptor.addInnerInterceptor(tenantInterceptor);

        // ===== 注册顺序 2: 数据权限拦截器（无条件注册） =====
        // 工作原理 —— 拦截待执行的 SELECT 语句，根据当前用户的数据权限范围，
        // 在 WHERE 条件中动态追加部门/用户级别的过滤条件，实现行级数据隔离。
        DataPermissionInterceptor dataPermissionInterceptor = new DataPermissionInterceptor();
        dataPermissionInterceptor.setDataPermissionHandler(
            new OinsistDataPermissionHandler(dataPermissionProviderProvider)
        );
        interceptor.addInnerInterceptor(dataPermissionInterceptor);

        // ===== 注册顺序 3: 分页插件（始终最后注册） =====
        // 工作原理 —— 拦截执行的 SQL，在查询语句末尾自动改写为带 LIMIT/OFFSET 的分页语句，
        // 同时额外执行一条 COUNT 查询以获取总记录数。
        // 指定 DbType.POSTGRE_SQL 是为了让插件生成符合 PostgreSQL 方言的分页语法，
        // 不同数据库的分页关键字有差异（如 MySQL 用 LIMIT，Oracle 用 ROWNUM），
        // 明确指定可以避免 MP 在运行时动态探测数据库类型带来的额外开销。
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));

        return interceptor;
    }
}
