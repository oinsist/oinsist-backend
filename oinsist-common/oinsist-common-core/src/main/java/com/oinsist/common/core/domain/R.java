package com.oinsist.common.core.domain;

import com.oinsist.common.core.enums.ResultCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一接口响应对象。
 *
 * <p>Controller 不直接返回裸数据，而是统一包装成 {@code code + msg + data}，
 * 这样前端可以用一套稳定协议处理成功、失败、参数错误、登录失效等所有场景。</p>
 *
 * <p>所有响应码的定义集中在 {@link ResultCode} 枚举中，本类通过静态工厂方法消费枚举，
 * 保证整个系统只有一份码值来源，避免各模块硬编码导致的不一致。</p>
 *
 * @param code 业务响应码
 * @param msg 响应消息
 * @param data 响应数据
 * @param <T> 数据类型
 */
public record R<T>(int code, String msg, T data) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ==================== 成功响应 ====================

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    // ==================== 失败响应 ====================

    public static <T> R<T> fail() {
        return fail(ResultCode.FAIL);
    }

    public static <T> R<T> fail(String msg) {
        return new R<>(ResultCode.FAIL.getCode(), msg, null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    // ==================== 枚举驱动 ====================

    /**
     * 基于 {@link ResultCode} 构建失败响应，使用枚举自带的默认消息。
     *
     * <p>全局异常处理器等场景可直接传入枚举：{@code R.fail(ResultCode.UNAUTHORIZED)}，
     * 无需手动拼接 code 和 msg，保持码值与消息的一致性。</p>
     */
    public static <T> R<T> fail(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMsg(), null);
    }

    /**
     * 基于 {@link ResultCode} 构建失败响应，允许覆盖默认消息。
     *
     * <p>当需要在保持标准码值的同时提供更具体的错误描述时使用，
     * 例如：{@code R.fail(ResultCode.BAD_REQUEST, "用户名不能为空")}。</p>
     */
    public static <T> R<T> fail(ResultCode resultCode, String msg) {
        return new R<>(resultCode.getCode(), msg, null);
    }

    // ==================== 状态判断 ====================

    /**
     * 判断当前响应是否为成功状态。
     *
     * <p>封装此方法避免调用方到处写 {@code r.code() == 200} 这样的魔法数字比较，
     * 同时如果未来成功判定逻辑发生变化（如增加状态码区间），只需修改此处即可。</p>
     */
    public boolean isSuccess() {
        return code == ResultCode.SUCCESS.getCode();
    }
}
