package com.oinsist.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oinsist.common.web.config.JacksonConfig;
import com.oinsist.system.service.SysRoleService;
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
 * 角色管理 Controller 接口契约测试
 * <p>
 * 只验证 HTTP 映射和统一响应结构，菜单授权查询细节交给 Service/Mapper 层负责。
 * </p>
 */
class SysRoleControllerTest {

    private SysRoleService sysRoleService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        sysRoleService = mock(SysRoleService.class);

        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().jacksonCustomizer().customize(builder);
        ObjectMapper objectMapper = builder.build();

        mockMvc = MockMvcBuilders.standaloneSetup(new SysRoleController(sysRoleService))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    @Test
    @DisplayName("查询角色当前菜单ID集合，雪花ID按全局规则输出为字符串")
    void listMenuIdsShouldReturnCurrentRoleMenuIds() throws Exception {
        long snowflakeMenuId = 2062030192421740546L;
        when(sysRoleService.listMenuIdsByRoleId(1L)).thenReturn(List.of(1L, snowflakeMenuId));

        mockMvc.perform(get("/system/role/1/menuIds"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                  "code": 200,
                  "msg": "操作成功",
                  "data": [1, "2062030192421740546"],
                  "success": true
                }
                """));
    }
}
