package com.oinsist.common.crypto.annotation;

import java.lang.annotation.*;

/**
 * 数据库字段加解密标记注解
 * <p>
 * 标注在实体的 String 字段上，表示该字段在入库时自动加密、查询时自动解密。
 * <p>
 * 设计说明：
 * - 本注解仅作为语义标记，实际加解密由 MyBatis TypeHandler（EncryptTypeHandler）执行
 * - 使用时必须同时配置 @TableField(typeHandler = EncryptTypeHandler.class)
 * - 实体类必须添加 @TableName(autoResultMap = true) 以启用查询结果自动映射
 * <p>
 * 重要限制：加密后的字段不支持 SQL 模糊查询（LIKE）、范围查询、排序操作，
 * 因为密文不保留明文的字典序。如需检索，应考虑额外存储哈希索引列。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EncryptField {
}
