package com.oinsist.admin.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oinsist.common.web.config.JacksonConfig;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * BigNumberSerializer 序列化行为验证
 * <p>
 * 不启动 Spring 容器、不依赖数据库/Redis：直接调用真实的 {@link JacksonConfig#jacksonCustomizer()}
 * 把序列化器装配进 ObjectMapper，既验证序列化器自身逻辑，又验证 JacksonConfig 的注册装配。
 * </p>
 * <p>
 * 验证目标：雪花算法主键（19 位 Long，超出 JS Number.MAX_SAFE_INTEGER）必须以「字符串」下发，
 * 而安全范围内的小整数仍以「数字」下发，避免前端被迫到处做类型转换。
 * </p>
 */
class BigNumberSerializerTest {

    /** JS Number.MAX_SAFE_INTEGER = 2^53 - 1 */
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 复用生产配置：经由真实的 JacksonConfig 装配，保证测试与运行时序列化行为一致
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().jacksonCustomizer().customize(builder);
        objectMapper = builder.build();
    }

    @Test
    @DisplayName("雪花大 Long 序列化为带引号的字符串，规避 JS 精度丢失")
    void snowflakeLongShouldSerializeAsString() throws Exception {
        long snowflakeId = 1933765234567890123L; // 19 位，远超 2^53
        IdHolder holder = new IdHolder(snowflakeId, 0);

        String json = objectMapper.writeValueAsString(holder);

        // userId 带引号 → 字符串；status 不带引号 → 数字
        assertEquals("{\"userId\":\"1933765234567890123\",\"status\":0}", json);
    }

    @Test
    @DisplayName("安全范围内的小 Long 仍序列化为数字，不影响前端使用习惯")
    void smallLongShouldSerializeAsNumber() throws Exception {
        IdHolder holder = new IdHolder(1L, 1);

        String json = objectMapper.writeValueAsString(holder);

        assertEquals("{\"userId\":1,\"status\":1}", json);
    }

    @Test
    @DisplayName("边界值：等于 MAX_SAFE_INTEGER 输出数字，+1 越界后输出字符串")
    void boundaryValueShouldSwitchAtSafeInteger() throws Exception {
        assertEquals("{\"userId\":9007199254740991,\"status\":0}",
                objectMapper.writeValueAsString(new IdHolder(MAX_SAFE_INTEGER, 0)));

        assertEquals("{\"userId\":\"9007199254740992\",\"status\":0}",
                objectMapper.writeValueAsString(new IdHolder(MAX_SAFE_INTEGER + 1, 0)));
    }

    /** 模拟典型实体：userId 为 Long（雪花主键），status 为 int（小整数，不应被波及） */
    @Data
    static class IdHolder {
        private final Long userId;
        private final int status;
    }
}
