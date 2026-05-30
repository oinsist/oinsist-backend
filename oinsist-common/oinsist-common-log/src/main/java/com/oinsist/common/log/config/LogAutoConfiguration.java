package com.oinsist.common.log.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 操作日志模块自动配置
 * <p>
 * 通过 Spring Boot 3 的自动配置机制（META-INF/spring/...AutoConfiguration.imports），
 * 自动扫描并注册本模块下的 AOP 切面等组件。
 * <p>
 * 引入 oinsist-common-log 依赖的模块无需手动配置，即可获得操作日志能力。
 */
@AutoConfiguration
@ComponentScan("com.oinsist.common.log")
public class LogAutoConfiguration {
}
