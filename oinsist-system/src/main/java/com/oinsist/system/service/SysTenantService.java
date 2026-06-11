package com.oinsist.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oinsist.common.core.exception.ServiceException;
import com.oinsist.common.mybatis.domain.PageQuery;
import com.oinsist.common.mybatis.domain.PageResult;
import com.oinsist.system.domain.SysTenant;
import com.oinsist.system.domain.dto.SysTenantAddDto;
import com.oinsist.system.domain.dto.SysTenantEditDto;
import com.oinsist.system.domain.vo.SysTenantVo;
import com.oinsist.system.domain.vo.TenantOptionVo;
import com.oinsist.system.mapper.SysTenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 租户管理服务
 *
 * <p>租户表本身是全局管理表，不受 tenant_id 行级拦截。这里保留最小 CRUD，
 * 真正的数据隔离仍由 MyBatis-Plus 租户拦截器统一处理，避免每个业务 Service 手写租户条件。</p>
 */
@Service
@RequiredArgsConstructor
public class SysTenantService {

    private static final long DEFAULT_TENANT_ID = 1L;

    private final SysTenantMapper sysTenantMapper;

    /** 分页查询租户 */
    public PageResult<SysTenantVo> listTenants(PageQuery pageQuery) {
        Page<SysTenant> page = sysTenantMapper.selectPage(
                pageQuery.buildPage(),
                new LambdaQueryWrapper<SysTenant>().orderByDesc(SysTenant::getCreateTime)
        );
        Page<SysTenantVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVo).toList());
        return PageResult.build(voPage);
    }

    /** 登录页只展示正常状态的租户 */
    public List<TenantOptionVo> listAvailableTenants() {
        return sysTenantMapper.selectList(
                new LambdaQueryWrapper<SysTenant>()
                        .eq(SysTenant::getStatus, "0")
                        .orderByAsc(SysTenant::getTenantId)
        ).stream().map(this::toOptionVo).toList();
    }

    /** 根据 ID 查询租户 */
    public SysTenantVo selectById(Long tenantId) {
        SysTenant tenant = sysTenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ServiceException("租户不存在");
        }
        return toVo(tenant);
    }

    /** 新增租户 */
    public void addTenant(SysTenantAddDto dto) {
        checkTenantNameUnique(dto.getTenantName(), null);
        SysTenant tenant = new SysTenant();
        tenant.setTenantName(dto.getTenantName());
        tenant.setContact(dto.getContact());
        tenant.setStatus(dto.getStatus() != null ? dto.getStatus() : "0");
        sysTenantMapper.insert(tenant);
    }

    /** 修改租户 */
    public void editTenant(SysTenantEditDto dto) {
        SysTenant existing = sysTenantMapper.selectById(dto.getTenantId());
        if (existing == null) {
            throw new ServiceException("租户不存在");
        }
        checkTenantNameUnique(dto.getTenantName(), dto.getTenantId());

        SysTenant tenant = new SysTenant();
        tenant.setTenantId(dto.getTenantId());
        tenant.setTenantName(dto.getTenantName());
        tenant.setContact(dto.getContact());
        tenant.setStatus(dto.getStatus());
        sysTenantMapper.updateById(tenant);
    }

    /** 删除租户 */
    public void deleteTenant(Long tenantId) {
        if (tenantId == DEFAULT_TENANT_ID) {
            throw new ServiceException("默认租户不允许删除");
        }
        SysTenant existing = sysTenantMapper.selectById(tenantId);
        if (existing == null) {
            throw new ServiceException("租户不存在");
        }
        sysTenantMapper.deleteById(tenantId);
    }

    private void checkTenantNameUnique(String tenantName, Long excludeTenantId) {
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getTenantName, tenantName);
        if (excludeTenantId != null) {
            wrapper.ne(SysTenant::getTenantId, excludeTenantId);
        }
        if (sysTenantMapper.exists(wrapper)) {
            throw new ServiceException("租户名称已存在");
        }
    }

    private SysTenantVo toVo(SysTenant tenant) {
        SysTenantVo vo = new SysTenantVo();
        vo.setTenantId(tenant.getTenantId());
        vo.setTenantName(tenant.getTenantName());
        vo.setContact(tenant.getContact());
        vo.setStatus(tenant.getStatus());
        vo.setCreateTime(tenant.getCreateTime());
        return vo;
    }

    private TenantOptionVo toOptionVo(SysTenant tenant) {
        TenantOptionVo vo = new TenantOptionVo();
        vo.setTenantId(tenant.getTenantId());
        vo.setTenantName(tenant.getTenantName());
        return vo;
    }
}
