package com.oinsist.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oinsist.common.core.exception.ServiceException;
import com.oinsist.system.domain.SysMenu;
import com.oinsist.system.domain.SysRoleMenu;
import com.oinsist.system.domain.dto.SysMenuAddDto;
import com.oinsist.system.domain.dto.SysMenuEditDto;
import com.oinsist.system.domain.vo.MetaVo;
import com.oinsist.system.domain.vo.RouterVo;
import com.oinsist.system.mapper.SysMenuMapper;
import com.oinsist.system.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private final SysRoleMenuMapper sysRoleMenuMapper;

    /** 菜单类型合法值集合：M=目录、C=菜单、F=按钮 */
    private static final Set<String> VALID_MENU_TYPES = Set.of("M", "C", "F");

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

    // ==================== 菜单管理 CRUD ====================

    /**
     * 查询全量菜单树
     * <p>
     * 用于管理端菜单列表展示，按 orderNum 排序后递归构建树形结构。
     * 与 getRouterTree 的区别：此方法不区分角色，返回所有菜单（含按钮类型）。
     * </p>
     *
     * @return 树形菜单列表
     */
    public List<SysMenu> listMenuTree() {
        // 查询全量菜单，按显示顺序排序
        List<SysMenu> menus = sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getOrderNum)
        );
        // 复用现有的树构建方法，以 0 作为顶级父节点
        return buildMenuTree(menus, 0L);
    }

    /**
     * 根据ID查询单个菜单
     *
     * @param menuId 菜单ID
     * @return 菜单实体
     */
    public SysMenu selectById(Long menuId) {
        return sysMenuMapper.selectById(menuId);
    }

    /**
     * 新增菜单
     * <p>
     * 对 menuType 进行合法性校验后构建实体并插入。
     * parentId 未传时默认为 0（顶级菜单）。
     * </p>
     *
     * @param dto 新增菜单请求
     */
    public void addMenu(SysMenuAddDto dto) {
        validateMenuType(dto.getMenuType());

        SysMenu menu = new SysMenu();
        menu.setMenuName(dto.getMenuName());
        menu.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        menu.setOrderNum(dto.getOrderNum());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setPerms(dto.getPerms());
        menu.setMenuType(dto.getMenuType());
        menu.setVisible(dto.getVisible());
        menu.setStatus(dto.getStatus());
        menu.setIcon(dto.getIcon());

        sysMenuMapper.insert(menu);
    }

    /**
     * 编辑菜单
     * <p>
     * 根据 menuId 更新菜单信息，menuType 同样需要校验合法性。
     * </p>
     *
     * @param dto 编辑菜单请求
     */
    public void editMenu(SysMenuEditDto dto) {
        // 校验菜单是否存在，防止 updateById 对不存在的记录静默成功
        SysMenu existing = sysMenuMapper.selectById(dto.getMenuId());
        if (existing == null) {
            throw new ServiceException("菜单不存在");
        }

        validateMenuType(dto.getMenuType());

        SysMenu menu = new SysMenu();
        menu.setMenuId(dto.getMenuId());
        menu.setMenuName(dto.getMenuName());
        menu.setParentId(dto.getParentId());
        menu.setOrderNum(dto.getOrderNum());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setPerms(dto.getPerms());
        menu.setMenuType(dto.getMenuType());
        menu.setVisible(dto.getVisible());
        menu.setStatus(dto.getStatus());
        menu.setIcon(dto.getIcon());

        sysMenuMapper.updateById(menu);
    }

    /**
     * 删除菜单
     * <p>
     * 删除前需要校验是否存在子菜单，防止出现孤儿节点。
     * 删除后还需清理 sys_role_menu 关联表中对应的授权记录，
     * 避免角色引用到已失效的菜单ID。
     * </p>
     * <p>
     * 注意：菜单本身通过 @TableLogic 逻辑删除；
     * 而 sys_role_menu 关联表为物理删除（关联表无需保留历史记录）。
     * </p>
     *
     * @param menuId 菜单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long menuId) {
        // 校验菜单是否存在，防止对不存在的记录静默返回成功
        SysMenu menu = sysMenuMapper.selectById(menuId);
        if (menu == null) {
            throw new ServiceException("菜单不存在");
        }

        // 校验是否存在子菜单
        long childCount = sysMenuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, menuId)
        );
        if (childCount > 0) {
            throw new ServiceException("存在子菜单，不允许删除");
        }

        // 逻辑删除菜单本身（BaseEntity 中 @TableLogic 字段生效）
        sysMenuMapper.deleteById(menuId);

        // 物理删除角色-菜单关联记录
        sysRoleMenuMapper.delete(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, menuId)
        );
    }

    /**
     * 校验菜单类型是否合法
     *
     * @param menuType 菜单类型
     */
    private void validateMenuType(String menuType) {
        if (!VALID_MENU_TYPES.contains(menuType)) {
            throw new ServiceException("菜单类型不合法，仅支持 M/C/F");
        }
    }
}
