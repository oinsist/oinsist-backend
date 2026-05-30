package com.oinsist.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oinsist.system.domain.SysUser;
import com.oinsist.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户查询服务
 * <p>
 * 当前仅提供登录链路所需的"根据用户名查询"能力。
 * 后续 CRUD 管理接口将在此扩展。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;

    /**
     * 根据用户名查询用户（仅查未删除的记录）
     *
     * @param username 用户账号
     * @return SysUser 或 null
     */
    public SysUser selectByUsername(String username) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
    }
}
