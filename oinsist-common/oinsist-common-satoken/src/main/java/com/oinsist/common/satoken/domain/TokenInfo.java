package com.oinsist.common.satoken.domain;

import lombok.Data;

/**
 * Token 信息
 * <p>
 * 封装登录成功后返回的 Token 元数据。
 * 作为 LoginHelper.login() 的返回值，避免业务层直接依赖 Sa-Token API。
 * </p>
 */
@Data
public class TokenInfo {

    /** Token 名称（即 Header 中的参数名，如 "Authorization"） */
    private String tokenName;

    /** Token 值 */
    private String tokenValue;
}
