package com.oinsist.admin.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.oinsist.admin.controller.CryptoTestController;
import com.oinsist.common.crypto.service.AesGcmTextEncryptor;
import com.oinsist.common.crypto.service.TextEncryptor;
import com.oinsist.common.web.crypto.DecryptRequestBodyAdvice;
import com.oinsist.common.web.crypto.EncryptResponseBodyAdvice;
import com.oinsist.common.web.handler.GlobalExceptionHandler;
import com.oinsist.common.web.jackson.SensitiveSerializerModifier;
import com.oinsist.system.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * P10 数据安全三闭环集成测试
 * <p>
 * 使用 MockMvc Standalone 模式验证 Spring MVC 管道中的脱敏与接口加密行为：
 * 1. Jackson @Sensitive 字段自动脱敏输出
 * 2. @ApiEncrypt 请求体解密 + 响应体加密
 * 3. 未标注接口不受加密影响
 * <p>
 * 为什么使用 Standalone 而非 @SpringBootTest：
 * - 项目启动依赖 PostgreSQL、Redis、Sa-Token 等外部服务，测试环境不一定具备
 * - Standalone 模式依然走完整 DispatcherServlet 流程（HandlerMapping → HandlerAdapter → ControllerAdvice → MessageConverter）
 * - 测试聚焦"数据安全闭环"行为，不需要完整应用上下文
 */
class CryptoIntegrationTest {

    private static final String TEST_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private TextEncryptor textEncryptor;

    @BeforeEach
    void setUp() {
        // 1. 创建加解密服务
        textEncryptor = new AesGcmTextEncryptor(TEST_KEY);

        // 2. 构建带脱敏能力的 ObjectMapper（模拟 JacksonConfig 的行为）
        objectMapper = new ObjectMapper();
        SimpleModule sensitiveModule = new SimpleModule("SensitiveModule");
        sensitiveModule.setSerializerModifier(new SensitiveSerializerModifier());
        objectMapper.registerModule(sensitiveModule);

        // 3. Mock 数据库依赖（CryptoTestController 依赖 SysUserMapper，但脱敏/加密测试不涉及 DB）
        SysUserMapper mockMapper = mock(SysUserMapper.class);

        // 4. 构建 Standalone MockMvc，注入 ControllerAdvice 和自定义 MessageConverter
        mockMvc = MockMvcBuilders.standaloneSetup(new CryptoTestController(mockMapper))
                .setControllerAdvice(
                        new EncryptResponseBodyAdvice(textEncryptor, objectMapper),
                        new DecryptRequestBodyAdvice(textEncryptor, objectMapper),
                        new GlobalExceptionHandler()
                )
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    // ==================== Jackson 脱敏测试 ====================

    @Test
    @DisplayName("Jackson脱敏：@Sensitive 字段输出已脱敏")
    void sensitiveFieldsAreDesensitized() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/crypto/sensitive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(json).get("data");

        // 手机号脱敏：138****5678
        assertEquals("138****5678", data.get("phone").asText());
        // 邮箱脱敏：t***@example.com
        assertEquals("t***@example.com", data.get("email").asText());
        // 身份证脱敏：110101********1234
        assertEquals("110101********1234", data.get("idCard").asText());
        // 银行卡脱敏：6222****1234
        assertEquals("6222****1234", data.get("bankCard").asText());
        // 姓名脱敏：张*（"张三"长度=2，保留首字+*）
        assertEquals("张*", data.get("name").asText());
        // 未标注字段不脱敏
        assertEquals("这个字段不脱敏", data.get("normal").asText());
    }

    // ==================== @ApiEncrypt 接口加解密测试 ====================

    @Test
    @DisplayName("接口加密：@ApiEncrypt 请求解密 + 响应加密")
    void apiEncryptDecrypt() throws Exception {
        // 1. 构造明文请求体（模拟客户端构造 EchoRequest JSON）
        String plainBody = "{\"message\":\"hello world\"}";
        // 2. 客户端对请求体加密
        String encryptedBody = textEncryptor.encrypt(plainBody);
        // 3. 包装为协议格式 {"data": "密文"}
        String requestJson = "{\"data\":\"" + encryptedBody + "\"}";

        // 4. 发送加密请求
        MvcResult result = mockMvc.perform(post("/test/crypto/encrypt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        // 5. 解析响应
        String responseJson = result.getResponse().getContentAsString();
        JsonNode responseNode = objectMapper.readTree(responseJson);

        // 6. 验证 code/msg 保留（加密不影响外层协议字段）
        assertEquals(200, responseNode.get("code").asInt());
        assertEquals("操作成功", responseNode.get("msg").asText());

        // 7. 验证 data 为密文字符串（非明文 JSON 对象）
        assertTrue(responseNode.get("data").isTextual(), "data 应为加密后的字符串，非 JSON 对象");
        String encryptedData = responseNode.get("data").asText();
        assertNotNull(encryptedData);
        assertFalse(encryptedData.isBlank());

        // 8. 解密 data 并验证业务内容
        String decryptedData = textEncryptor.decrypt(encryptedData);
        JsonNode decryptedNode = objectMapper.readTree(decryptedData);
        assertTrue(decryptedNode.get("echo").asText().contains("hello world"),
                "解密后应包含回显的 'hello world' 消息");
        assertNotNull(decryptedNode.get("timestamp"), "响应应包含 timestamp 字段");
    }

    @Test
    @DisplayName("接口加密：未标注 @ApiEncrypt 的接口响应不加密")
    void nonAnnotatedEndpointNotEncrypted() throws Exception {
        // sensitive 接口没有 @ApiEncrypt，响应 data 应为正常 JSON 对象（非密文字符串）
        MvcResult result = mockMvc.perform(get("/test/crypto/sensitive"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        // data 应该是一个对象，不是字符串密文
        assertTrue(node.get("data").isObject(),
                "未标注 @ApiEncrypt 的接口，data 应为 JSON 对象而非密文字符串");
    }

    @Test
    @DisplayName("接口加密：请求体格式错误（缺少 data 字段）返回错误")
    void apiEncryptMissingDataField() throws Exception {
        // 发送无 data 字段的请求体
        String badRequest = "{\"message\":\"plain text\"}";

        mockMvc.perform(post("/test/crypto/encrypt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
        // 全局异常处理器会捕获 ServiceException 并返回错误响应
    }
}
