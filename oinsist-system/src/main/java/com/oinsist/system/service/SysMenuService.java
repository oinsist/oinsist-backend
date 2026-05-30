package com.oinsist.system.service;

import com.oinsist.system.domain.SysMenu;
import com.oinsist.system.domain.vo.MetaVo;
import com.oinsist.system.domain.vo.RouterVo;
import com.oinsist.system.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单服务
 * <p>
 * 负责菜单树的查询与组装，为动态路由接口提供数据支撑。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper sysMenuMapper;

    /**
     * 根据用户ID获取动态路由树
     * <p>
     * admin 角色查询全量菜单，其他角色根据 sys_role_menu 关联查询。
     * 查询结果为扁平列表，需通过 {@link #buildMenuTree} 组装为前端所需的树结构。
     * </p>
     *
     * @param userId  用户ID
     * @param isAdmin 是否为超级管理员
     * @return 路由树
     */
    public List<RouterVo> getRouterTree(Long userId, boolean isAdmin) {
        List<SysMenu> menus;
        if (isAdmin) {
            // 超级管理员可访问所有菜单
            menus = sysMenuMapper.selectMenuTreeAll();
        } else {
            menus = sysMenuMapper.selectMenuTreeByUserId(userId);
        }
        // 将扁平菜单列表组装为树结构
        List<SysMenu> menuTree = buildMenuTree(menus, 0L);
        // 转换为前端路由 VO
        return buildRouterVo(menuTree);
    }

    /**
     * 递归构建菜单树
     * <p>
     * 从扁平列表中找出所有 parentId 等于指定值的节点作为当前层级，
     * 然后递归为每个节点查找其子节点。
     * </p>
     *
     * @param menus    扁平菜单列表
     * @param parentId 父级ID
     * @return 树形菜单列表
     */
    private List<SysMenu> buildMenuTree(List<SysMenu> menus, Long parentId) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (parentId.equals(menu.getParentId())) {
                menu.setChildren(buildMenuTree(menus, menu.getMenuId()));
                tree.add(menu);
            }
        }
        return tree;
    }

    /**
     * 将菜单树转换为前端路由 VO 树
     *
     * @param menus 树形菜单列表
     * @return 路由 VO 列表
     */
    private List<RouterVo> buildRouterVo(List<SysMenu> menus) {
        List<RouterVo> routers = new ArrayList<>();
        for (SysMenu menu : menus) {
            RouterVo router = new RouterVo();
            router.setName(capitalize(menu.getPath()));
            router.setPath("M".equals(menu.getMenuType()) ? "/" + menu.getPath() : menu.getPath());
            router.setComponent("M".equals(menu.getMenuType()) ? "Layout" : menu.getComponent());
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), false, "1".equals(menu.getVisible())));
            router.setHidden("1".equals(menu.getVisible()));

            // 递归处理子路由
            List<SysMenu> children = menu.getChildren();
            if (children != null && !children.isEmpty()) {
                router.setChildren(buildRouterVo(children));
            }
            routers.add(router);
        }
        return routers;
    }

    /**
     * 首字母大写（用于生成路由 name）
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
