package com.oinsist.common.mybatis.domain;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * 分页查询的通用入参封装
 * <p>
 * Controller 层接收前端传入的分页参数后，通过 {@link #buildPage()} 方法转换为
 * MyBatis-Plus 的 {@link Page} 分页对象，供 Service/Mapper 层直接使用。
 * <p>
 * 为什么放在 common-mybatis 而非 common-core：
 * {@code buildPage()} 方法直接依赖 MyBatis-Plus 的 {@link Page} 类，属于持久层工具，
 * 若放置在 common-core 会引入 MyBatis-Plus 依赖，污染无技术依赖的核心公共层，
 * 违反"common-core 不得依赖具体技术栈"的模块职责边界。
 */
@Data
public class PageQuery {

    /**
     * 每页最大允许条数（防止前端传入过大值导致全表扫描）
     */
    private static final int MAX_PAGE_SIZE = 500;

    /**
     * 当前页码（从 1 开始），默认第 1 页
     */
    private Integer pageNum = 1;

    /**
     * 每页条数，默认 10 条
     */
    private Integer pageSize = 10;

    /**
     * 构建 MyBatis-Plus 分页对象
     * <p>
     * 在构建前对分页参数进行安全校验：
     * - pageNum < 1 时重置为 1，防止无效页码导致查询异常
     * - pageSize < 1 时重置为 10，保证至少返回有效数据
     * - pageSize > MAX_PAGE_SIZE 时重置为 MAX_PAGE_SIZE（500），
     *   防止前端恶意或误传过大值导致单次查询加载海量数据，引发全表扫描和内存溢出
     *
     * @param <T> 分页记录的实体类型
     * @return MyBatis-Plus 分页对象，可直接传入 Mapper 的 selectPage 方法
     */
    public <T> Page<T> buildPage() {
        // 页码安全校验：不允许小于 1
        int validPageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        // 每页条数安全校验：不允许小于 1，不允许超过最大限制
        int validPageSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        if (validPageSize > MAX_PAGE_SIZE) {
            validPageSize = MAX_PAGE_SIZE;
        }
        return new Page<>(validPageNum, validPageSize);
    }
}
