package com.oinsist.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oinsist.system.domain.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色-菜单关联表 Mapper
 * <p>
 * 用于管理端批量维护角色与菜单的授权关系，
 * 典型操作：先按 roleId 删除旧关联，再批量插入新关联（"先删后增"策略）。
 * </p>
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
}
