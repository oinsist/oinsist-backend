package com.oinsist.common.web.handler;

import com.oinsist.common.core.domain.R;
import com.oinsist.common.core.enums.ResultCode;
import com.oinsist.common.core.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器。
 *
 * <p><b>为什么需要全局异常处理器？</b></p>
 * <ul>
 *   <li>避免每个 Controller 重复编写 try-catch，将异常处理逻辑集中到一处，遵循 DRY 原则；</li>
 *   <li>统一错误响应格式为 {@code R<Void>}，前端只需一套解析逻辑即可处理所有异常场景；</li>
 *   <li>隐藏内部实现细节（如堆栈信息、SQL 异常），防止敏感信息泄露给客户端。</li>
 * </ul>
 *
 * <p><b>{@code @RestControllerAdvice} 的工作原理</b></p>
 * <ul>
 *   <li>它是 Spring MVC 异常解析机制的组成部分（而非代理式 Spring AOP）。当 Controller 方法抛出未捕获异常时，
 *       {@code DispatcherServlet} 会委托 {@code HandlerExceptionResolver} 链进行处理，
 *       其中 {@code ExceptionHandlerExceptionResolver} 负责查找匹配的 {@code @ExceptionHandler} 方法并调用。</li>
 *   <li>返回值自动通过 {@code @ResponseBody} 序列化为 JSON，与 Controller 的正常返回走同一套消息转换器。</li>
 * </ul>
 *
 * <p><b>异常处理的优先级策略</b></p>
 * <ul>
 *   <li>Spring 会按异常类型的精确度进行匹配：越具体的异常类型越优先被处理。
 *       例如 {@code MethodArgumentNotValidException} 比 {@code Exception} 更精确，会被优先匹配。</li>
 *   <li>使用 {@code @Order(Ordered.HIGHEST_PRECEDENCE + 10)} 确保本处理器优先级略低于
 *       Sa-Token 认证异常处理器（{@code SaTokenExceptionHandler}，Order = HIGHEST_PRECEDENCE）。
 *       这样当 Sa-Token 抛出的 NotLoginException 等异常传入时，Sa-Token 处理器会优先匹配，
 *       避免被本处理器的 Exception 兜底 handler 错误截获而返回 500。</li>
 * </ul>
 *
 * @author oinsist
 */
@Slf4j
// 优先级低于 SaTokenExceptionHandler（HIGHEST_PRECEDENCE），确保认证异常由 Sa-Token 处理器优先捕获
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 参数校验异常 ====================

    /**
     * 处理 @RequestBody + @Valid 参数校验失败异常。
     *
     * <p>当请求体中的字段不满足 JSR-380（如 @NotBlank、@Size）约束时，
     * Spring MVC 会抛出 {@link MethodArgumentNotValidException}，
     * 本方法从中提取第一条校验失败的具体消息返回给前端，便于用户快速定位输入错误。</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        // 从 BindingResult 中取出第一条字段校验错误
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = (fieldError != null) ? fieldError.getDefaultMessage() : "参数校验失败";

        log.warn("[参数校验失败] URI: {}, 字段: {}, 原因: {}",
                request.getRequestURI(),
                fieldError != null ? fieldError.getField() : "unknown",
                message);

        return R.fail(ResultCode.BAD_REQUEST, message);
    }

    // ==================== 请求参数缺失异常 ====================

    /**
     * 处理必要请求参数缺失异常。
     *
     * <p>当 Controller 方法参数标注了 {@code @RequestParam}（默认 required=true），
     * 但客户端请求中完全没有传递该参数时，Spring MVC 在参数绑定阶段就会抛出
     * {@link MissingServletRequestParameterException}。</p>
     *
     * <p>注意与 {@code ConstraintViolationException} 的区别：
     * 本异常处理"参数压根没传"的情况（绑定阶段），
     * 而 ConstraintViolationException 处理"参数传了但值不满足约束"的情况（校验阶段）。
     * 两者分别对应请求处理管线的不同阶段，需要各自独立处理。</p>
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingServletRequestParameter(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("[参数缺失] URI: {}, 参数名: {}, 类型: {}",
                request.getRequestURI(), e.getParameterName(), e.getParameterType());

        return R.fail(ResultCode.BAD_REQUEST, "缺少必要参数: " + e.getParameterName());
    }

    // ==================== 单参数约束校验异常 ====================

    /**
     * 处理 @RequestParam / @PathVariable 等单参数约束校验失败异常。
     *
     * <p>当 Controller 类上标注 {@code @Validated}，并在方法参数上直接使用
     * {@code @NotBlank}、{@code @Min} 等约束注解时，校验失败会抛出
     * {@link ConstraintViolationException}（注意不是 MethodArgumentNotValidException）。
     * 这是因为此时不经过 DataBinder 绑定流程，而是直接由 MethodValidationPostProcessor 触发校验。</p>
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        // 提取第一条约束违规的消息
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("参数校验失败");

        log.warn("[约束校验失败] URI: {}, message: {}", request.getRequestURI(), message);

        return R.fail(ResultCode.BAD_REQUEST, message);
    }

    // ==================== Spring 6.1+ 方法级校验异常 ====================

    /**
     * 处理 Spring Framework 6.1+ 新增的方法级参数校验异常。
     *
     * <p>Spring Boot 3.2 起，Spring MVC 引入了新的方法级校验路径：
     * 当 Controller 方法参数标注了约束注解时，参数解析器可能直接执行校验
     * （绕过传统的 AOP 代理 + MethodValidationPostProcessor 路径），
     * 校验失败时抛出 {@link HandlerMethodValidationException} 而非 {@code ConstraintViolationException}。</p>
     *
     * <p>补充此 handler 是为了兼容 Spring 版本演进——即使未来 Spring 默认走新路径，
     * 校验失败仍能被本处理器正确捕获并返回 400 响应，避免落到兜底的 500。</p>
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public R<Void> handleHandlerMethodValidation(HandlerMethodValidationException e, HttpServletRequest request) {
        // 从所有参数校验结果中提取第一条错误消息
        String message = e.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("参数校验失败");

        log.warn("[方法级校验失败] URI: {}, message: {}", request.getRequestURI(), message);

        return R.fail(ResultCode.BAD_REQUEST, message);
    }

    // ==================== 表单绑定校验异常 ====================

    /**
     * 处理表单提交 / Query 对象绑定校验失败异常。
     *
     * <p>当 Controller 方法参数是一个 POJO（通过 {@code @ModelAttribute} 或无注解自动绑定 query string），
     * 且标注了 {@code @Valid}，绑定过程中校验失败会抛出 {@link BindException}。</p>
     *
     * <p>注意：{@code MethodArgumentNotValidException extends BindException}，
     * 但 Spring 的异常匹配是精确优先，所以 @RequestBody 的校验失败会被上面的 handler 先捕获，
     * 这里只会兜住纯表单/Query 对象绑定的校验失败场景。</p>
     */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e, HttpServletRequest request) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = (fieldError != null) ? fieldError.getDefaultMessage() : "参数绑定校验失败";

        log.warn("[绑定校验失败] URI: {}, 字段: {}, 原因: {}",
                request.getRequestURI(),
                fieldError != null ? fieldError.getField() : "unknown",
                message);

        return R.fail(ResultCode.BAD_REQUEST, message);
    }

    // ==================== 请求体不可读异常 ====================

    /**
     * 处理请求体 JSON 解析失败异常。
     *
     * <p>当客户端发送的请求体不是合法 JSON（如缺少引号、多余逗号、编码错误），
     * 或 JSON 结构与目标类型不兼容时，Jackson 反序列化失败，
     * Spring MVC 会抛出 {@link HttpMessageNotReadableException}。</p>
     *
     * <p>这是客户端的格式错误，应返回 400 而非落到兜底的 500。</p>
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("[请求体解析失败] URI: {}, message: {}", request.getRequestURI(), e.getMessage());

        return R.fail(ResultCode.BAD_REQUEST, "请求体格式错误，请检查 JSON 格式");
    }

    // ==================== 参数类型转换异常 ====================

    /**
     * 处理请求参数类型不匹配异常。
     *
     * <p>当 URL 参数或路径变量的值无法转换为 Controller 方法声明的参数类型时
     * （如 {@code /config?key=abc} 对应参数类型为 Integer），
     * Spring MVC 会抛出 {@link MethodArgumentTypeMismatchException}。</p>
     *
     * <p>这是客户端传参错误，应返回 400。</p>
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = String.format("参数 '%s' 类型错误，期望类型: %s",
                e.getName(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");

        log.warn("[参数类型错误] URI: {}, {}", request.getRequestURI(), message);

        return R.fail(ResultCode.BAD_REQUEST, message);
    }

    // ==================== 请求方法不支持异常 ====================

    /**
     * 处理 HTTP 请求方法不支持异常。
     *
     * <p>当客户端使用了 Controller 未定义的 HTTP 方法访问端点时
     * （如对只支持 GET 的接口发送 POST 请求），
     * Spring MVC 会抛出 {@link HttpRequestMethodNotSupportedException}。</p>
     *
     * <p>这是客户端调用方式错误，返回 405 语义码，使前端能准确区分“参数错误”和“方法不支持”两种情况。</p>
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("[请求方法不支持] URI: {}, method: {}, 支持: {}",
                request.getRequestURI(), e.getMethod(), e.getSupportedHttpMethods());

        return R.fail(ResultCode.METHOD_NOT_ALLOWED, "不支持的请求方法: " + e.getMethod());
    }

    // ==================== 业务异常 ====================

    /**
     * 处理 Service 层主动抛出的业务异常。
     *
     * <p>{@link ServiceException} 是预期内的业务逻辑失败（如"用户名已存在"、"余额不足"），
     * 属于正常控制流分支，因此只记录 warn 日志且不打印堆栈——堆栈对排查业务规则无帮助，
     * 反而会在高并发场景下产生大量无意义的日志噪音。</p>
     */
    @ExceptionHandler(ServiceException.class)
    public R<Void> handleServiceException(ServiceException e, HttpServletRequest request) {
        log.warn("[业务异常] URI: {}, code: {}, message: {}",
                request.getRequestURI(), e.getCode(), e.getMessage());

        return R.fail(e.getCode(), e.getMessage());
    }

    // ==================== 未知异常（兜底） ====================

    /**
     * 兜底处理所有未被上层精确匹配的异常。
     *
     * <p>这类异常通常是非预期的系统级错误（如 NPE、数据库连接超时、第三方服务不可用），
     * 对外统一返回"操作失败"的模糊提示以避免泄露内部实现；
     * 对内记录 error 级别日志并保留完整堆栈，便于开发人员快速定位根因。</p>
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("[系统异常] URI: {}, message: {}", request.getRequestURI(), e.getMessage(), e);

        return R.fail(ResultCode.FAIL);
    }
}
