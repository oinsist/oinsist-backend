package com.oinsist.common.mybatis.handler;

import com.oinsist.common.core.exception.ServiceException;
import com.oinsist.common.crypto.service.AesGcmTextEncryptor;
import com.oinsist.common.crypto.service.TextEncryptor;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.*;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * EncryptTypeHandler 单元测试
 * <p>
 * 验证数据库字段加解密 TypeHandler 的核心行为：
 * 1. 写入时明文被加密（setNonNullParameter）
 * 2. 读取时密文被解密（getNullableResult）
 * 3. null/空值安全通过
 * 4. 未初始化时抛出明确异常（非 NPE）
 */
class EncryptTypeHandlerTest {

    private static final String TEST_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private EncryptTypeHandler handler;
    private TextEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new AesGcmTextEncryptor(TEST_KEY);
        EncryptTypeHandler.setTextEncryptor(encryptor);
        handler = new EncryptTypeHandler();
    }

    @Test
    @DisplayName("写入加密：setNonNullParameter 将明文加密后存入 PreparedStatement")
    void setNonNullParameterEncrypts() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        handler.setNonNullParameter(ps, 1, "test@example.com", JdbcType.VARCHAR);
        // 验证 ps.setString 被调用，且参数值不等于原始明文（即已加密）
        verify(ps).setString(eq(1), argThat(arg -> !arg.equals("test@example.com") && !arg.isBlank()));
    }

    @Test
    @DisplayName("写入加密对称性：加密后的密文可通过 TextEncryptor 解密还原")
    void encryptedValueCanBeDecrypted() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        handler.setNonNullParameter(ps, 1, "13812345678", JdbcType.VARCHAR);
        // 捕获实际写入的密文
        verify(ps).setString(eq(1), argThat(cipherText -> {
            String decrypted = encryptor.decrypt(cipherText);
            return "13812345678".equals(decrypted);
        }));
    }

    @Test
    @DisplayName("按列名读取解密：getNullableResult(ResultSet, columnName)")
    void getNullableResultByColumnNameDecrypts() throws Exception {
        String cipherText = encryptor.encrypt("13812345678");
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("phonenumber")).thenReturn(cipherText);

        String result = handler.getNullableResult(rs, "phonenumber");
        assertEquals("13812345678", result);
    }

    @Test
    @DisplayName("按列索引读取解密：getNullableResult(ResultSet, columnIndex)")
    void getNullableResultByColumnIndexDecrypts() throws Exception {
        String cipherText = encryptor.encrypt("test@example.com");
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(2)).thenReturn(cipherText);

        String result = handler.getNullableResult(rs, 2);
        assertEquals("test@example.com", result);
    }

    @Test
    @DisplayName("CallableStatement 读取解密：getNullableResult(CallableStatement, columnIndex)")
    void getNullableResultFromCallableStatementDecrypts() throws Exception {
        String cipherText = encryptor.encrypt("sensitive_data");
        CallableStatement cs = mock(CallableStatement.class);
        when(cs.getString(1)).thenReturn(cipherText);

        String result = handler.getNullableResult(cs, 1);
        assertEquals("sensitive_data", result);
    }

    @Test
    @DisplayName("空值安全：null 值直接通过不解密")
    void nullValuePassThrough() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("email")).thenReturn(null);

        String result = handler.getNullableResult(rs, "email");
        assertNull(result);
    }

    @Test
    @DisplayName("空白值安全：空字符串直接通过不解密")
    void blankValuePassThrough() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("email")).thenReturn("");

        String result = handler.getNullableResult(rs, "email");
        assertEquals("", result);
    }

    @Test
    @DisplayName("未初始化时写入抛出 ServiceException（非 NPE）")
    void throwsServiceExceptionOnWriteWhenNotInitialized() {
        EncryptTypeHandler.setTextEncryptor(null);
        EncryptTypeHandler uninitHandler = new EncryptTypeHandler();

        PreparedStatement ps = mock(PreparedStatement.class);
        ServiceException ex = assertThrows(ServiceException.class, () ->
                uninitHandler.setNonNullParameter(ps, 1, "test", JdbcType.VARCHAR));
        assertTrue(ex.getMessage().contains("未初始化"));
    }

    @Test
    @DisplayName("未初始化时读取非空值抛出 ServiceException（非 NPE）")
    void throwsServiceExceptionOnReadWhenNotInitialized() {
        EncryptTypeHandler.setTextEncryptor(null);
        EncryptTypeHandler uninitHandler = new EncryptTypeHandler();

        ResultSet rs = mock(ResultSet.class);
        try {
            when(rs.getString("phone")).thenReturn("some_cipher_text");
        } catch (Exception ignored) {
        }
        assertThrows(ServiceException.class, () ->
                uninitHandler.getNullableResult(rs, "phone"));
    }

    @AfterEach
    void tearDown() {
        // 恢复 encryptor 避免影响其他测试
        EncryptTypeHandler.setTextEncryptor(new AesGcmTextEncryptor(TEST_KEY));
    }
}
