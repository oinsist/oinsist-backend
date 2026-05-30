/**
 * 数据安全公共模块
 * <p>
 * 提供三大核心能力：
 * 1. 响应数据脱敏（@Sensitive + SensitiveUtils）
 * 2. 数据库字段加解密（@EncryptField + TextEncryptor）
 * 3. 接口请求/响应加密（@ApiEncrypt）
 * <p>
 * 设计原则：本模块仅包含注解定义、枚举、加解密服务接口与实现、脱敏工具类，
 * 不依赖 Jackson、MyBatis、Spring MVC 等具体技术栈，保持纯净的基础能力定位。
 */
package com.oinsist.common.crypto;
