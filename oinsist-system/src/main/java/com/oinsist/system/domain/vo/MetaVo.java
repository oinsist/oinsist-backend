package com.oinsist.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 路由元信息 VO
 * <p>
 * 前端侧边栏和面包屑等组件从 meta 中读取标题、图标等展示信息。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaVo {

    /** 菜单标题（显示在侧边栏和面包屑中） */
    private String title;

    /** 菜单图标 */
    private String icon;

    /** 是否不缓存（true=不缓存，每次进入都重新加载） */
    private Boolean noCache;

    /** 是否隐藏路由（true=隐藏，不在侧边栏显示） */
    private Boolean hidden;
}
