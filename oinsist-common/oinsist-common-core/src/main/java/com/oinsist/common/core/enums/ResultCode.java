package com.oinsist.common.core.enums;

/**
 * 统一基础响应码枚举。
 *
 * <p>后台系统中，Controller、全局异常处理器、远程调用等多处都需要使用 响应码。
 * 如果各模块各自硬编码 {@code 401}、{@code 403} 等数字，时间一长就会出现：
 * 同一个语义用了不同数字、同一个数字表达了不同含义的混乱局面。</p>
 *
 * <p>集中到枚举后，所有消费方通过 {@code ResultCode.UNAUTHORIZED} 这样的语义化引用获取码值，
 * 既避免了魔法数字，也让 IDE 能够追踪引用、一键重构。</p>
 *
 * <p>注意：本枚举只收录 HTTP 语义级别的基础码，不放业务级别的码
 * （如"用户已存在"、"余额不足"等应由各业务模块自行定义）。
 * 这样 common-core 才能保持通用性，不被具体业务逻辑污染。</p>
 */
public enum ResultCode {

    /** 操作成功 */
    SUCCESS(200, "操作成功"),

    /** 操作失败（通用服务端错误） */
    FAIL(500, "操作失败"),

    /** 请求参数校验失败 */
    BAD_REQUEST(400, "请求参数错误"),

    /** 认证失败，Token 缺失或已过期 */
    UNAUTHORIZED(401, "认证失败，请重新登录"),

    /** 已认证但无操作权限 */
    FORBIDDEN(403, "没有操作权限"),

    /** 请求的资源不存在 */
    NOT_FOUND(404, "请求资源不存在"),

    /** 请求方法不支持（如 POST 到只支持 GET 的接口） */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /** 请求过于频繁（触发限流） */
    TOO_MANY_REQUESTS(429, "请求过于频繁");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 获取响应码数值，与 {@link com.oinsist.common.core.domain.R} 的 code 字段对齐。
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取响应码默认消息，可直接用于 {@link com.oinsist.common.core.domain.R} 的 msg 字段。
     */
    public String getMsg() {
        return msg;
    }
}
