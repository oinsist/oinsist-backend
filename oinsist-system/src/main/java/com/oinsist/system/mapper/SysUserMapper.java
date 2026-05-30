package com.oinsist.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oinsist.common.mybatis.annotation.DataPermission;
import com.oinsist.system.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
