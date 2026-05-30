package com.oinsist.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据范围枚举
 * <p>
 * 用于 RBAC 数据权限控制，定义角色可访问的数据边界。
 * 配合数据权限拦截器，在 SQL 层面自动追加过滤条件，
 * 实现不同角色看到不同范围的业务数据。
 * </p>
 */
@Getter
@AllArgsConstructor
public enum DataScopeEnum {

    /** 全部数据权限 —— 不做任何数据过滤，可查看所有数据 */
    ALL("ALL", "全部数据权限"),

    /** 自定义部门数据权限 —— 通过角色关联的部门列表来限定可见范围 */
    CUSTOM("CUSTOM", "自定义部门数据权限"),

    /** 本部门数据权限 —— 仅能查看当前用户所属部门的数据 */
    DEPT("DEPT", "本部门数据权限"),

    /** 本部门及以下数据权限 —— 可查看当前部门及其所有子部门的数据 */
    DEPT_AND_CHILD("DEPT_AND_CHILD", "本部门及以下数据权限"),

    /** 仅本人数据权限 —— 只能查看自己创建的数据 */
    SELF("SELF", "仅本人数据权限");

    /** 数据范围编码，对应数据库 VARCHAR(20) 存储 */
    private final String code;

    /** 数据范围描述 */
    private final String desc;

    /**
     * 根据 code 值查找对应的数据范围枚举
     *
     * @param code 数据范围编码
     * @return 匹配的枚举实例
     * @throws IllegalArgumentException 当 code 无法匹配任何枚举值时抛出
     */
    public static DataScopeEnum fromCode(String code) {
        for (DataScopeEnum scope : values()) {
            if (scope.code.equals(code)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("未知的数据范围编码: " + code);
    }
}
