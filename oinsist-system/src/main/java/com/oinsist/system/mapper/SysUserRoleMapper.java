package com.oinsist.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oinsist.system.domain.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-角色关联表 Mapper
 * <p>
 * 用于管理端批量维护用户与角色的授权关系，
 * 典型操作：先按 userId 删除旧关联，再批量插入新关联（"先删后增"策略）。
 * </p>
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
