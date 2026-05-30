package com.oinsist.common.web.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.oinsist.common.crypto.enums.SensitiveType;
import com.oinsist.common.crypto.support.SensitiveUtils;

import java.io.IOException;

/**
 * 敏感字段 JSON 序列化器
 * <p>
 * 为什么不使用 @JsonSerialize(using=...) 直接标注在字段上：
 * 1. 避免实体类耦合 Jackson 注解（实体可能在非 Web 场景使用）
 * 2. 通过 BeanSerializerModifier 动态检测 @Sensitive 并替换，实现无侵入
 * 3. 脱敏逻辑统一收敛到 SensitiveUtils，日志和接口共享同一规则
 */
public class SensitiveSerializer extends JsonSerializer<Object> {

    private final SensitiveType sensitiveType;

    public SensitiveSerializer(SensitiveType sensitiveType) {
        this.sensitiveType = sensitiveType;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value instanceof String str) {
            // 调用统一脱敏工具，确保与日志脱敏规则一致
            gen.writeString(SensitiveUtils.desensitize(str, sensitiveType));
        } else {
            // 非 String 类型原样输出（理论上不会走到此分支，因为 Modifier 已做类型过滤）
            gen.writeObject(value);
        }
    }
}
