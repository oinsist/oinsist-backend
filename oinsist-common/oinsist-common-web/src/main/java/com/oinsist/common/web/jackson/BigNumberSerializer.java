package com.oinsist.common.web.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 大整数 JSON 序列化器
 * <p>
 * 解决的问题：本项目主键统一采用雪花算法（{@code IdType.ASSIGN_ID}），生成的是 19 位 Long
 * （约 1.9×10^18）。而 JavaScript 的 Number 安全整数上限只有 2^53-1（约 9.007×10^15），
 * 若把雪花 ID 按裸数字下发，前端 {@code JSON.parse} 会静默丢失精度，
 * 造成「查询返回的 ID 回传后命中错误记录或查不到」这类极隐蔽的线上事故。
 * </p>
 * <p>
 * 为什么不直接用 {@code ToStringSerializer} 把所有数字都转字符串：
 * 那会让 {@code status}、{@code sort}、{@code count} 等小整数也变成字符串，
 * 逼迫前端到处做类型转换、破坏数值比较。这里只对「超出 JS 安全范围」的值转 String，
 * 安全范围内的数字仍按数字输出 —— 既根除精度风险，又把对前端的影响降到最小。
 * 这正是对 RuoYi-Vue-Plus {@code BigNumberSerializer} 设计思想的提炼。
 * </p>
 */
public class BigNumberSerializer extends JsonSerializer<Number> {

    /** JS Number.MAX_SAFE_INTEGER = 2^53 - 1 */
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;

    /** JS Number.MIN_SAFE_INTEGER = -(2^53 - 1) */
    private static final long MIN_SAFE_INTEGER = -9007199254740991L;

    /** 无状态、线程安全，复用单例即可，避免重复实例化 */
    public static final BigNumberSerializer INSTANCE = new BigNumberSerializer();

    @Override
    public void serialize(Number value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        long longValue = value.longValue();
        // 仅当超出 JS 安全整数范围时才转字符串规避精度丢失；范围内仍输出数字，保持前端使用习惯
        if (longValue > MAX_SAFE_INTEGER || longValue < MIN_SAFE_INTEGER) {
            gen.writeString(value.toString());
        } else {
            gen.writeNumber(longValue);
        }
    }
}
