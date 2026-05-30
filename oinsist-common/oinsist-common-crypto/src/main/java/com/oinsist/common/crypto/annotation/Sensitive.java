package com.oinsist.common.crypto.annotation;

import com.oinsist.common.crypto.enums.SensitiveType;
import java.lang.annotation.*;

/**
 * 字段脱敏注解
 * <p>
 * 标注在实体/VO 的 String 字段上，序列化输出时自动按指定规则进行数据脱敏。
 * <p>
 * 设计思路：注解本身不依赖 Jackson，保持 crypto 模块的纯净性。
 * Jackson 序列化适配逻辑放在 common-web 模块（SensitiveSerializer），
 * 通过 BeanSerializerModifier 动态识别本注解并替换序列化器。
 * 这样即使不使用 Jackson，也能通过 SensitiveUtils 手动调用脱敏。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sensitive {
    /**
     * 脱敏类型
     */
    SensitiveType type();
}
