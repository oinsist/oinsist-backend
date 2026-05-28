package com.oinsist.common.core.domain;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一接口响应对象。
 *
 * <p>Controller 不直接返回裸数据，而是统一包装成 {@code code + msg + data}，
 * 这样前端可以用一套稳定协议处理成功、失败、参数错误、登录失效等所有场景。
 * 这里暂时只保留最核心的响应结构，避免过早引入复杂的响应码枚举和国际化体系。</p>
 *
 * @param code 业务响应码
 * @param msg 响应消息
 * @param data 响应数据
 * @param <T> 数据类型
 */
public record R<T>(int code, String msg, T data) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 请求成功。
     */
    public static final int SUCCESS = 200;

    /**
     * 请求失败。
     */
    public static final int FAIL = 500;

    /**
     * 默认成功消息。
     */
    public static final String SUCCESS_MSG = "操作成功";

    /**
     * 默认失败消息。
     */
    public static final String FAIL_MSG = "操作失败";

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(SUCCESS, SUCCESS_MSG, data);
    }

    public static <T> R<T> fail() {
        return fail(FAIL_MSG);
    }

    public static <T> R<T> fail(String msg) {
        return new R<>(FAIL, msg, null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    /**
     * 判断当前响应是否为成功状态。
     *
     * <p>封装此方法避免调用方到处写 {@code r.code() == R.SUCCESS} 这样的魔法数字比较，
     * 同时如果未来成功判定逻辑发生变化（如增加状态码区间），只需修改此处即可。</p>
     */
    public boolean isSuccess() {
        return code == SUCCESS;
    }
}
