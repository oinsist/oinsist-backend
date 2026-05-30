package com.oinsist.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oinsist.system.domain.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 菜单 Mapper 接口
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 根据用户ID查询权限标识集合
     * <p>
     * 通过 sys_user_role + sys_role_menu 两层关联查询，
     * 获取用户拥有的所有菜单/按钮权限标识（perms 字段）。
     * 仅返回状态正常且未删除的菜单权限。
     * </p>
     *
     * @param userId 用户ID
     * @return 权限标识集合（如 ["system:user:list", "system:user:add"]）
     */
    Set<String> selectPermsByUserId(@Param("userId") Long userId);

    /**
     * 查询所有正常状态的目录和菜单（不含按钮）
     * <p>
     * 供超级管理员使用，直接查询全量菜单。
     * </p>
     *
     * @return 目录+菜单列表
     */
    List<SysMenu> selectMenuTreeAll();

    /**
     * 根据用户ID查询可访问的目录和菜单（不含按钮）
     * <p>
     * 通过 sys_user_role + sys_role_menu 关联查询，
     * 仅返回菜单类型为 M（目录）或 C（菜单）的记录。
     * </p>
     *
     * @param userId 用户ID
     * @return 该用户可访问的目录+菜单列表
     */
    List<SysMenu> selectMenuTreeByUserId(@Param("userId") Long userId);
}
