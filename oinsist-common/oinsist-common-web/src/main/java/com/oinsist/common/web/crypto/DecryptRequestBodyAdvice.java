package com.oinsist.common.web.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oinsist.common.core.exception.ServiceException;
import com.oinsist.common.crypto.annotation.ApiEncrypt;
import com.oinsist.common.crypto.service.TextEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * 请求体解密处理器
 * <p>
 * 工作原理：
 * Spring MVC 在读取 @RequestBody 参数前，会调用 RequestBodyAdvice 链。
 * 本类拦截标注了 @ApiEncrypt 的接口，将加密的请求体解密后传递给后续处理。
 * <p>
 * 协议约定：
 * 客户端发送格式：{"data": "Base64编码的AES-GCM密文"}
 * 解密后：将明文 JSON 字符串作为新的请求体传入，由 Jackson 正常反序列化为目标对象
 * <p>
 * 为什么使用 RequestBodyAdviceAdapter 而非 Filter：
 * 1. Filter 层拿不到 Controller 方法注解信息，无法精确判断哪些接口需要解密
 * 2. RequestBodyAdvice 在消息转换器之前执行，时机精准
 * 3. 可以只修改 InputStream，不影响其他请求处理流程
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@ConditionalOnBean(TextEncryptor.class)
public class DecryptRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private final TextEncryptor textEncryptor;
    private final ObjectMapper objectMapper;

    /**
     * 判断是否需要解密：仅对标注了 @ApiEncrypt 的方法或类生效
     */
    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return methodParameter.hasMethodAnnotation(ApiEncrypt.class)
                || methodParameter.getContainingClass().isAnnotationPresent(ApiEncrypt.class);
    }

    /**
     * 在请求体读取前进行解密
     * <p>
     * 流程：读取原始 InputStream → 解析 {"data":"密文"} → 解密 → 替换为明文 InputStream
     */
    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                           Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        // 1. 读取原始请求体
        String requestBody = new String(inputMessage.getBody().readAllBytes(), StandardCharsets.UTF_8);

        // 2. 解析加密包装格式 {"data": "密文"}
        JsonNode node = objectMapper.readTree(requestBody);
        JsonNode dataNode = node.get("data");
        if (dataNode == null || dataNode.isNull() || !dataNode.isTextual()) {
            throw new ServiceException("加密接口请求格式错误，缺少 data 字段");
        }

        // 3. 解密得到明文 JSON
        String plainText = textEncryptor.decrypt(dataNode.asText());
        log.debug("接口请求体解密完成，目标类型：{}", targetType.getTypeName());

        // 4. 构造新的 HttpInputMessage，用明文替换原始请求体
        byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(plainBytes);
            }

            @Override
            public HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }
}
