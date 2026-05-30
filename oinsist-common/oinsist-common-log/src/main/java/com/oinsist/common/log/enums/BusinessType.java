package com.oinsist.common.log.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务操作类型枚举
 * <p>
 * 用于 {@code @OperLog} 注解，标识当前操作的业务语义。
 * 每个枚举项携带固定的 {@code code} 值用于数据库持久化，
 * 不依赖 ordinal()，避免枚举顺序调整导致历史日志语义污染。
 */
@Getter
@AllArgsConstructor
public enum BusinessType {

    /** 其他 */
    OTHER(0),

    /** 新增 */
    INSERT(1),

    /** 修改 */
    UPDATE(2),

    /** 删除 */
    DELETE(3),

    /** 导出 */
    EXPORT(4),

    /** 导入 */
    IMPORT(5),

    /** 授权（如分配角色、分配权限） */
    GRANT(6),

    /** 强制退出 */
    FORCE_LOGOUT(7);

    /**
     * 持久化编码（固定值，不随枚举顺序变化）
     */
    private final int code;
}
