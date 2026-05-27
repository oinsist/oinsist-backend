package com.oinsist.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 系统配置 Service —— 用于验证跨模块 Bean 调用链路的连通性。
 *
 * <h3>为什么要在 system 模块创建这个 Service？</h3>
 * <p>
 * 在 Spring Boot 多模块项目中，一个非常常见的"坑"是：
 * <strong>启动模块（admin）无法扫描到业务模块（system）的 Bean</strong>，
 * 导致运行时报 {@code NoSuchBeanDefinitionException}。
 * </p>
 * <p>
 * 这通常是因为 Spring Boot 的 {@code @SpringBootApplication} 默认只扫描
 * <strong>主类所在包及其子包</strong>。如果 admin 的主类在 {@code com.oinsist.admin}，
 * 而 system 的 Service 在 {@code com.oinsist.system}，两者属于平级包，不在扫描范围内。
 * </p>
 *
 * <h3>解决方案（本项目的做法）</h3>
 * <p>
 * 将 admin 模块的主启动类放在 {@code com.oinsist} 这个更上层的包路径下，
 * 这样 Spring Boot 会自动递归扫描 {@code com.oinsist.*} 的所有子包，
 * 自然能发现 {@code com.oinsist.system.service.SysConfigService}。
 * 这是 RuoYi-Vue-Plus 等开源项目的标准做法——启动类在最外层包，业务模块按功能分子包。
 * </p>
 *
 * <h3>本类当前的验证职责</h3>
 * <p>
 * 目前只提供一个简单的 {@link #getConfigValue(String)} 方法，返回模拟的配置值。
 * 后续接入数据库后，这里会演化为从 sys_config 表中读取系统参数的正式实现。
 * 当前阶段的唯一目标：证明 admin 模块能成功注入并调用 system 模块中的 Spring Bean。
 * </p>
 */
@Service
public class SysConfigService {

    private static final Logger log = LoggerFactory.getLogger(SysConfigService.class);

    /**
     * 根据配置 Key 获取配置值（当前为模拟实现）。
     *
     * <p>
     * 该方法存在的意义：
     * <ul>
     *     <li>验证 admin 模块通过 Maven 依赖引入 system 模块后，Spring IoC 容器能正确实例化本类</li>
     *     <li>验证 admin 的 Controller 可以通过 {@code @Autowired} 注入本 Service 并发起调用</li>
     *     <li>后续真实场景会替换为 MyBatis-Plus 查询 sys_config 表</li>
     * </ul>
     * </p>
     *
     * @param configKey 配置参数的唯一标识（如 "sys.user.initPassword"）
     * @return 配置参数值（当前返回模拟硬编码值）
     */
    public String getConfigValue(String configKey) {
        log.info("【SysConfigService】收到配置查询请求, configKey={}", configKey);

        // 模拟配置数据：后续会接入 PostgreSQL + MyBatis-Plus，从 sys_config 表中查询
        // 此处硬编码只是为了验证跨模块调用链路的连通性
        return switch (configKey) {
            case "sys.user.initPassword" -> "123456";
            case "sys.index.title" -> "OInsist 管理系统";
            default -> "未找到配置项: " + configKey;
        };
    }
}
