package com.oinsist.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oinsist.common.web.config.JacksonConfig;
import com.oinsist.system.service.SysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户管理 Controller 接口契约测试
 * <p>
 * 使用 Standalone MockMvc 只验证 HTTP 映射和统一响应结构，
 * 业务查询细节交给 Service/Mapper 层负责，避免测试耦合到数据库。
 * </p>
 */
class SysUserControllerTest {

    private SysUserService sysUserService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        sysUserService = mock(SysUserService.class);

        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().jacksonCustomizer().customize(builder);
        ObjectMapper objectMapper = builder.build();

        mockMvc = MockMvcBuilders.standaloneSetup(new SysUserController(sysUserService))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    @Test
    @DisplayName("查询用户当前角色ID集合，雪花ID按全局规则输出为字符串")
    void listRoleIdsShouldReturnCurrentUserRoleIds() throws Exception {
        long snowflakeRoleId = 2061725689713627137L;
        when(sysUserService.listRoleIdsByUserId(1L)).thenReturn(List.of(1L, snowflakeRoleId));

        mockMvc.perform(get("/system/user/1/roleIds"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                  "code": 200,
                  "msg": "操作成功",
                  "data": [1, "2061725689713627137"],
                  "success": true
                }
                """));
    }
}
