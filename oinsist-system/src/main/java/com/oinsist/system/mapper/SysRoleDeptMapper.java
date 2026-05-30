package com.oinsist.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oinsist.system.domain.SysRoleDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

/**
 * 角色-部门关联 Mapper
 */
@Mapper
public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {

    /**
     * 查询角色关联的自定义部门ID集合
     */
    @Select("SELECT dept_id FROM sys_role_dept WHERE role_id = #{roleId}")
    Set<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);
}
