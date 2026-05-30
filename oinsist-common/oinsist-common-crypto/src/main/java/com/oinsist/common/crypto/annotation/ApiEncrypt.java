package com.oinsist.common.crypto.annotation;

import java.lang.annotation.*;

/**
 * 接口加密注解
 * <p>
 * 标注在 Controller 的方法或类上，启用该接口的请求体解密和响应体加密。
 * <p>
 * 协议约定：
 * - 请求格式：{"data": "AES-GCM密文(Base64编码)"}
 * - 响应格式：保持 R<T> 结构，data 字段的值被加密为密文字符串
 * <p>
 * 设计思路：
 * - 通过 RequestBodyAdvice 在请求体读取前解密
 * - 通过 ResponseBodyAdvice 在响应体写出前加密
 * - 仅对标注了本注解的接口生效，未标注接口行为完全不变
 * - 不影响文件上传/下载、Swagger 文档、健康检查等非业务接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiEncrypt {
}
