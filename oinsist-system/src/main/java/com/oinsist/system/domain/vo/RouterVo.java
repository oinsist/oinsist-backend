package com.oinsist.system.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 动态路由节点 VO（/auth/routers 接口返回）
 * <p>
 * 对应前端路由配置结构，前端接收后直接注册为 Vue Router 路由。
 * </p>
 */
@Data
public class RouterVo {

    /** 路由名称（对应前端路由 name，建议与 path 保持一致） */
    private String name;

    /** 路由地址 */
    private String path;

    /** 组件路径（如 "system/user/index"） */
    private String component;

    /** 路由元信息 */
    private MetaVo meta;

    /** 是否隐藏路由（true=不在侧边栏显示） */
    private Boolean hidden;

    /** 子路由 */
    private List<RouterVo> children;
}
