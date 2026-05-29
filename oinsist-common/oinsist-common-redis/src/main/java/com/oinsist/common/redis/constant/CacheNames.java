package com.oinsist.common.redis.constant;

/**
 * 缓存名称常量集中管理。
 * <p>
 * 将所有 {@code @Cacheable} / {@code @CacheEvict} 等注解中使用的缓存名称
 * 统一定义在此处，避免硬编码字符串散落在各业务 Service 中，
 * 从而防止因拼写不一致导致的 key 冲突和后期维护困难。
 * </p>
 * <p>
 * 使用方式示例：{@code @Cacheable(cacheNames = CacheNames.SYS_CONFIG, key = "#configKey")}
 * </p>
 *
 * @author oinsist
 */
public final class CacheNames {

    private CacheNames() {
        // 工具类禁止实例化
    }

    /** 默认缓存（兜底，当业务未指定专属缓存名时使用） */
    public static final String DEFAULT = "default";

    /** 系统配置缓存（sys_config 表数据） */
    public static final String SYS_CONFIG = "sys_config";

    /** 系统字典缓存（字典类型 + 字典数据） */
    public static final String SYS_DICT = "sys_dict";
}
