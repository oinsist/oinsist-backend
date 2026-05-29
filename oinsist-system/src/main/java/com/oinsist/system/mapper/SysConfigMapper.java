package com.oinsist.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oinsist.system.domain.SysConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统配置 Mapper 接口。
 * <p>
 * 继承 BaseMapper 即可获得 CRUD 能力，无需手写 SQL。
 * </p>
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
}
