package com.oinsist.system.service;

import com.oinsist.system.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 部门 Service
 */
@Service
@RequiredArgsConstructor
public class SysDeptService {

    private final SysDeptMapper deptMapper;

    /**
     * 查询部门的所有子部门ID
     */
    public Set<Long> getChildDeptIds(Long deptId) {
        return deptMapper.selectChildDeptIds(deptId);
    }
}
