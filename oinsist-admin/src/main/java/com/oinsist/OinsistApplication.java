package com.oinsist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OInsist 后台管理系统启动入口。
 *
 * <h3>为什么主启动类放在 {@code com.oinsist} 包下，而不是 {@code com.oinsist.admin}？</h3>
 * <p>
 * 这是 Spring Boot 多模块项目中最关键的包路径设计决策之一：
 * {@link SpringBootApplication} 注解内部组合了 {@code @ComponentScan}，
 * 默认扫描范围是<strong>主类所在包及其所有子包</strong>。
 * </p>
 * <p>
 * 如果主类放在 {@code com.oinsist.admin}，那么 Spring 只会扫描 {@code com.oinsist.admin.*}，
 * 导致 {@code com.oinsist.system.service.SysConfigService} 等平级包中的 Bean 无法被发现，
 * 运行时会抛出 {@code NoSuchBeanDefinitionException}。
 * </p>
 * <p>
 * 将主类提升到 {@code com.oinsist} 包下，Spring 就能自动递归扫描：
 * <ul>
 *     <li>{@code com.oinsist.admin.controller.*} → Controller 层</li>
 *     <li>{@code com.oinsist.system.service.*} → 业务 Service 层</li>
 *     <li>{@code com.oinsist.common.web.config.*} → Web 公共配置</li>
 * </ul>
 * 这是 RuoYi-Vue-Plus 等主流多模块项目的标准做法，无需额外配置 {@code scanBasePackages}。
 * </p>
 *
 * <h3>为什么排除了 DataSource 自动配置？</h3>
 * <p>
 * 当前阶段尚未引入数据库依赖（PostgreSQL + MyBatis-Plus），但 Spring Boot 检测到
 * classpath 中存在 DataSource 相关类时会尝试自动配置数据源。
 * 如果没有配置 spring.datasource.url，启动会直接报错。
 * 通过 {@code exclude = DataSourceAutoConfiguration.class} 可以在当前阶段安全跳过，
 * 等后续引入数据库专题时再移除此排除项。
 * </p>
 */
@SpringBootApplication
public class OinsistApplication {

    public static void main(String[] args) {
        SpringApplication.run(OinsistApplication.class, args);
    }
}
