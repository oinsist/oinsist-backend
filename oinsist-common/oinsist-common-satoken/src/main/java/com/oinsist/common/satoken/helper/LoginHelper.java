package com.oinsist.common.satoken.helper;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.oinsist.common.core.enums.ResultCode;
import com.oinsist.common.core.exception.ServiceException;
import com.oinsist.common.satoken.domain.LoginUser;
import com.oinsist.common.satoken.domain.TokenInfo;

/**
 * 登录上下文操作工具类
 * <p>
 * 为什么要封装 LoginHelper 而不直接在业务代码中调用 StpUtil？
 * <ol>
 *     <li><b>解耦认证框架</b>：业务层只依赖 LoginHelper，不直接耦合 Sa-Token API。
 *         未来若需替换认证框架（如迁移到 Spring Security），只需修改本类内部实现，
 *         业务代码零改动。</li>
 *     <li><b>统一 Session 存取规范</b>：LoginUser 在 Session 中的 Key、类型转换、
 *         null 处理等逻辑集中在此，避免各处业务代码重复编写且风格不一致。</li>
 *     <li><b>语义清晰</b>：{@code LoginHelper.getUserId()} 比
 *         {@code StpUtil.getLoginIdAsLong()} 更具业务语义，代码可读性更强。</li>
 * </ol>
 * </p>
 */
public final class LoginHelper {

    /**
     * Sa-Token Session 中存储 LoginUser 的 Key
     * <p>
     * 使用固定常量避免魔法字符串散落在各处，
     * 同时保证存取时 Key 一致性，杜绝因拼写错误导致的空指针。
     * </p>
     */
    private static final String LOGIN_USER_KEY = "loginUser";

    /**
     * 私有构造器，禁止实例化
     */
    private LoginHelper() {
    }

    /**
     * 执行登录并返回 Token 信息
     * <p>
     * 流程：
     * 1. 调用 StpUtil.login() 建立登录态，Sa-Token 会生成 Token 并关联到 userId；
     * 2. 将 LoginUser 完整信息写入该用户的 Session，后续请求可随时取用；
     * 3. 封装 Token 元数据返回，屏蔽 Sa-Token API 细节，业务层无需直接调用 StpUtil。
     * </p>
     *
     * @param loginUser 登录用户信息（由认证服务校验通过后构建）
     * @return TokenInfo 包含 tokenName 和 tokenValue
     */
    public static TokenInfo login(LoginUser loginUser) {
        StpUtil.login(loginUser.getUserId());
        StpUtil.getSession().set(LOGIN_USER_KEY, loginUser);

        // 封装 Token 信息返回，屏蔽 Sa-Token API 细节
        SaTokenInfo saTokenInfo = StpUtil.getTokenInfo();
        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setTokenName(saTokenInfo.getTokenName());
        tokenInfo.setTokenValue(saTokenInfo.getTokenValue());
        return tokenInfo;
    }

    /**
     * 执行退出操作
     * <p>
     * Sa-Token 会自动清除该用户的 Token 和 Session，无需手动删除。
     * </p>
     */
    public static void logout() {
        StpUtil.logout();
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 用户 ID（Long 类型，雪花算法生成）
     */
    public static Long getUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 获取当前登录用户的完整上下文信息
     * <p>
     * 从 Sa-Token Session 中取出登录时写入的 LoginUser 对象。
     * 若 Session 中不存在，则说明登录态异常，直接抛出未认证异常，
     * 避免返回 null 导致上层出现静默空指针。
     * </p>
     *
     * @return LoginUser 登录用户上下文
     * @throws ServiceException 当 Session 中无法获取 LoginUser 时抛出 UNAUTHORIZED 异常
     */
    public static LoginUser getLoginUser() {
        SaSession session = StpUtil.getSession();
        Object obj = session.get(LOGIN_USER_KEY);
        if (obj == null) {
            throw new ServiceException(ResultCode.UNAUTHORIZED);
        }
        return (LoginUser) obj;
    }

    /**
     * 安全获取当前登录用户信息（不抛异常）
     * <p>
     * 未登录时返回 null，适用于租户提供者等非强制登录场景。
     * 与 getLoginUser() 的区别：
     * - getLoginUser()：强制要求登录态，用于业务接口等必须认证的场景
     * - getLoginUserOrNull()：容忍未登录，用于拦截器/提供者等可能在登录前执行的场景
     * </p>
     *
     * @return LoginUser 登录用户上下文，未登录时返回 null
     */
    public static LoginUser getLoginUserOrNull() {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            return getLoginUser();
        } catch (Exception e) {
            return null;
        }
    }
}
