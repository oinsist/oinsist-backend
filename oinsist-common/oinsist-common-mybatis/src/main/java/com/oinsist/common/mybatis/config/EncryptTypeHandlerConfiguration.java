package com.oinsist.common.mybatis.config;

import com.oinsist.common.crypto.service.TextEncryptor;
import com.oinsist.common.mybatis.handler.EncryptTypeHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * EncryptTypeHandler 自动配置
 * <p>
 * 解决的核心问题：
 * MyBatis TypeHandler 由 MyBatis 框架直接实例化，无法参与 Spring 依赖注入。
 * 本配置类作为桥接器，在 Spring 容器完全就绪后，将 TextEncryptor Bean
 * 通过静态方法注入到 EncryptTypeHandler，使其能够使用加解密能力。
 * <p>
 * 容错设计：
 * 当未配置 aes-key 时（即容器中无 TextEncryptor Bean），
 * 本配置类仍会加载并打印 warn 日志警告，而非静默跳过。
 * 实际使用加密字段时会在运行时由 EncryptTypeHandler 抛出明确异常。
 */
@Slf4j
@Configuration
public class EncryptTypeHandlerConfiguration {

    @Autowired(required = false)
    private TextEncryptor textEncryptor;

    /**
     * 容器初始化后注入 TextEncryptor 到 TypeHandler 的静态字段
     */
    @PostConstruct
    public void init() {
        if (textEncryptor != null) {
            EncryptTypeHandler.setTextEncryptor(textEncryptor);
            log.info("EncryptTypeHandler 初始化完成，字段加解密能力已就绪");
        } else {
            log.warn("未配置 oinsist.crypto.aes-key，字段加解密功能不可用。如使用了 @EncryptField 字段将在运行时报错");
        }
    }
}
