package com.oinsist.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oinsist.common.web.config.JacksonConfig;
import com.oinsist.system.domain.vo.TenantOptionVo;
import com.oinsist.system.service.SysLoginService;
import com.oinsist.system.service.SysMenuService;
import com.oinsist.system.service.SysTenantService;
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
 * 登录页租户选项接口契约测试
 */
class AuthTenantControllerTest {

    private SysTenantService sysTenantService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SysLoginService sysLoginService = mock(SysLoginService.class);
        SysMenuService sysMenuService = mock(SysMenuService.class);
        sysTenantService = mock(SysTenantService.class);

        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().jacksonCustomizer().customize(builder);
        ObjectMapper objectMapper = builder.build();

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(sysLoginService, sysMenuService, sysTenantService))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    @Test
    @DisplayName("登录前可查询正常租户选项")
    void tenantOptionsShouldReturnAvailableTenants() throws Exception {
        TenantOptionVo tenant = new TenantOptionVo();
        tenant.setTenantId(1L);
        tenant.setTenantName("默认租户");
        when(sysTenantService.listAvailableTenants()).thenReturn(List.of(tenant));

        mockMvc.perform(get("/auth/tenants"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                  "code": 200,
                  "msg": "操作成功",
                  "data": [
                    {
                      "tenantId": 1,
                      "tenantName": "默认租户"
                    }
                  ],
                  "success": true
                }
                """));
    }
}
