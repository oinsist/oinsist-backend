package com.oinsist.admin.controller;

import com.oinsist.common.core.domain.R;
import com.oinsist.common.crypto.annotation.ApiEncrypt;
import com.oinsist.common.crypto.annotation.Sensitive;
import com.oinsist.common.crypto.enums.SensitiveType;
import com.oinsist.system.domain.SysUser;
import com.oinsist.system.mapper.SysUserMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * 数据安全功能验证 Controller —— 开发测试专用，生产环境不会加载
 * <p>
 * 通过 @Profile("dev") 限制仅在 dev 环境激活，避免测试接口暴露到生产环境。
 * <p>
 * 验证三个核心能力：
 * 1. GET /test/crypto/sensitive → 响应脱敏
 * 2. POST /test/crypto/encrypt → 接口加解密
 * 3. GET /test/crypto/user/{userId} → 字段加密存储 + 查询解密（返回 VO，不暴露密码）
 * 4. POST /test/crypto/user/{userId} → 更新用户（字段入库加密）
 */
@Profile("dev")
@RestController
@RequestMapping("/test/crypto")
@RequiredArgsConstructor
public class CryptoTestController {

    private final SysUserMapper sysUserMapper;

    // ==================== 1. 响应脱敏验证 ====================

    /**
     * 验证 @Sensitive 脱敏效果
     * <p>
     * 预期响应：
     * - phone: "138****5678"
     * - email: "t***@example.com"
     * - idCard: "110101********1234"
     * - name: "张*"
     */
    @GetMapping("/sensitive")
    public R<SensitiveDemo> sensitive() {
        SensitiveDemo demo = new SensitiveDemo();
        demo.setPhone("13812345678");
        demo.setEmail("test@example.com");
        demo.setIdCard("110101199001011234");
        demo.setBankCard("6222021234561234");
        demo.setName("张三");
        demo.setNormal("这个字段不脱敏");
        return R.ok(demo);
    }

    // ==================== 2. 接口加解密验证 ====================

    /**
     * 验证 @ApiEncrypt 接口加解密
     * <p>
     * 请求格式：{"data": "AES-GCM密文(Base64)"}
     * 密文解密后应为合法 JSON，如：{"message":"hello"}
     * <p>
     * 响应格式：R 的 data 字段为加密后的密文字符串
     */
    @ApiEncrypt
    @PostMapping("/encrypt")
    public R<EchoResponse> encrypt(@RequestBody EchoRequest request) {
        EchoResponse response = new EchoResponse();
        response.setEcho("收到消息: " + request.getMessage());
        response.setTimestamp(System.currentTimeMillis());
        return R.ok(response);
    }

    // ==================== 3. 数据库字段加密验证 ====================

    /**
     * 查询用户 - 验证加密字段自动解密
     * <p>
     * 预期：数据库中 email/phonenumber 为密文，查询结果为明文（但响应输出会被 @Sensitive 脱敏）
     * 返回 UserCryptoVO 而非 SysUser 原始实体，避免暴露 password 等敏感字段
     */
    @GetMapping("/user/{userId}")
    public R<UserCryptoVO> getUser(@PathVariable Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return R.fail("用户不存在");
        }
        UserCryptoVO vo = new UserCryptoVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setPhonenumber(user.getPhonenumber());
        return R.ok(vo);
    }

    /**
     * 更新用户邮箱和手机号 - 验证加密字段自动加密入库
     * <p>
     * 调用后可直接查询数据库确认 email/phonenumber 列为密文
     */
    @PostMapping("/user/{userId}")
    public R<Void> updateUser(@PathVariable Long userId,
                              @RequestParam String email,
                              @RequestParam String phonenumber) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setEmail(email);
        user.setPhonenumber(phonenumber);
        sysUserMapper.updateById(user);
        return R.ok();
    }

    // ==================== 内部 DTO ====================

    /**
     * 用户加密字段查询结果 VO —— 仅暴露必要字段，不包含 password
     */
    @Data
    static class UserCryptoVO {
        private Long userId;
        private String username;

        @Sensitive(type = SensitiveType.EMAIL)
        private String email;

        @Sensitive(type = SensitiveType.PHONE)
        private String phonenumber;
    }

    /**
     * 脱敏演示对象
     */
    @Data
    static class SensitiveDemo {
        @Sensitive(type = SensitiveType.PHONE)
        private String phone;

        @Sensitive(type = SensitiveType.EMAIL)
        private String email;

        @Sensitive(type = SensitiveType.ID_CARD)
        private String idCard;

        @Sensitive(type = SensitiveType.BANK_CARD)
        private String bankCard;

        @Sensitive(type = SensitiveType.NAME)
        private String name;

        private String normal;
    }

    /**
     * 接口加密测试 - 请求体
     */
    @Data
    static class EchoRequest {
        private String message;
    }

    /**
     * 接口加密测试 - 响应体
     */
    @Data
    static class EchoResponse {
        private String echo;
        private Long timestamp;
    }
}
