package com.oinsist.common.crypto.service;

import com.oinsist.common.core.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 加解密实现
 * <p>
 * 为什么选择 AES-GCM：
 * 1. 认证加密（AEAD）：同时提供机密性和完整性保护，密文被篡改会解密失败
 * 2. 无需额外 HMAC：GCM 模式内置认证标签（Auth Tag），简化实现
 * 3. JDK 原生支持：Java 21 内置 AES-GCM，无需引入第三方加密库
 * 4. 业界标准：TLS 1.3 默认密码套件，安全性经过充分验证
 * <p>
 * 密文格式：Base64(IV[12字节] + 密文 + AuthTag[16字节])
 * - IV（初始化向量）：每次加密随机生成 12 字节，确保相同明文产生不同密文
 * - AuthTag：128 位认证标签，GCM 模式自动附加在密文尾部
 */
@Slf4j
public class AesGcmTextEncryptor implements TextEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    /** GCM 推荐 IV 长度：12 字节（96 位） */
    private static final int IV_LENGTH = 12;
    /** GCM 认证标签长度：128 位 */
    private static final int TAG_LENGTH = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom;

    /**
     * 构造器
     *
     * @param hexKey 十六进制编码的 256 位密钥（64 个 hex 字符）
     */
    public AesGcmTextEncryptor(String hexKey) {
        if (hexKey == null || hexKey.length() != 64) {
            throw new ServiceException("AES 密钥必须为 64 位十六进制字符串（256 位）");
        }
        byte[] keyBytes = hexToBytes(hexKey);
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        this.secureRandom = new SecureRandom();
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null) {
            throw new ServiceException("加密明文不能为 null");
        }
        try {
            // 1. 生成随机 IV（每次加密不同，确保语义安全性）
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            // 2. 初始化 GCM 加密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // 3. 执行加密（GCM 模式会自动在密文尾部附加 AuthTag）
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 4. 拼接 IV + 密文（解密时需要从头部提取 IV）
            byte[] combined = new byte[IV_LENGTH + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(cipherBytes, 0, combined, IV_LENGTH, cipherBytes.length);

            // 5. Base64 编码输出（便于存储和传输）
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES-GCM 加密失败", e);
            throw new ServiceException("数据加密失败");
        }
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            throw new ServiceException("解密密文不能为空");
        }
        try {
            // 1. Base64 解码
            byte[] combined = Base64.getDecoder().decode(cipherText);
            if (combined.length < IV_LENGTH) {
                throw new ServiceException("密文格式无效：长度不足");
            }

            // 2. 分离 IV 和实际密文
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherBytes = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherBytes, 0, cipherBytes.length);

            // 3. 初始化 GCM 解密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // 4. 执行解密（GCM 会自动验证 AuthTag，篡改的密文会抛异常）
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("AES-GCM 解密失败（可能密文被篡改或密钥不匹配）", e);
            throw new ServiceException("数据解密失败");
        }
    }

    /**
     * 十六进制字符串转字节数组
     */
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }
}
