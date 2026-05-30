package com.oinsist.common.web.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.oinsist.common.crypto.annotation.Sensitive;
import com.oinsist.common.crypto.enums.SensitiveType;

import java.util.List;

/**
 * Jackson Bean 序列化修改器 - 脱敏处理
 * <p>
 * 工作原理：
 * Jackson 在首次序列化某个类时，会通过 SerializerFactory 构建 BeanSerializer。
 * BeanSerializerModifier 是 Jackson 提供的扩展点，允许在 Serializer 构建阶段
 * 修改各字段的序列化行为。
 * <p>
 * 本类的职责：
 * 遍历 Bean 的所有属性，检测字段上是否标注了 @Sensitive 注解，
 * 如果是 String 类型且标注了 @Sensitive，则替换其序列化器为 SensitiveSerializer。
 * <p>
 * 为什么选择 BeanSerializerModifier 而非 ContextualSerializer：
 * - BeanSerializerModifier 在 Serializer 构建阶段一次性处理，缓存后不再重复检测
 * - 性能更优，且实现更直观（直接操作 BeanPropertyWriter 列表）
 */
public class SensitiveSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                     BeanDescription beanDesc,
                                                     List<BeanPropertyWriter> beanProperties) {
        for (BeanPropertyWriter writer : beanProperties) {
            // 仅处理 String 类型字段（脱敏只对字符串有意义）
            if (writer.getType().getRawClass() != String.class) {
                continue;
            }
            // 检测字段上的 @Sensitive 注解（通过 Jackson 的 AnnotatedMember 获取原始 Java 注解）
            Sensitive sensitive = writer.getAnnotation(Sensitive.class);
            if (sensitive == null && writer.getMember() != null) {
                // 兼容：有些场景注解在 getter 上，尝试从 member 获取
                sensitive = writer.getMember().getAnnotation(Sensitive.class);
            }
            if (sensitive != null) {
                SensitiveType type = sensitive.type();
                // 替换为脱敏序列化器
                writer.assignSerializer(new SensitiveSerializer(type));
            }
        }
        return beanProperties;
    }
}
