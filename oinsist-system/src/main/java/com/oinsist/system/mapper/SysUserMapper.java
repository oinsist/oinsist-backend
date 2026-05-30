package com.oinsist.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oinsist.system.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
