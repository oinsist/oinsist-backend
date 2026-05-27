package com.oinsist.admin.controller;

import com.oinsist.system.service.SysConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页测试 Controller —— 验证多模块 Bean 注入与 HTTP 调用链路的连通性。
 *
 * <h3>这个 Controller 解决了什么问题？</h3>
 * <p>
 * 在多模块 Spring Boot 项目中，最容易出现的问题是"编译通过但运行时注入失败"。
 * 常见原因包括：
 * <ul>
 *     <li>启动类包路径不对，导致 {@code @ComponentScan} 范围覆盖不到业务模块</li>
 *     <li>Maven 依赖传递链路断裂，业务模块的 jar 未被打入 classpath</li>
 *     <li>Spring Boot 自动配置与手动配置冲突，导致 Bean 未注册</li>
 * </ul>
 * 本 Controller 通过注入 {@code com.oinsist.system.service.SysConfigService}（跨模块 Bean），
 * 并在 HTTP 接口中调用其方法，可以一次性验证上述三个环节全部正确。
 * </p>
 *
 * <h3>为什么 Controller 放在 admin 模块而不是 system 模块？</h3>
 * <p>
 * 这遵循了 RuoYi-Vue-Plus 的分层原则：
 * <ul>
 *     <li>system 模块只包含 Service/Mapper 层（纯业务逻辑，不感知 HTTP 协议）</li>
 *     <li>admin 模块包含 Controller 层（负责 HTTP 入参校验、响应封装、路由定义）</li>
 * </ul>
 * 这种分离使得同一套业务逻辑可以被不同的入口（REST API、RPC、定时任务）复用，
 * 而不需要修改 system 模块的任何代码。
 * </p>
 */
@RestController
public class IndexController {

    private final SysConfigService sysConfigService;

    /**
     * 通过构造器注入 SysConfigService。
     *
     * <p>
     * 使用构造器注入而非 {@code @Autowired} 字段注入，原因：
     * <ul>
     *     <li>构造器注入是 Spring 官方推荐的方式，能保证依赖不可变（final 修饰）</li>
     *     <li>便于单元测试时 Mock 依赖，无需反射注入</li>
     *     <li>如果依赖缺失，应用启动阶段即可 fail-fast，而非运行时才 NPE</li>
     * </ul>
     * </p>
     */
    public IndexController(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    /**
     * 首页接口 —— 用于快速验证应用是否启动成功。
     *
     * <p>调用示例：{@code GET http://localhost:8080/}</p>
     *
     * @return 简单的欢迎信息，证明 Spring MVC 路由正常工作
     */
    @GetMapping("/")
    public String index() {
        return "OInsist Backend is running!";
    }

    /**
     * 配置查询接口 —— 验证跨模块 Bean 调用是否成功。
     *
     * <p>
     * 调用示例：{@code GET http://localhost:8080/config?key=sys.user.initPassword}
     * </p>
     * <p>
     * 如果该接口能返回正确的配置值，说明以下链路全部打通：
     * <ol>
     *     <li>admin 模块成功依赖了 system 模块（Maven 依赖链路正确）</li>
     *     <li>Spring IoC 容器成功扫描并实例化了 SysConfigService（包路径正确）</li>
     *     <li>Controller 成功通过构造器注入获得了 Service 实例（DI 机制正常）</li>
     * </ol>
     * </p>
     *
     * @param key 要查询的配置项 Key
     * @return 配置项的值
     */
    @GetMapping("/config")
    public String getConfig(@RequestParam(defaultValue = "sys.index.title") String key) {
        return sysConfigService.getConfigValue(key);
    }
}
