package com.oinsist.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oinsist.common.core.exception.ServiceException;
import com.oinsist.common.mybatis.domain.PageQuery;
import com.oinsist.common.mybatis.domain.PageResult;
import com.oinsist.system.domain.SysRole;
import com.oinsist.system.domain.SysUser;
import com.oinsist.system.domain.SysUserRole;
import com.oinsist.system.domain.dto.SysUserAddDto;
import com.oinsist.system.domain.dto.SysUserEditDto;
import com.oinsist.system.domain.vo.SysUserVo;
import com.oinsist.system.mapper.SysRoleMapper;
import com.oinsist.system.mapper.SysUserMapper;
import com.oinsist.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户管理服务
 * <p>
 * 提供用户 CRUD 和角色分配等核心业务能力。
 * 参考 RuoYi-Vue-Plus 的用户管理思路，保留核心保护逻辑（如内置管理员不可删除、
 * 超级管理员角色不可移除），同时精简冗余分支，保持代码可读性。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;

    /** BCrypt 密码编码器，线程安全，声明为静态常量避免重复创建 */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 根据用户名查询用户（仅查未删除的记录）
     *
     * @param username 用户账号
     * @return SysUser 或 null
     */
    public SysUser selectByUsername(String username) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
    }

    /**
     * 分页查询用户列表
     * <p>
     * 查询结果转换为 SysUserVo，排除密码等敏感字段，遵循最小暴露原则。
     * </p>
     *
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    public PageResult<SysUserVo> listUsers(PageQuery pageQuery) {
        // 调用自定义的 selectUserPage 方法，触发 @DataPermission 数据权限过滤
        Page<SysUser> page = sysUserMapper.selectUserPage(pageQuery.buildPage(), new LambdaQueryWrapper<>());
        // 将实体转换为 VO，排除密码字段
        Page<SysUserVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<SysUserVo> voList = page.getRecords().stream().map(this::toVo).toList();
        voPage.setRecords(voList);
        return PageResult.build(voPage);
    }

    /**
     * 根据 ID 查询单个用户
     *
     * @param userId 用户ID
     * @return 用户 VO
     */
    public SysUserVo selectById(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return toVo(user);
    }

    /**
     * 查询用户当前已分配的角色ID集合
     * <p>
     * 该接口服务于前端"分配角色"弹窗的预勾选场景，只读取 sys_user_role 关联表，
     * 不额外联查角色详情，保持查询路径简单清晰。
     * </p>
     *
     * @param userId 用户ID
     * @return 当前用户已绑定的角色ID集合
     */
    public List<Long> listRoleIdsByUserId(Long userId) {
        return sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .select(SysUserRole::getRoleId)
                        .eq(SysUserRole::getUserId, userId)
        ).stream().map(SysUserRole::getRoleId).toList();
    }

    /**
     * 新增用户
     * <p>
     * 业务规则：
     * 1. 用户名全局唯一，重复则拒绝创建；
     * 2. 密码使用 BCrypt 加密存储，防止明文泄漏。
     * </p>
     *
     * @param dto 新增用户请求
     */
    public void addUser(SysUserAddDto dto) {
        // 校验用户名唯一性
        SysUser existing = selectByUsername(dto.getUsername());
        if (existing != null) {
            throw new ServiceException("用户名已存在");
        }
        // 构建实体并加密密码
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setPassword(PASSWORD_ENCODER.encode(dto.getPassword()));
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : "0");
        sysUserMapper.insert(user);
    }

    /**
     * 编辑用户
     * <p>
     * 仅允许修改昵称和状态，用户名和密码不可通过此接口变更。
     * 更新前校验用户是否存在，防止对不存在的 ID 执行静默成功的无效更新。
     * </p>
     *
     * @param dto 编辑用户请求
     */
    public void editUser(SysUserEditDto dto) {
        // 校验用户存在性，避免 updateById 对不存在记录静默成功
        SysUser existing = sysUserMapper.selectById(dto.getUserId());
        if (existing == null) {
            throw new ServiceException("用户不存在");
        }
        SysUser user = new SysUser();
        user.setUserId(dto.getUserId());
        user.setNickname(dto.getNickname());
        user.setStatus(dto.getStatus());
        sysUserMapper.updateById(user);
    }

    /**
     * 删除用户（逻辑删除）
     * <p>
     * 核心保护：内置管理员（userId=1）不允许删除，防止系统失去超级管理入口。
     * 删除用户时同步清理 sys_user_role 关联数据，避免孤立授权数据残留。
     * </p>
     *
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        // 内置管理员保护：ID=1 的用户为系统初始化创建的超级管理员，不可删除
        if (userId == 1L) {
            throw new ServiceException("内置管理员用户不允许删除");
        }
        // 校验用户存在性，防止对不存在的用户执行无意义的删除操作
        SysUser existing = sysUserMapper.selectById(userId);
        if (existing == null) {
            throw new ServiceException("用户不存在");
        }
        // 逻辑删除用户记录
        sysUserMapper.deleteById(userId);
        // 物理删除 sys_user_role 中该用户的所有关联，避免孤立授权数据残留
        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
        );
    }

    /**
     * 为用户分配角色（先删后增策略）
     * <p>
     * 事务保证：删除旧关联和插入新关联必须在同一事务中完成，
     * 防止中途失败导致用户丧失所有角色权限。
     * <p>
     * 核心保护：超级管理员（userId=1）的 admin 角色不可移除，
     * 防止管理员被误操作降权导致系统无法管理。
     * </p>
     *
     * @param userId  用户ID
     * @param roleIds 新的角色ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 空值规范化：null 视为空列表，允许清空普通用户角色
        if (roleIds == null) {
            roleIds = List.of();
        }
        // 去重处理：防止重复 ID 导致 selectBatchIds 结果数量不匹配被误判为"角色不存在"
        List<Long> distinctRoleIds = roleIds.stream().distinct().toList();

        // 校验目标用户存在性，防止给不存在或已删除的用户分配角色
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        // 校验角色列表中的所有角色均存在，防止分配无效角色
        if (!distinctRoleIds.isEmpty()) {
            List<SysRole> existingRoles = sysRoleMapper.selectBatchIds(distinctRoleIds);
            if (existingRoles.size() != distinctRoleIds.size()) {
                throw new ServiceException("部分角色不存在");
            }
        }
        // 超级管理员角色保护
        if (userId == 1L) {
            // 查询 admin 角色的 roleId
            SysRole adminRole = sysRoleMapper.selectOne(
                    new LambdaQueryWrapper<SysRole>()
                            .eq(SysRole::getRoleKey, "admin")
            );
            if (adminRole != null && !distinctRoleIds.contains(adminRole.getRoleId())) {
                throw new ServiceException("不允许移除超级管理员的管理员角色");
            }
        }

        // 先删除该用户的所有旧角色关联
        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
        );

        // 再批量插入新的角色关联（使用去重后的列表，避免重复插入）
        for (Long roleId : distinctRoleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        }
    }

    /**
     * 实体转 VO（排除密码等敏感字段）
     */
    private SysUserVo toVo(SysUser user) {
        SysUserVo vo = new SysUserVo();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
