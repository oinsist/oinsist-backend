package com.oinsist.common.mybatis.handler;

import com.oinsist.common.core.exception.ServiceException;
import com.oinsist.common.crypto.service.TextEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库字段加解密 TypeHandler
 * <p>
 * 核心机制：
 * - 写入（setParameter）：将明文加密后存入数据库，确保数据库中只有密文
 * - 读取（getResult）：从数据库取出密文后解密，返回明文给业务层
 * <p>
 * 为什么使用 static holder 注入 TextEncryptor：
 * MyBatis 的 TypeHandler 由 MyBatis 框架自行实例化（通过反射 newInstance），
 * 不经过 Spring 容器管理，因此无法使用 @Autowired 注入 Spring Bean。
 * 解决方案是通过静态方法在 Spring 容器启动后注入 TextEncryptor 实例。
 * <p>
 * 使用方式：
 * 1. 实体字段添加 @TableField(typeHandler = EncryptTypeHandler.class)
 * 2. 实体类添加 @TableName(autoResultMap = true) 以启用查询映射
 * <p>
 * 重要限制：
 * - 加密后的字段不支持 SQL LIKE 模糊查询（密文无法保留明文字典序）
 * - 不支持数据库层面的排序（ORDER BY）
 * - 不支持数据库层面的唯一约束校验（相同明文每次加密产生不同密文）
 * - 如需检索，应额外存储明文的哈希值作为索引列
 */
@Slf4j
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class EncryptTypeHandler extends BaseTypeHandler<String> {

    /**
     * 静态持有 TextEncryptor 实例
     * 由 Spring 容器启动后通过 setTextEncryptor() 注入
     */
    private static TextEncryptor textEncryptor;

    /**
     * 由自动配置类在 Spring 容器就绪后调用，注入加解密服务
     */
    public static void setTextEncryptor(TextEncryptor encryptor) {
        textEncryptor = encryptor;
        log.info("EncryptTypeHandler 已注入 TextEncryptor 实例");
    }

    /**
     * 写入数据库前加密
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ensureEncryptorAvailable();
        // 加密明文后写入（TextEncryptor.encrypt 失败会抛 ServiceException）
        ps.setString(i, textEncryptor.encrypt(parameter));
    }

    /**
     * 从 ResultSet 按列名读取并解密
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String cipherText = rs.getString(columnName);
        return decryptIfNotNull(cipherText);
    }

    /**
     * 从 ResultSet 按列索引读取并解密
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String cipherText = rs.getString(columnIndex);
        return decryptIfNotNull(cipherText);
    }

    /**
     * 从 CallableStatement 读取并解密（存储过程场景）
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String cipherText = cs.getString(columnIndex);
        return decryptIfNotNull(cipherText);
    }

    /**
     * 空值安全解密：null 或空字符串直接返回，非空则解密
     */
    private String decryptIfNotNull(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            return cipherText;
        }
        ensureEncryptorAvailable();
        return textEncryptor.decrypt(cipherText);
    }

    /**
     * 确保 TextEncryptor 已注入，未配置密钥时给出明确的错误提示而非 NPE
     */
    private static void ensureEncryptorAvailable() {
        if (textEncryptor == null) {
            throw new ServiceException("字段加解密服务未初始化，请检查是否配置了 oinsist.crypto.aes-key");
        }
    }
}
