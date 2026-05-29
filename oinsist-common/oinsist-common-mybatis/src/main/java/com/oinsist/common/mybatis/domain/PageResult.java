package com.oinsist.common.mybatis.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页查询通用响应封装
 * <p>
 * 参考 RuoYi-Vue-Plus 的 TableDataInfo 思想，但遵循极简原则，
 * 只保留前端渲染分页列表所必需的 rows（数据列表）和 total（总记录数）。
 * <p>
 * 前端发起分页请求时已携带 pageNum/pageSize，无需在响应中重复返回分页参数。
 * <p>
 * 使用方式：作为 {@code R<PageResult<T>>} 的 data 字段返回，
 * Controller 中典型用法：{@code return R.ok(PageResult.build(page));}
 *
 * @param <T> 数据记录的实体类型
 */
@Data
public class PageResult<T> {

    /** 当前页数据列表 */
    private List<T> rows;

    /** 总记录数 */
    private long total;

    /**
     * 从 MyBatis-Plus 的 IPage 分页结果构建 PageResult
     * <p>
     * 将 MP 分页查询返回的 IPage 对象转换为前端友好的精简分页响应，
     * 提取其中的 records（数据列表）和 total（总条数）。
     *
     * @param page MyBatis-Plus 分页查询结果
     * @param <T>  数据记录的实体类型
     * @return 封装后的分页响应对象
     */
    public static <T> PageResult<T> build(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRows(page.getRecords());
        result.setTotal(page.getTotal());
        return result;
    }
}
