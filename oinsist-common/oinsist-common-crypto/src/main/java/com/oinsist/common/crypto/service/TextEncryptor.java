package com.oinsist.common.crypto.service;

/**
 * 文本加解密服务接口
 * <p>
 * 定义统一的加解密契约，支持不同算法实现的替换（策略模式）。
 * 当前默认实现为 AES-256-GCM（AesGcmTextEncryptor）。
 * <p>
 * 设计思路：
 * - 面向接口编程，业务代码仅依赖此接口，不耦合具体算法
 * - 便于后续扩展（如切换为 RSA、SM4 等国密算法）
 * - TypeHandler、BodyAdvice 等消费方通过 Spring 注入此接口
 */
public interface TextEncryptor {
    /**
     * 加密明文
     *
     * @param plainText 待加密的明文，不能为 null
     * @return Base64 编码的密文（包含 IV）
     * @throws com.oinsist.common.core.exception.ServiceException 加密失败时抛出
     */
    String encrypt(String plainText);

    /**
     * 解密密文
     *
     * @param cipherText Base64 编码的密文（包含 IV）
     * @return 解密后的明文
     * @throws com.oinsist.common.core.exception.ServiceException 解密失败时抛出（如密文被篡改、密钥不匹配）
     */
    String decrypt(String cipherText);
}
