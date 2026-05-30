package com.oinsist.common.crypto.config;

import com.oinsist.common.crypto.service.AesGcmTextEncryptor;
import com.oinsist.common.crypto.service.TextEncryptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 加解密模块自动配置
 * <p>
 * 为什么使用自动配置而非 @Component 直接注册：
 * 1. 条件化加载：仅在配置了密钥时才创建 Bean，避免未配置时启动失败
 * 2. 可替换性：@ConditionalOnMissingBean 允许业务方自定义实现覆盖默认
 * 3. 模块解耦：crypto 模块被多个模块依赖，自动配置确保 Bean 全局唯一
 */
@AutoConfiguration
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoAutoConfiguration {

    /**
     * 注册 AES-GCM 加解密服务
     * <p>
     * 仅在配置了 oinsist.crypto.aes-key 时才激活，
     * 避免未配置密钥的模块启动时报错（如纯脱敏场景不需要加密能力）
     */
    @Bean
    @ConditionalOnMissingBean(TextEncryptor.class)
    @ConditionalOnProperty(prefix = "oinsist.crypto", name = "aes-key")
    public TextEncryptor textEncryptor(CryptoProperties properties) {
        return new AesGcmTextEncryptor(properties.getAesKey());
    }
}
