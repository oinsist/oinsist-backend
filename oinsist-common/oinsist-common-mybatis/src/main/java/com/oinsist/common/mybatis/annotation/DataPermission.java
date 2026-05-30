package com.oinsist.common.mybatis.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * <p>
 * 标记在 Mapper 的查询方法上，表明该方法需要进行数据范围过滤。
 * 由 MyBatis-Plus 的 DataPermissionInterceptor 在 SQL 执行前拦截，
 * 根据当前用户的角色数据范围，自动追加 WHERE 条件，避免权限逻辑散落到 Service 层。
 * </p>
 *
 * <h3>设计思路</h3>
 * <ul>
 *     <li>注解只声明"哪些列参与权限过滤"，不包含具体过滤逻辑——职责分离</li>
 *     <li>支持表别名配置，兼容多表 JOIN 的复杂查询场景</li>
 *     <li>默认值覆盖最常见的单表场景（dept_id / create_by），减少使用时的模板代码</li>
 * </ul>
 *
 * @author oinsist
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 部门字段所在表的别名
     * <p>
     * 多表 JOIN 时需要指定表别名以避免列名歧义，
     * 例如 {@code SELECT u.* FROM sys_user u LEFT JOIN sys_dept d ...}
     * 此时设置 deptAlias = "u" 会生成条件 {@code u.dept_id IN (...)}
     * </p>
     */
    String deptAlias() default "";

    /**
     * 部门字段列名（默认 dept_id）
     * <p>用于 DEPT / DEPT_AND_CHILD / CUSTOM 范围过滤</p>
     */
    String deptIdColumn() default "dept_id";

    /**
     * 用户字段所在表的别名
     * <p>与 deptAlias 类似，用于多表 JOIN 场景下指定 create_by 列所属的表</p>
     */
    String userAlias() default "";

    /**
     * 用户字段列名（默认 create_by）
     * <p>用于 SELF 范围过滤，匹配当前登录用户ID</p>
     */
    String userIdColumn() default "create_by";
}
