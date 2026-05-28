package com.oinsist.admin.controller;

import com.oinsist.common.core.domain.R;
import com.oinsist.common.core.exception.ServiceException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 异常处理验证 Controller —— 专用于验证 GlobalExceptionHandler 各分支是否正常工作。
 *
 * <p><b>为什么需要这个 Controller？</b></p>
 * <ul>
 *   <li>全局异常处理器包含多种异常类型的处理分支（参数校验、业务异常、未知异常等），
 *       需要有对应的触发入口才能端到端验证每个分支是否正确收敛为统一的 R 响应格式。</li>
 *   <li>本 Controller 不承载任何真实业务逻辑，纯粹作为基础设施的功能验收入口。
 *       生产环境可通过 Profile 或权限控制对外屏蔽。</li>
 * </ul>
 *
 * <p><b>验证清单</b></p>
 * <ol>
 *   <li>{@code GET /test/success} — 正常响应，验证 R.ok() 格式</li>
 *   <li>{@code GET /test/service-error} — 业务异常，验证 ServiceException → R.fail(code, msg)</li>
 *   <li>{@code POST /test/validate-body} — @RequestBody 参数校验异常，验证 MethodArgumentNotValidException → 400</li>
 *   <li>{@code GET /test/validate-param} — @RequestParam 单参数校验异常，验证 ConstraintViolationException → 400</li>
 *   <li>{@code GET /test/unknown-error} — 未知异常（兜底），验证 Exception → R.fail(500)</li>
 * </ol>
 *
 * <p><b>环境隔离</b></p>
 * <p>
 * 本 Controller 仅在 {@code dev} Profile 激活时注册到 Spring 容器。
 * 生产环境不会加载这些测试接口，避免暴露内部异常触发入口。
 * 若需在本地验证，启动时通过 JVM 参数 {@code --spring.profiles.active=dev}
 * 或环境变量 {@code SPRING_PROFILES_ACTIVE=dev} 激活 dev Profile。
 * </p>
 *
 * @author oinsist
 */
@Profile("dev")
@Validated
@RestController
@RequestMapping("/test")
public class TestController {

    // ==================== 1. 正常响应 ====================

    /**
     * 正常响应接口 —— 验证统一响应格式 R.ok()
     *
     * <p>调用示例：{@code GET http://localhost:8080/test/success}</p>
     * <p>预期响应：{@code {"code":200, "msg":"操作成功", "data":"一切正常"}}</p>
     */
    @GetMapping("/success")
    public R<String> success() {
        return R.ok("一切正常");
    }

    // ==================== 2. 业务异常 ====================

    /**
     * 业务异常接口 —— 验证 ServiceException 被 GlobalExceptionHandler 正确捕获
     *
     * <p>调用示例：{@code GET http://localhost:8080/test/service-error}</p>
     * <p>预期响应：{@code {"code":500, "msg":"模拟业务异常：用户名已存在", "data":null}}</p>
     *
     * <p>也可传入自定义 code：{@code GET http://localhost:8080/test/service-error?code=403&msg=没有操作权限}</p>
     */
    @GetMapping("/service-error")
    public R<Void> serviceError(
            @RequestParam(defaultValue = "500") int code,
            @RequestParam(defaultValue = "模拟业务异常：用户名已存在") String msg) {
        throw new ServiceException(code, msg);
    }

    // ==================== 3. 参数校验异常（@RequestBody） ====================

    /**
     * 参数校验异常接口 —— 验证 @RequestBody + @Valid 触发 MethodArgumentNotValidException
     *
     * <p>调用示例：
     * {@code POST http://localhost:8080/test/validate-body}
     * {@code Content-Type: application/json}
     * {@code {"name": ""}}
     * </p>
     * <p>预期响应：{@code {"code":400, "msg":"名称不能为空", "data":null}}</p>
     */
    @PostMapping("/validate-body")
    public R<String> validateBody(@Valid @RequestBody TestRequest request) {
        return R.ok("校验通过，name = " + request.name());
    }

    // ==================== 3.5 单参数约束校验异常（@RequestParam） ====================

    /**
     * 单参数校验测试接口 —— 验证 @Validated + @NotBlank 对 @RequestParam 的约束校验。
     *
     * <p>
     * 调用示例：
     * <ul>
     *     <li>正常：{@code GET http://localhost:8080/test/validate-param?value=hello} → 200</li>
     *     <li>异常：{@code GET http://localhost:8080/test/validate-param?value=} → 400（ConstraintViolationException）</li>
     *     <li>异常：{@code GET http://localhost:8080/test/validate-param} → 400（MissingServletRequestParameterException）</li>
     * </ul>
     * </p>
     *
     * <p>该接口验证的完整链路：Controller @Validated → MethodValidationPostProcessor 触发校验 →
     * 校验失败抛出 ConstraintViolationException → GlobalExceptionHandler 捕获 → R.fail(400, msg)</p>
     */
    @GetMapping("/validate-param")
    public R<String> validateParam(@NotBlank(message = "value 不能为空") @RequestParam String value) {
        return R.ok("校验通过，value = " + value);
    }

    // ==================== 4. 未知异常（兜底） ====================

    /**
     * 未知异常接口 —— 验证未预期的 RuntimeException 被兜底捕获
     *
     * <p>调用示例：{@code GET http://localhost:8080/test/unknown-error}</p>
     * <p>预期响应：{@code {"code":500, "msg":"操作失败", "data":null}}</p>
     * <p>（同时后台日志会打印完整堆栈，但前端看不到内部细节）</p>
     */
    @GetMapping("/unknown-error")
    public R<Void> unknownError() {
        // 模拟一个非预期的系统级异常
        throw new RuntimeException("模拟未知异常：空指针或数据库连接超时等不可控情况");
    }

    // ==================== 内部 DTO ====================

    /**
     * 用于验证 @RequestBody 校验的请求体。
     *
     * <p>使用 Java 21 record，天然不可变且自动生成 getter/toString/equals/hashCode，
     * 比传统 POJO + Lombok 更简洁。</p>
     *
     * @param name 名称字段，不能为空且长度 2~20
     */
    public record TestRequest(
            @NotBlank(message = "名称不能为空")
            @Size(min = 2, max = 20, message = "名称长度必须在 2-20 之间")
            String name
    ) {}
}
