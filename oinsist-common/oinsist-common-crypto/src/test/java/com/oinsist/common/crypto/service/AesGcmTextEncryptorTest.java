package com.oinsist.common.crypto.service;

import com.oinsist.common.core.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AES-GCM 加解密服务单元测试
 */
class AesGcmTextEncryptorTest {

    /** 测试用 256 位密钥（64 hex 字符） */
    private static final String TEST_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private AesGcmTextEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new AesGcmTextEncryptor(TEST_KEY);
    }

    @Test
    @DisplayName("加密解密对称性：解密后等于原文")
    void encryptDecryptSymmetry() {
        String plainText = "hello@example.com";
        String cipherText = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(cipherText);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("加密解密对称性：中文内容")
    void encryptDecryptChinese() {
        String plainText = "张三的邮箱地址";
        String cipherText = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(cipherText);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("加密解密对称性：空字符串")
    void encryptDecryptEmpty() {
        String plainText = "";
        String cipherText = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(cipherText);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("语义安全性：相同明文每次加密产生不同密文（随机 IV）")
    void semanticSecurity() {
        String plainText = "13812345678";
        String cipher1 = encryptor.encrypt(plainText);
        String cipher2 = encryptor.encrypt(plainText);
        // 由于 IV 随机，两次加密结果必须不同
        assertNotEquals(cipher1, cipher2);
        // 但解密后都等于原文
        assertEquals(plainText, encryptor.decrypt(cipher1));
        assertEquals(plainText, encryptor.decrypt(cipher2));
    }

    @Test
    @DisplayName("篡改密文应抛出 ServiceException")
    void tamperDetection() {
        String cipherText = encryptor.encrypt("test");
        // 篡改密文的最后一个字符
        String tampered = cipherText.substring(0, cipherText.length() - 1) + "X";
        assertThrows(ServiceException.class, () -> encryptor.decrypt(tampered));
    }

    @Test
    @DisplayName("无效密文格式应抛出 ServiceException")
    void invalidCipherText() {
        assertThrows(ServiceException.class, () -> encryptor.decrypt("not-a-valid-base64-cipher!!!"));
    }

    @Test
    @DisplayName("null 明文加密应抛出 ServiceException")
    void encryptNull() {
        assertThrows(ServiceException.class, () -> encryptor.encrypt(null));
    }

    @Test
    @DisplayName("null 密文解密应抛出 ServiceException")
    void decryptNull() {
        assertThrows(ServiceException.class, () -> encryptor.decrypt(null));
    }

    @Test
    @DisplayName("空白密文解密应抛出 ServiceException")
    void decryptBlank() {
        assertThrows(ServiceException.class, () -> encryptor.decrypt("   "));
    }

    @Test
    @DisplayName("无效密钥长度应抛出 ServiceException")
    void invalidKeyLength() {
        assertThrows(ServiceException.class, () -> new AesGcmTextEncryptor("tooshort"));
    }

    @Test
    @DisplayName("null 密钥应抛出 ServiceException")
    void nullKey() {
        assertThrows(ServiceException.class, () -> new AesGcmTextEncryptor(null));
    }

    @Test
    @DisplayName("不同密钥无法解密")
    void differentKeyCannotDecrypt() {
        String anotherKey = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
        AesGcmTextEncryptor anotherEncryptor = new AesGcmTextEncryptor(anotherKey);
        String cipherText = encryptor.encrypt("secret");
        // 用不同密钥解密应失败
        assertThrows(ServiceException.class, () -> anotherEncryptor.decrypt(cipherText));
    }
}
