package com.oinsist.common.crypto.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 加解密配置属性
 * <p>
 * 通过 application.yml 中的 oinsist.crypto.* 前缀进行配置。
 * <p>
 * 配置示例：
 * <pre>
 * oinsist:
 *   crypto:
 *     aes-key: "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
 * </pre>
 * <p>
 * 安全说明：
 * - aes-key 为 256 位 AES 密钥的十六进制编码（64个hex字符）
 * - 生产环境应通过环境变量或外部密钥管理系统注入，禁止提交到代码仓库
 * - 本项目为教学目的，仅在 dev 配置文件中配置示例密钥
 */
@Data
@ConfigurationProperties(prefix = "oinsist.crypto")
public class CryptoProperties {
    /**
     * AES-256-GCM 加密密钥（Hex 编码，64字符 = 256位）
     */
    private String aesKey;
}
