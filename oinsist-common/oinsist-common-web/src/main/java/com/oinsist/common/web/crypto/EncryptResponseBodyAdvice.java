package com.oinsist.common.web.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oinsist.common.core.domain.R;
import com.oinsist.common.core.exception.ServiceException;
import com.oinsist.common.crypto.annotation.ApiEncrypt;
import com.oinsist.common.crypto.service.TextEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应体加密处理器
 * <p>
 * 工作原理：
 * Spring MVC 在将响应对象写入 HTTP 输出流前，会调用 ResponseBodyAdvice 链。
 * 本类拦截标注了 @ApiEncrypt 的接口，将响应中的 data 字段加密。
 * <p>
 * 协议约定：
 * - 原始响应：R(code=200, msg="操作成功", data={实际业务对象})
 * - 加密后响应：R(code=200, msg="操作成功", data="Base64编码的AES-GCM密文")
 * - 客户端拿到响应后，对 data 字段进行解密即可得到原始 JSON
 * <p>
 * 为什么只加密 data 而非整个响应体：
 * 1. code 和 msg 是通信协议的一部分，客户端需要先判断请求是否成功
 * 2. 如果整个响应都加密，客户端无法区分「加密成功响应」和「未加密错误响应」
 * 3. 保持 R 结构一致，降低客户端解析复杂度
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@ConditionalOnBean(TextEncryptor.class)
public class EncryptResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final TextEncryptor textEncryptor;
    private final ObjectMapper objectMapper;

    /**
     * 判断是否需要加密：仅对标注了 @ApiEncrypt 的方法或类生效
     */
    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.hasMethodAnnotation(ApiEncrypt.class)
                || returnType.getContainingClass().isAnnotationPresent(ApiEncrypt.class);
    }

    /**
     * 在响应体写出前加密 data 字段
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // 仅处理统一响应对象 R
        if (!(body instanceof R<?> r)) {
            log.warn("@ApiEncrypt 标注的接口返回值不是 R 类型，跳过加密");
            return body;
        }

        // data 为 null 时无需加密
        if (r.data() == null) {
            return body;
        }

        try {
            // 1. 将 data 序列化为 JSON 字符串
            String dataJson = objectMapper.writeValueAsString(r.data());

            // 2. 加密 JSON 字符串
            String encryptedData = textEncryptor.encrypt(dataJson);

            // 3. 构造新的 R 对象，data 替换为密文字符串，保留原始 code/msg
            log.debug("接口响应体加密完成");
            return new R<>(r.code(), r.msg(), encryptedData);
        } catch (Exception e) {
            log.error("响应体加密失败", e);
            // 加密失败不应返回明文数据（安全原则），抛出异常由全局处理器处理
            throw new ServiceException("响应加密失败");
        }
    }
}
