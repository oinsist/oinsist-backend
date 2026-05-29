package com.oinsist.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.oinsist.common.core.domain.R;
import com.oinsist.common.mybatis.domain.PageQuery;
import com.oinsist.common.mybatis.domain.PageResult;
import com.oinsist.system.domain.SysConfig;
import com.oinsist.system.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * 系统配置测试接口（P03 验证用）。
 * <p>
 * 本 Controller 用于验证 MyBatis-Plus 三大核心能力：
 * <ul>
 *     <li>自动填充：新增时自动写入 createTime、updateTime</li>
 *     <li>分页查询：配合 PaginationInnerInterceptor 实现物理分页</li>
 *     <li>逻辑删除：DELETE 操作转化为 UPDATE SET deleted=1，查询自动过滤已删除数据</li>
 * </ul>
 * P03 验证阶段暂不创建 Service 层，直接注入 Mapper 使用。
 * </p>
 */
@Profile("dev") // 测试接口仅在开发环境下注册，生产环境不暴露，避免敏感数据操作被外部访问
@RestController
@RequestMapping("/test/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigMapper sysConfigMapper;

    /**
     * 新增配置（验证自动填充）
     * <p>
     * 请求体只需传入 configName、configKey、configValue，
     * createTime 和 updateTime 由 MybatisMetaObjectHandler 自动填充。
     * </p>
     */
    @PostMapping
    public R<SysConfig> add(@RequestBody SysConfig sysConfig) {
        sysConfigMapper.insert(sysConfig);
        return R.ok(sysConfig);
    }

    /**
     * 分页查询配置列表（验证分页 + 逻辑删除自动过滤）
     * <p>
     * MP 分页插件会自动改写 SQL 为 COUNT + LIMIT/OFFSET，
     * 逻辑删除拦截器会自动追加 WHERE deleted = 0 条件。
     * </p>
     */
    @GetMapping("/list")
    public R<PageResult<SysConfig>> list(PageQuery pageQuery) {
        IPage<SysConfig> page = sysConfigMapper.selectPage(pageQuery.buildPage(), null);
        return R.ok(PageResult.build(page));
    }

    /**
     * 逻辑删除配置（验证 delete 被改写为 update deleted=1）
     * <p>
     * 调用 deleteById 后，MP 不会执行 DELETE 语句，
     * 而是执行 UPDATE sys_config SET deleted=1 WHERE config_id=? AND deleted=0。
     * </p>
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        sysConfigMapper.deleteById(id);
        return R.ok();
    }
}
