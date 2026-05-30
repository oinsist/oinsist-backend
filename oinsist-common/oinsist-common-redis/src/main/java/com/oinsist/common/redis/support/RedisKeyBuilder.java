package com.oinsist.common.redis.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Redis Key 构建工具
 * <p>
 * 统一管理 Redis key 的命名规则，避免各处硬编码前缀，保证 key 格式一致。
 * 参数摘要使用 MD5 哈希，避免直接存储敏感明文参数到 Redis。
 * </p>
 */
public final class RedisKeyBuilder {
    
    private RedisKeyBuilder() {}
    
    /** 分布式锁 key 前缀 */
    public static final String LOCK_PREFIX = "lock:";
    
    /** 幂等标记 key 前缀 */
    public static final String IDEMPOTENT_PREFIX = "idempotent:";
    
    /** 防重复提交 key 前缀 */
    public static final String REPEAT_SUBMIT_PREFIX = "repeat_submit:";
    
    /**
     * 构建完整的 Redis key
     *
     * @param prefix 前缀（如 "lock:"）
     * @param parts  key 的组成部分
     * @return 拼接后的完整 key
     */
    public static String buildKey(String prefix, String... parts) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(":");
            }
            sb.append(parts[i]);
        }
        return sb.toString();
    }
    
    /**
     * 对参数进行 MD5 摘要
     * <p>
     * 用于防重复提交场景，避免将请求参数明文存入 Redis，
     * 既保护敏感信息，又保持 key 长度可控。
     * </p>
     *
     * @param content 待摘要的原始内容
     * @return MD5 十六进制字符串
     */
    public static String md5Digest(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 在所有 JVM 中必须支持，此异常不应出现
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }
}
