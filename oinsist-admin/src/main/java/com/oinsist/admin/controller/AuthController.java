package com.oinsist.admin.controller;

import com.oinsist.common.core.domain.R;
import com.oinsist.common.satoken.domain.LoginUser;
import com.oinsist.common.satoken.helper.LoginHelper;
import com.oinsist.system.domain.LoginBody;
import com.oinsist.system.domain.LoginVo;
import com.oinsist.system.domain.vo.RouterVo;
import com.oinsist.system.domain.vo.UserInfoVo;
import com.oinsist.system.service.SysLoginService;
import com.oinsist.system.service.SysMenuService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证控制器
 * <p>
 * 负责用户登录、退出、获取当前用户信息、获取动态路由等认证相关的 HTTP 接口。
 * Controller 层只做 HTTP 协议编排（参数接收、响应封装），
 * 不包含任何业务逻辑，具体认证校验委托给 SysLoginService 处理。
 * </p>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysLoginService sysLoginService;
    private final SysMenuService sysMenuService;

    /**
     * 用户登录
     * <p>
     * 接收用户名和密码，校验通过后生成 Token 返回给前端。
     * 前端后续请求需在 Header 中携带该 Token 以标识身份。
     * Controller 层负责从 HTTP 请求中提取 IP 和 User-Agent，
     * 再传递给 Service 层，保持 Service 不依赖 HTTP 上下文。
     * </p>
     *
     * @param loginBody 登录请求参数（包含 username、password）
     * @param request   HTTP 请求（Spring MVC 自动注入）
     * @return 登录成功返回 tokenName 和 tokenValue
     */
    @PostMapping("/login")
    public R<LoginVo> login(@Valid @RequestBody LoginBody loginBody, HttpServletRequest request) {
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        LoginVo loginVo = sysLoginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getTenantId(), ip, userAgent);
        return R.ok(loginVo);
    }

    /**
     * 用户退出
     * <p>
     * 清除当前用户的登录态，Token 失效后前端需重新登录。
     * </p>
     *
     * @return 退出成功
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        LoginHelper.logout();
        return R.ok();
    }

    /**
     * 获取当前登录用户信息
     * <p>
     * 返回用户基础信息 + 角色标识集合 + 权限标识集合。
     * 前端据此进行页面级别的权限控制（如按钮显示/隐藏）。
     * </p>
     *
     * @return 用户信息（含角色和权限）
     */
    @GetMapping("/userInfo")
    public R<UserInfoVo> userInfo() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        UserInfoVo vo = new UserInfoVo();
        vo.setUserId(loginUser.getUserId());
        vo.setUsername(loginUser.getUsername());
        vo.setNickname(loginUser.getNickname());
        vo.setRoles(loginUser.getRoleKeys());
        vo.setPermissions(loginUser.getPermissions());
        return R.ok(vo);
    }

    /**
     * 获取动态路由
     * <p>
     * 根据当前登录用户的角色，查询其可访问的菜单并组装为前端路由树。
     * 前端接收后注册为 Vue Router 动态路由，实现菜单级别的权限控制。
     * 注意：只返回目录(M)和菜单(C)，不返回按钮(F)——按钮权限通过 permissions 集合控制。
     * </p>
     *
     * @return 路由树
     */
    @GetMapping("/routers")
    public R<List<RouterVo>> routers() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        boolean isAdmin = loginUser.getRoleKeys() != null && loginUser.getRoleKeys().contains("admin");
        List<RouterVo> routerTree = sysMenuService.getRouterTree(loginUser.getUserId(), isAdmin);
        return R.ok(routerTree);
    }

    /**
     * 获取客户端真实 IP
     * <p>
     * 优先从反向代理头 X-Forwarded-For / X-Real-IP 中获取，
     * 兜底使用 remoteAddr。适配 Nginx 等反向代理场景。
     * </p>
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
