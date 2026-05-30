package com.oinsist.common.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.oinsist.common.mybatis.datapermission.DataPermissionProvider;
import com.oinsist.common.mybatis.datapermission.OinsistDataPermissionHandler;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 数据权限拦截器必须在分页拦截器之前注册，原因：</p>
 * <ul>
 *     <li>数据权限拦截器先改写 SQL 的 WHERE 条件</li>
 *     <li>分页拦截器再基于改写后的 SQL 执行 COUNT 查询和 LIMIT 分页</li>
 * </ul>
 * <p>若顺序反过来，分页的 COUNT 将基于未过滤的全量数据计算，导致总数与实际列表不一致。</p>
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
     * 数据权限提供者（可选依赖）。
     * <p>
     * 使用 required = false 注入，实现优雅降级：
     * - 当业务模块提供了 DataPermissionProvider 实现时，自动启用数据权限过滤
     * - 当没有任何模块实现该接口时，跳过数据权限拦截器注册，不影响系统正常运行
     * </p>
     */
    @Autowired(required = false)
    private DataPermissionProvider dataPermissionProvider;

    /**
     * 注册 MyBatis-Plus 核心拦截器。
     *
     * <p>{@link MybatisPlusInterceptor} 是 MP 3.4+ 引入的"插件主体"，
     * 它内部维护了一个 InnerInterceptor 链表，按添加顺序依次执行。</p>
     *
     * <p>当前注册顺序：DataPermissionInterceptor → PaginationInnerInterceptor</p>
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        /*
         * 1. 数据权限拦截器（必须先于分页插件注册）
         *
         * 工作原理 —— 拦截待执行的 SELECT 语句，根据当前用户的数据权限范围，
         * 在 WHERE 条件中动态追加部门/用户级别的过滤条件，实现行级数据隔离。
         *
         * 仅当 DataPermissionProvider 存在时才注册，保证未配置数据权限的项目不受影响。
         */
        if (dataPermissionProvider != null) {
            DataPermissionInterceptor dataPermissionInterceptor = new DataPermissionInterceptor();
            dataPermissionInterceptor.setDataPermissionHandler(new OinsistDataPermissionHandler(dataPermissionProvider));
            interceptor.addInnerInterceptor(dataPermissionInterceptor);
        }

        /*
         * 2. 分页插件（始终最后注册）
         *
         * 工作原理 —— 拦截执行的 SQL，在查询语句末尾自动改写为带 LIMIT/OFFSET 的分页语句，
         * 同时额外执行一条 COUNT 查询以获取总记录数。
         *
         * 指定 DbType.POSTGRE_SQL 是为了让插件生成符合 PostgreSQL 方言的分页语法，
         * 不同数据库的分页关键字有差异（如 MySQL 用 LIMIT，Oracle 用 ROWNUM），
         * 明确指定可以避免 MP 在运行时动态探测数据库类型带来的额外开销。
         */
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));

        return interceptor;
    }
}
