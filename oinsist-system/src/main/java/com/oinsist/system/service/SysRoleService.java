package com.oinsist.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oinsist.common.core.exception.ServiceException;
import com.oinsist.common.mybatis.domain.PageQuery;
import com.oinsist.common.mybatis.domain.PageResult;
import com.oinsist.system.domain.SysMenu;
import com.oinsist.system.domain.SysRole;
import com.oinsist.system.domain.SysRoleMenu;
import com.oinsist.system.domain.SysUserRole;
import com.oinsist.system.domain.dto.SysRoleAddDto;
import com.oinsist.system.domain.dto.SysRoleEditDto;
import com.oinsist.system.domain.vo.SysRoleVo;
import com.oinsist.system.mapper.SysMenuMapper;
import com.oinsist.system.mapper.SysRoleMapper;
import com.oinsist.system.mapper.SysRoleMenuMapper;
import com.oinsist.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色管理服务
 * <p>
 * 提供角色的 CRUD 操作以及角色-菜单授权能力。
 * 参考 RuoYi-Vue-Plus 的角色管理思想：
 * - roleKey 全局唯一，作为权限编码标识，Sa-Token 的权限校验最终依赖它
 * - 内置管理员角色（admin）不允许删除，确保系统始终有超级管理员兜底
 * - 角色删除时级联清理关联关系，避免产生孤立的中间表数据
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysMenuMapper sysMenuMapper;

    /**
     * 分页查询角色列表
     * <p>
     * 将 SysRole 实体转换为 SysRoleVo 返回，避免将数据库实体直接暴露给前端，
     * 后续可在 VO 转换中灵活控制返回字段（如隐藏内部审计信息）。
     * </p>
     *
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    public PageResult<SysRoleVo> listRoles(PageQuery pageQuery) {
        Page<SysRole> page = sysRoleMapper.selectPage(pageQuery.buildPage(), new LambdaQueryWrapper<>());
        // 将实体分页结果转换为 VO 分页结果
        List<SysRoleVo> voList = page.getRecords().stream()
                .map(this::toVo)
                .toList();
        PageResult<SysRoleVo> result = new PageResult<>();
        result.setRows(voList);
        result.setTotal(page.getTotal());
        return result;
    }

    /**
     * 根据角色ID查询单个角色
     *
     * @param roleId 角色ID
     * @return 角色 VO
     */
    public SysRoleVo selectById(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        return role == null ? null : toVo(role);
    }

    /**
     * 新增角色
     * <p>
     * 新增前校验 roleKey 唯一性：roleKey 是权限编码的唯一标识，
     * 若出现重复会导致不同角色拥有相同权限标识，造成权限逻辑混乱。
     * </p>
     *
     * @param dto 新增角色请求
     */
    public void addRole(SysRoleAddDto dto) {
        // 校验 roleKey 唯一性
        boolean exists = sysRoleMapper.exists(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleKey, dto.getRoleKey())
        );
        if (exists) {
            throw new ServiceException("角色标识已存在");
        }
        // 构建实体并插入
        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName());
        role.setRoleKey(dto.getRoleKey());
        role.setStatus(dto.getStatus());
        sysRoleMapper.insert(role);
    }

    /**
     * 编辑角色
     * <p>
     * 编辑时同样需要校验 roleKey 唯一性，但要排除自身记录，
     * 否则用户仅修改 roleName 而不改 roleKey 时会误判为重复。
     * </p>
     *
     * @param dto 编辑角色请求
     */
    public void editRole(SysRoleEditDto dto) {
        // 校验角色是否存在：防止对不存在的记录执行 updateById 静默成功
        SysRole existing = sysRoleMapper.selectById(dto.getRoleId());
        if (existing == null) {
            throw new ServiceException("角色不存在");
        }
        // 内置管理员角色标识保护：roleId==1 为系统内置 admin 角色，
        // 其 roleKey 是权限体系的根基标识，一旦被篡改会导致超级管理员权限判定失效
        if (dto.getRoleId() == 1L && !existing.getRoleKey().equals(dto.getRoleKey())) {
            throw new ServiceException("内置管理员角色标识不允许修改");
        }
        // 校验 roleKey 唯一性（排除自身）
        boolean exists = sysRoleMapper.exists(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleKey, dto.getRoleKey())
                        .ne(SysRole::getRoleId, dto.getRoleId())
        );
        if (exists) {
            throw new ServiceException("角色标识已存在");
        }
        // 按 roleId 更新允许修改的字段
        SysRole role = new SysRole();
        role.setRoleId(dto.getRoleId());
        role.setRoleName(dto.getRoleName());
        role.setRoleKey(dto.getRoleKey());
        role.setStatus(dto.getStatus());
        sysRoleMapper.updateById(role);
    }

    /**
     * 删除角色
     * <p>
     * 设计考量：
     * 1. 内置管理员保护 —— admin 角色是系统超级管理员，若被删除将导致无人拥有最高权限，
     *    系统陷入不可管理状态，因此必须在删除前拦截。
     * 2. 级联清理 —— 删除角色本身后，必须同时清理 sys_user_role 和 sys_role_menu 关联数据，
     *    否则会产生指向不存在角色的孤立记录，影响后续查询的正确性。
     * 3. 事务保证 —— 角色删除与关联清理必须在同一事务中，防止部分成功导致数据不一致。
     * </p>
     *
     * @param roleId 角色ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        // 第一重保护：基于 roleId 的硬编码判断。
        // 即使攻击者通过 editRole 篡改了 roleKey，只要 roleId==1 就一定是内置管理员，
        // 从根源上杜绝"先改标识再删除"的绕过攻击路径
        if (roleId == 1L) {
            throw new ServiceException("内置管理员角色不允许删除");
        }
        // 查询角色是否存在：逻辑删除场景下 deleteById 不会报错，必须显式校验
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        // 第二重保护：基于 roleKey 的语义判断，作为防御纵深的二重校验
        if ("admin".equals(role.getRoleKey())) {
            throw new ServiceException("内置管理员角色不允许删除");
        }
        // 逻辑删除角色本身（BaseEntity 中配置了逻辑删除字段，MP 自动处理）
        sysRoleMapper.deleteById(roleId);
        // 级联物理删除关联表数据（关联表无逻辑删除，直接物理清除）
        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, roleId)
        );
        sysRoleMenuMapper.delete(
                new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId)
        );
    }

    /**
     * 为角色分配菜单权限
     * <p>
     * 采用"先删后增"策略：
     * - 先删除该角色所有旧的菜单关联，确保不会出现残留的过期授权
     * - 再批量插入新的菜单关联，实现权限的完整替换
     * 这种方式实现简单且语义清晰，适用于"全量覆盖"式的权限分配场景。
     * </p>
     *
     * @param roleId  角色ID
     * @param menuIds 要分配的菜单ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        // 校验目标角色存在性：防止为一个不存在的 roleId 插入关联数据，
        // 这些孤立记录会干扰后续的权限查询逻辑
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        // 对 menuIds 做去重处理：前端可能传入重复 ID，
        // 若不去重，selectBatchIds 会自动去重导致 size 不匹配而误判为"部分菜单不存在"
        if (menuIds == null) {
            menuIds = List.of();
        }
        List<Long> distinctMenuIds = menuIds.stream().distinct().toList();
        // 校验菜单 ID 全部有效：如果前端传入了不存在的 menuId，
        // 会导致关联表中出现无效引用，权限分配结果与用户预期不符
        if (!distinctMenuIds.isEmpty()) {
            List<SysMenu> menus = sysMenuMapper.selectBatchIds(distinctMenuIds);
            if (menus.size() != distinctMenuIds.size()) {
                throw new ServiceException("部分菜单不存在");
            }
        }
        // 清除旧的角色-菜单关联
        sysRoleMenuMapper.delete(
                new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId)
        );
        // 批量插入新的角色-菜单关联
        if (!distinctMenuIds.isEmpty()) {
            for (Long menuId : distinctMenuIds) {
                SysRoleMenu roleMenu = new SysRoleMenu();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                sysRoleMenuMapper.insert(roleMenu);
            }
        }
    }

    // ==================== 内部工具方法 ====================

    /**
     * SysRole 实体 → SysRoleVo 视图对象转换
     */
    private SysRoleVo toVo(SysRole role) {
        SysRoleVo vo = new SysRoleVo();
        vo.setRoleId(role.getRoleId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleKey(role.getRoleKey());
        vo.setStatus(role.getStatus());
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }
}
