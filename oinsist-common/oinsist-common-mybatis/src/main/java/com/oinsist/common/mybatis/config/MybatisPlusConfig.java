package com.oinsist.common.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件注册中心。
 *
 * <p>本类是整个项目持久层插件的集中配置点，所有 MyBatis-Plus 的 InnerInterceptor
 * （分页、租户、乐观锁、数据权限等）都应在此统一注册，确保插件执行顺序可控。</p>
 *
 * <h3>为什么放在 oinsist-common-mybatis 而不是各业务模块？</h3>
 * <ul>
 *     <li>分页是所有业务模块共享的基础能力，属于"技术基础设施"而非"业务逻辑"，
 *         应当由公共持久层统一提供，业务模块只需关注 SQL 本身。</li>
 *     <li>数据库方言（DbType）是全局统一的配置，放在公共层可以避免各模块重复声明，
 *         也防止出现方言不一致的隐患。</li>
 *     <li>参考 RuoYi-Vue-Plus 的设计思路：将 MP 拦截器集中管理于 common-mybatis 模块，
 *         业务模块通过 Maven 依赖自动获得分页能力，做到"零配置即可用"。</li>
 * </ul>
 *
 * @author oinsist
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 核心拦截器。
     *
     * <p>{@link MybatisPlusInterceptor} 是 MP 3.4+ 引入的"插件主体"，
     * 它内部维护了一个 InnerInterceptor 链表，按添加顺序依次执行。
     * 后续如需添加租户、乐观锁等插件，只需在此方法中追加即可。</p>
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        /*
         * 分页插件（PaginationInnerInterceptor）：
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
