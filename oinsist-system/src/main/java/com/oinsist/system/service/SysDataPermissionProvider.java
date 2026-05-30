package com.oinsist.system.service;

import com.oinsist.common.core.enums.DataScopeEnum;
import com.oinsist.common.mybatis.datapermission.DataPermissionProvider;
import com.oinsist.common.satoken.domain.LoginUser;
import com.oinsist.common.satoken.helper.LoginHelper;
import com.oinsist.system.domain.SysRole;
import com.oinsist.system.mapper.SysDeptMapper;
import com.oinsist.system.mapper.SysRoleDeptMapper;
import com.oinsist.system.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据权限上下文提供者实现
 * <p>
 * 核心职责：根据当前登录用户的角色集合，合并计算其可访问的数据范围。
 * 本类是 common-mybatis 中 {@link DataPermissionProvider} 接口的唯一实现，
 * 通过 Spring DI 注入到数据权限拦截器，实现"基础设施定义接口、业务层提供实现"的依赖反转。
 * <p>
 * <b>多角色合并策略：</b>
 * <ol>
 *     <li>遍历用户所有角色的 data_scope</li>
 *     <li>若任一角色为 ALL → 直接放行全部数据（hasAllDataScope = true）</li>
 *     <li>否则取各角色可访问部门的<b>并集</b>（不是交集，避免缩小权限）：
 *         <ul>
 *             <li>DEPT → 加入用户本部门</li>
 *             <li>DEPT_AND_CHILD → 加入用户本部门 + 递归查询所有子部门</li>
 *             <li>CUSTOM → 加入该角色关联的自定义部门集合（sys_role_dept）</li>
 *             <li>SELF → 不加入任何部门（仅按 create_by 过滤）</li>
 *         </ul>
 *     </li>
 *     <li>若所有角色均为 SELF，则 isSelfScopeOnly() 返回 true，
 *         Handler 会使用 create_by = 当前用户ID 进行过滤</li>
 * </ol>
 * <p>
 * <b>防御策略：</b>
 * 本类所有方法均对未登录场景做 try-catch 防御，因为 LoginHelper.getLoginUser()
 * 在未登录时会抛出异常。当系统初始化或非认证请求触发 SQL 执行时，
 * 返回 null/空集/false，让 Handler 跳过数据权限过滤，保证程序不中断。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SysDataPermissionProvider implements DataPermissionProvider {

    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleDeptMapper roleDeptMapper;

    @Override
    public Long getCurrentUserId() {
        try {
            return LoginHelper.getUserId();
        } catch (Exception e) {
            // 未登录场景下返回 null，让 Handler 跳过数据权限过滤
            return null;
        }
    }

    @Override
    public Long getCurrentDeptId() {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            return loginUser.getDeptId();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean hasAllDataScope() {
        List<SysRole> roles = getUserRoles();
        return roles.stream()
            .anyMatch(r -> DataScopeEnum.ALL.getCode().equals(r.getDataScope()));
    }

    @Override
    public Set<Long> getAccessibleDeptIds() {
        List<SysRole> roles = getUserRoles();
        Long currentDeptId = getCurrentDeptId();
        Set<Long> deptIds = new HashSet<>();

        for (SysRole role : roles) {
            String scope = role.getDataScope();
            DataScopeEnum scopeEnum = DataScopeEnum.fromCode(scope);

            switch (scopeEnum) {
                case ALL -> {
                    // ALL 不应走到这里（hasAllDataScope 会先拦截），防御性返回空集
                    return Set.of();
                }
                case DEPT -> {
                    if (currentDeptId != null) {
                        deptIds.add(currentDeptId);
                    }
                }
                case DEPT_AND_CHILD -> {
                    if (currentDeptId != null) {
                        deptIds.add(currentDeptId);
                        Set<Long> childIds = deptMapper.selectChildDeptIds(currentDeptId);
                        if (childIds != null) {
                            deptIds.addAll(childIds);
                        }
                    }
                }
                case CUSTOM -> {
                    Set<Long> customDeptIds = roleDeptMapper.selectDeptIdsByRoleId(role.getRoleId());
                    if (customDeptIds != null) {
                        deptIds.addAll(customDeptIds);
                    }
                }
                case SELF -> {
                    // SELF 不加入部门，后续由 Handler 使用 create_by = userId 过滤
                }
            }
        }

        return deptIds;
    }

    @Override
    public boolean isSelfScopeOnly() {
        List<SysRole> roles = getUserRoles();
        // 无角色时视为无权限，按 SELF 处理
        if (roles.isEmpty()) {
            return true;
        }
        return roles.stream()
            .allMatch(r -> DataScopeEnum.SELF.getCode().equals(r.getDataScope()));
    }

    @Override
    public boolean hasSelfScope() {
        List<SysRole> roles = getUserRoles();
        // 至少一个角色的 data_scope 为 SELF，即 SELF 权限参与并集合并
        return roles.stream()
            .anyMatch(r -> DataScopeEnum.SELF.getCode().equals(r.getDataScope()));
    }

    /**
     * 获取当前用户的所有有效角色（含 dataScope 字段）
     * <p>
     * 查询逻辑：通过 LoginUser 中缓存的 roleKeys 反查 sys_role 表，
     * 获取角色详细信息（主要是 dataScope 字段）。
     * <p>
     * 注意：BaseEntity 中 deleted 字段已标注 @TableLogic，
     * MyBatis-Plus 会自动追加 WHERE deleted = 0 条件，无需手动指定。
     * 此外，额外过滤 status='0' 确保只取启用状态的角色。
     * </p>
     */
    private List<SysRole> getUserRoles() {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            Set<String> roleKeys = loginUser.getRoleKeys();
            if (roleKeys == null || roleKeys.isEmpty()) {
                return List.of();
            }
            // 通过 roleKey 集合查询角色详细信息，MP 自动追加 deleted=0 条件
            return roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                    .in(SysRole::getRoleKey, roleKeys)
                    .eq(SysRole::getStatus, "0")
            );
        } catch (Exception e) {
            // 未登录场景下返回空集合
            return List.of();
        }
    }
}
