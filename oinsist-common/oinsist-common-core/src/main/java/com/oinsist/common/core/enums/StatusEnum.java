package com.oinsist.common.core.enums;

import java.util.Arrays;

/**
 * 通用状态枚举。
 *
 * <p>后台系统中大量数据都会有"正常/停用"这类基础状态，例如用户、角色、菜单、岗位等。
 * 如果每个实体都各自定义 0 和 1 的含义，时间久了就会出现状态值含义不一致的问题。
 * 抽成枚举后，代码中可以使用有语义的 {@code StatusEnum.NORMAL}，
 * 而不是直接写魔法数字，阅读和维护成本都会更低。</p>
 *
 * <p>这个枚举只表达最基础的启停状态，不绑定任何具体业务流程。
 * 例如"审核中、已驳回、已删除"这类业务状态不应该放在这里，
 * 否则 common-core 会被业务概念污染，破坏多模块的依赖边界。</p>
 */
public enum StatusEnum {

    /**
     * 正常状态。
     *
     * <p>通常表示数据可以被系统正常读取、使用或展示。</p>
     */
    NORMAL(0, "正常"),

    /**
     * 停用状态。
     *
     * <p>通常表示数据仍然保留，但暂时不参与业务流程。它不同于物理删除，
     * 后续权限、菜单、用户等模块可以基于该状态做启停控制。</p>
     */
    DISABLED(1, "停用");

    private final int code;

    private final String description;

    StatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取数据库或接口传输时使用的状态值。
     *
     * <p>状态值使用 int，方便后续与 PostgreSQL smallint / integer 字段映射，
     * 也便于前端字典直接展示。</p>
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取状态中文描述。
     *
     * <p>描述只用于日志、调试或简单展示。真正的前端国际化不应依赖这个字段，
     * 后续可以由字典模块统一管理。</p>
     */
    public String getDescription() {
        return description;
    }

    /**
     * 判断当前状态是否可用。
     *
     * <p>封装这个方法是为了避免业务代码到处写 {@code status == 0}。
     * 一旦未来状态值发生调整，只需要修改枚举内部逻辑。</p>
     */
    public boolean isNormal() {
        return this == NORMAL;
    }

    /**
     * 根据状态值反查枚举。
     *
     * <p>数据库、HTTP 参数、缓存中通常保存的是数字状态值，进入 Java 领域后转换为枚举，
     * 可以让后续业务逻辑使用更清晰的语义判断。</p>
     *
     * @param code 状态值
     * @return 匹配的状态枚举
     * @throws IllegalArgumentException 当状态值不在系统约定范围内时抛出，避免非法状态静默流入业务层
     */
    public static StatusEnum fromCode(int code) {
        return Arrays.stream(values())
            .filter(status -> status.code == code)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未知状态值: " + code));
    }
}
