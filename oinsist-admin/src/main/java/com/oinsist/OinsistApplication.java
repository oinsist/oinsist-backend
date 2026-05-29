package com.oinsist;

import org.mybatis.spring.annotation.MapperScan;
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
 */
@SpringBootApplication
@MapperScan("com.oinsist.**.mapper")
public class OinsistApplication {

    public static void main(String[] args) {
        SpringApplication.run(OinsistApplication.class, args);
    }
}
