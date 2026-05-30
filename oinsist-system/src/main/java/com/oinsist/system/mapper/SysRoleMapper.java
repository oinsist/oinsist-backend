package com.oinsist.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oinsist.system.domain.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Set;

/**
 * 角色 Mapper 接口
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据用户ID查询角色标识集合
     * <p>
     * 通过 sys_user_role 关联表连接 sys_role，
     * 仅返回状态正常（status='0'）且未删除的角色标识。
     * </p>
     *
     * @param userId 用户ID
     * @return 角色标识集合（如 ["admin"]）
     */
    Set<String> selectRoleKeysByUserId(@Param("userId") Long userId);
}
