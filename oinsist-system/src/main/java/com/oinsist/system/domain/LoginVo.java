package com.oinsist.system.domain;

import lombok.Data;

/**
 * 登录成功响应信息
 */
@Data
public class LoginVo {

    /**
     * Token 名称（即 Header 中的参数名，如 "Authorization"）
     */
    private String tokenName;

    /**
     * Token 值，前端使用时按 tokenName 作为 Header 名传入此值
     */
    private String tokenValue;
}
