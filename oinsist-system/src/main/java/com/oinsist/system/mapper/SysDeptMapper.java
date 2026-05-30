package com.oinsist.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oinsist.system.domain.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

/**
 * 部门 Mapper
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 查询指定部门的所有子部门ID（递归）
     * <p>使用 PostgreSQL 的递归 CTE 实现部门树向下遍历</p>
     */
    @Select("""
        WITH RECURSIVE dept_tree AS (
            SELECT dept_id FROM sys_dept WHERE parent_id = #{deptId} AND deleted = 0
            UNION ALL
            SELECT d.dept_id FROM sys_dept d INNER JOIN dept_tree dt ON d.parent_id = dt.dept_id WHERE d.deleted = 0
        )
        SELECT dept_id FROM dept_tree
        """)
    Set<Long> selectChildDeptIds(@Param("deptId") Long deptId);
}
