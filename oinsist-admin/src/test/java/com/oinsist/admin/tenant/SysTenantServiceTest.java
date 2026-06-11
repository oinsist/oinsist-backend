package com.oinsist.admin.tenant;

import com.oinsist.common.core.exception.ServiceException;
import com.oinsist.system.mapper.SysTenantMapper;
import com.oinsist.system.service.SysTenantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 租户服务核心保护规则测试
 */
class SysTenantServiceTest {

    @Test
    @DisplayName("默认租户不能删除")
    void deleteDefaultTenantShouldBeRejected() {
        SysTenantMapper mapper = mock(SysTenantMapper.class);
        SysTenantService service = new SysTenantService(mapper);

        assertThrows(ServiceException.class, () -> service.deleteTenant(1L));
        verifyNoInteractions(mapper);
    }
}
