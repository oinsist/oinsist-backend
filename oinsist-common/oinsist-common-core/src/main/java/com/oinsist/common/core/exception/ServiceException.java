package com.oinsist.common.core.exception;

import com.oinsist.common.core.enums.ResultCode;

/**
 * 业务异常（Service 层主动抛出）
 *
 * <p><b>为什么继承 RuntimeException（非受检异常）？</b></p>
 * <ul>
 *   <li>业务规则校验失败（如"用户已存在"、"余额不足"）属于程序可预见的逻辑分支，
 *       不应强制调用方 try-catch 或在方法签名声明 throws，否则会污染整个调用链。</li>
 *   <li>非受检异常让 Service 层代码保持简洁：发现违规直接 throw，由框架统一兜底。</li>
 * </ul>
 *
 * <p><b>为什么需要 code 字段？</b></p>
 * <ul>
 *   <li>前端需要根据不同的错误码执行不同的 UI 策略（如 401 跳转登录页、403 提示无权限），
 *       仅靠 message 文本无法做程序化判断，code 提供了机器可读的语义标识。</li>
 * </ul>
 *
 * <p><b>为什么不自定义 message 字段？</b></p>
 * <ul>
 *   <li>{@code RuntimeException} 内部已维护了 {@code detailMessage} 字段，
 *       通过 {@code super(message)} 传入、{@code getMessage()} 获取，语义完全一致。
 *       自行再声明一个 message 字段属于冗余，还需要额外重写 getMessage() 保持一致性。
 *       精简后只保留 code 一个扩展字段，消息由父类统一管理。</li>
 * </ul>
 *
 * <p><b>与全局异常处理器的协作关系</b></p>
 * <ul>
 *   <li>本异常会被 {@code oinsist-common-web} 模块中 {@code @RestControllerAdvice} 标注的
 *       全局异常处理器捕获，处理器通过 {@code getCode()} 和 {@code getMessage()} 提取信息，
 *       封装为统一响应体 {@code R<Void>} 返回给前端。</li>
 * </ul>
 *
 * @author oinsist
 */
public class ServiceException extends RuntimeException {

    /**
     * 错误码，与 {@link ResultCode} 体系对齐，前端据此做差异化处理
     */
    private final int code;

    /**
     * 仅传递错误消息，错误码默认取 {@link ResultCode#FAIL} 的 code（500）
     *
     * @param message 业务失败原因描述
     */
    public ServiceException(String message) {
        super(message);
        this.code = ResultCode.FAIL.getCode();
    }

    /**
     * 自定义错误码 + 错误消息
     *
     * @param code    自定义错误码
     * @param message 业务失败原因描述
     */
    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 直接传入 {@link ResultCode} 枚举，code 和 message 均从枚举中获取
     *
     * @param resultCode 预定义的响应码枚举
     */
    public ServiceException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }

    /**
     * 传入 {@link ResultCode} 获取 code，但使用自定义消息覆盖枚举默认描述
     * <p>适用场景：错误类型明确（如 BAD_REQUEST），但需要更具体的业务上下文描述</p>
     *
     * @param resultCode 预定义的响应码枚举（仅取 code）
     * @param message    覆盖枚举默认描述的自定义消息
     */
    public ServiceException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
