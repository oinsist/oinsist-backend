package com.oinsist.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oinsist.common.mybatis.annotation.DataPermission;
import com.oinsist.system.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper 接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 分页查询用户列表（带数据权限过滤）
     * <p>
     * 使用 @DataPermission 标记此方法需要数据权限拦截。
     * 由于 sys_user 单表查询无需表别名，deptAlias 和 userAlias 留空，
     * 拦截器会直接追加 dept_id IN (...) 或 create_by = ? 条件。
     * </p>
     * <p>
     * 为什么不能直接在 BaseMapper.selectPage 上加注解：
     * DataPermissionHandler 通过 mappedStatementId 反射查找注解，
     * 只能定位到当前 Mapper 接口声明的方法。BaseMapper 的方法属于父接口，
     * 注解无法"覆盖"到父类方法上，因此必须声明自定义方法并映射到 XML SQL。
     * </p>
     *
     * @param page    分页参数（MyBatis-Plus 分页拦截器自动处理 LIMIT/OFFSET）
     * @param wrapper 查询条件构造器（支持后续按用户名、状态等条件动态筛选）
     * @return 分页结果
     */
    @DataPermission(deptIdColumn = "dept_id", userIdColumn = "create_by")
    Page<SysUser> selectUserPage(Page<SysUser> page, @Param("ew") Wrapper<SysUser> wrapper);

    /**
     * 登录专用：根据用户名和租户ID查询用户
     *
     * <p>设计说明：
     * 登录时 Sa-Token Session 尚未建立，TenantProvider 无法获取租户ID，
     * 因此必须使用 @InterceptorIgnore 跳过租户拦截器，在 SQL 中显式指定 tenant_id。</p>
     *
     * <p>安全约束：
     * - 此方法仅限登录流程调用，禁止在其他业务场景使用
     * - SQL 显式带 tenant_id 条件，不存在跨租户泄漏风险</p>
     *
     * @param username 用户账号
     * @param tenantId 租户ID（从登录请求参数中获取）
     * @return 用户实体，不存在则返回 null
     */
    @InterceptorIgnore(tenantLine = "1")
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND tenant_id = #{tenantId} AND deleted = 0")
    SysUser selectByUsernameAndTenantId(@Param("username") String username, @Param("tenantId") Long tenantId);
}
