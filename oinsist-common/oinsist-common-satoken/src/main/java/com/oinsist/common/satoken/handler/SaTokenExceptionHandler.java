package com.oinsist.common.satoken.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.oinsist.common.core.domain.R;
import com.oinsist.common.core.enums.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 认证鉴权异常全局处理器
 *
 * <p><b>为什么要单独建立此处理器而非放入 GlobalExceptionHandler？</b></p>
 * <ul>
 *   <li>模块解耦：GlobalExceptionHandler 在 common-web 模块中，不应反向依赖 Sa-Token；</li>
 *   <li>职责单一：认证相关异常由认证模块自行处理，后续替换认证框架时只需修改此模块；</li>
 *   <li>Spring 的 {@code @RestControllerAdvice} 支持多个异常处理器共存，
 *       异常会被最精确匹配的 handler 捕获（NotLoginException 比 Exception 更具体，
 *       即使 GlobalExceptionHandler 有 Exception 兜底，Sa-Token 异常仍会被本处理器优先捕获）。</li>
 * </ul>
 *
 * <p><b>核心原则</b></p>
 * <p>不直接返回 Sa-Token 自带的 SaResult，统一走项目的 {@code R<T>} 响应体，
 * 确保前端只需适配一种响应格式。</p>
 *
 * <p><b>优先级说明</b></p>
 * <p>使用 {@code @Order(Ordered.HIGHEST_PRECEDENCE)} 确保本处理器在多 ControllerAdvice 场景下
 * 拥有最高优先级，优先于 {@code GlobalExceptionHandler}（Order = HIGHEST_PRECEDENCE + 10）。
 * 这样即使 GlobalExceptionHandler 中有 Exception 兜底 handler，
 * Sa-Token 的具体异常类型（如 NotLoginException）也会被本处理器优先捕获并返回 401/403，
 * 而不会落入兜底处理器返回 500。</p>
 *
 * @author oinsist
 */
@Slf4j
// 最高优先级，确保认证鉴权异常优先于 GlobalExceptionHandler（HIGHEST_PRECEDENCE + 10）被捕获
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SaTokenExceptionHandler {

    /**
     * 处理未登录异常
     *
     * <p>触发场景：</p>
     * <ul>
     *   <li>未携带 Token 访问受保护接口</li>
     *   <li>Token 已过期或被踢下线</li>
     *   <li>Token 无效（伪造或已注销）</li>
     * </ul>
     *
     * <p>Sa-Token 的 {@link NotLoginException} 内部携带了 {@code type} 字段，
     * 可细分为 NOT_TOKEN、INVALID_TOKEN、TOKEN_TIMEOUT、BE_REPLACED、KICK_OUT 等类型，
     * 当前统一返回 401 认证失败，后续可按需细化提示信息。</p>
     */
    @ExceptionHandler(NotLoginException.class)
    public R<Void> handleNotLoginException(NotLoginException e) {
        log.warn("认证失败 [type={}]：{}", e.getType(), e.getMessage());
        return R.fail(ResultCode.UNAUTHORIZED);
    }

    /**
     * 处理缺少权限异常
     *
     * <p>触发场景：用户已登录，但不具备访问当前接口所需的权限标识（如 system:user:add）。
     * Sa-Token 通过 {@code StpUtil.checkPermission("xxx")} 或 {@code @SaCheckPermission} 注解触发校验。</p>
     */
    @ExceptionHandler(NotPermissionException.class)
    public R<Void> handleNotPermissionException(NotPermissionException e) {
        log.warn("权限不足，缺少权限：{}", e.getPermission());
        return R.fail(ResultCode.FORBIDDEN);
    }

    /**
     * 处理缺少角色异常
     *
     * <p>触发场景：用户已登录，但不具备访问当前接口所需的角色（如 admin）。
     * Sa-Token 通过 {@code StpUtil.checkRole("xxx")} 或 {@code @SaCheckRole} 注解触发校验。</p>
     */
    @ExceptionHandler(NotRoleException.class)
    public R<Void> handleNotRoleException(NotRoleException e) {
        log.warn("权限不足，缺少角色：{}", e.getRole());
        return R.fail(ResultCode.FORBIDDEN);
    }
}
