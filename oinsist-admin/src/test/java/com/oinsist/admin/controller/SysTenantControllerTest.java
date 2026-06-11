package com.oinsist.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oinsist.common.mybatis.domain.PageResult;
import com.oinsist.common.web.config.JacksonConfig;
import com.oinsist.system.domain.vo.SysTenantVo;
import com.oinsist.system.service.SysTenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 租户管理 Controller 接口契约测试
 */
class SysTenantControllerTest {

    private SysTenantService sysTenantService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        sysTenantService = mock(SysTenantService.class);

        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().jacksonCustomizer().customize(builder);
        ObjectMapper objectMapper = builder.build();

        mockMvc = MockMvcBuilders.standaloneSetup(new SysTenantController(sysTenantService))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    @Test
    @DisplayName("租户分页列表按统一分页协议返回")
    void listShouldReturnTenantPage() throws Exception {
        SysTenantVo tenant = new SysTenantVo();
        tenant.setTenantId(2062030192421740546L);
        tenant.setTenantName("演示租户");
        tenant.setContact("管理员");
        tenant.setStatus("0");

        PageResult<SysTenantVo> page = new PageResult<>();
        page.setRows(List.of(tenant));
        page.setTotal(1);
        when(sysTenantService.listTenants(any())).thenReturn(page);

        mockMvc.perform(get("/system/tenant/list"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                  "code": 200,
                  "msg": "操作成功",
                  "data": {
                    "rows": [
                      {
                        "tenantId": "2062030192421740546",
                        "tenantName": "演示租户",
                        "contact": "管理员",
                        "status": "0"
                      }
                    ],
                    "total": 1
                  },
                  "success": true
                }
                """));
    }
}
