package com.oinsist.common.core.constant;

/**
 * 全局基础常量。
 *
 * <p>这个类只保存"跨模块都能理解、且不包含业务规则"的基础常量，例如字符集、
 * 通用分隔符、布尔状态值等。这样设计的原因是：常量一旦散落在各个模块中，
 * 后续在 Controller、Service、拦截器、缓存 Key 等位置很容易出现重复字面量，
 * 最终导致修改成本高、含义不统一。</p>
 *
 * <p>注意：这里暂时不放统一响应码、接口返回对象等内容。统一响应属于 P02 的 Web
 * 契约设计，应当在异常处理和响应规范一起建立，避免 P01 阶段过早绑定接口协议。</p>
 */
public final class Constants {

    /**
     * 系统默认字符集。
     *
     * <p>后续处理请求体、文件导入导出、签名摘要时都需要明确字符集。
     * 统一使用 UTF-8 可以避免不同操作系统默认编码不一致导致的中文乱码问题。</p>
     */
    public static final String UTF8 = "UTF-8";

    /**
     * 通用成功标记。
     *
     * <p>使用字符串形式而不是 boolean，是为了兼容数据库字段、前端字典、缓存值等场景。
     * 这里仅表达"启用/成功"这个基础语义，不承载具体业务含义。</p>
     */
    public static final String YES = "Y";

    /**
     * 通用失败标记。
     *
     * <p>与 {@link #YES} 配套使用，保证简单开关类字段在各模块中的取值保持一致。</p>
     */
    public static final String NO = "N";

    /**
     * 根路径标识。
     *
     * <p>后续在菜单、路由、文件路径等场景都会用到根路径。抽成常量可以避免不同模块
     * 对根路径写法不一致，例如空字符串、斜杠、null 混用。</p>
     */
    public static final String ROOT_PATH = "/";

    /**
     * 常用分隔符。
     *
     * <p>分隔符经常出现在权限标识、缓存 Key、批量参数拼接中。集中定义后，
     * 能减少硬编码，也便于后续统一调整命名规范。</p>
     */
    public static final String COMMA = ",";
    public static final String COLON = ":";
    public static final String UNDERLINE = "_";

    /**
     * 工具常量类不允许实例化。
     *
     * <p>常量类只提供静态字段，如果允许 new 出对象，会让调用方误以为它存在状态或行为。
     * 私有构造方法可以明确表达"这是一个纯静态常量容器"。</p>
     */
    private Constants() {
    }
}
