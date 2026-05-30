package com.oinsist.common.crypto.support;

import com.oinsist.common.crypto.enums.SensitiveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SensitiveUtils 脱敏工具单元测试
 */
class SensitiveUtilsTest {

    @Test
    @DisplayName("手机号脱敏：保留前3后4")
    void desensitizePhone() {
        assertEquals("138****5678", SensitiveUtils.desensitize("13812345678", SensitiveType.PHONE));
    }

    @Test
    @DisplayName("手机号脱敏：短号码返回***")
    void desensitizePhoneShort() {
        assertEquals("***", SensitiveUtils.desensitize("123456", SensitiveType.PHONE));
    }

    @Test
    @DisplayName("邮箱脱敏：保留首字符+域名")
    void desensitizeEmail() {
        assertEquals("t***@gmail.com", SensitiveUtils.desensitize("test@gmail.com", SensitiveType.EMAIL));
    }

    @Test
    @DisplayName("邮箱脱敏：无@符号返回***")
    void desensitizeEmailInvalid() {
        assertEquals("***", SensitiveUtils.desensitize("noemail", SensitiveType.EMAIL));
    }

    @Test
    @DisplayName("身份证脱敏：保留前6后4")
    void desensitizeIdCard() {
        assertEquals("110101********1234", SensitiveUtils.desensitize("110101199001011234", SensitiveType.ID_CARD));
    }

    @Test
    @DisplayName("身份证脱敏：短号码返回***")
    void desensitizeIdCardShort() {
        assertEquals("***", SensitiveUtils.desensitize("12345", SensitiveType.ID_CARD));
    }

    @Test
    @DisplayName("银行卡脱敏：保留前4后4")
    void desensitizeBankCard() {
        assertEquals("6222****1234", SensitiveUtils.desensitize("6222021234561234", SensitiveType.BANK_CARD));
    }

    @Test
    @DisplayName("银行卡脱敏：短号码返回***")
    void desensitizeBankCardShort() {
        assertEquals("***", SensitiveUtils.desensitize("1234567", SensitiveType.BANK_CARD));
    }

    @Test
    @DisplayName("姓名脱敏：两字名")
    void desensitizeNameTwo() {
        assertEquals("张*", SensitiveUtils.desensitize("张三", SensitiveType.NAME));
    }

    @Test
    @DisplayName("姓名脱敏：三字名")
    void desensitizeNameThree() {
        assertEquals("张三*", SensitiveUtils.desensitize("张三丰", SensitiveType.NAME));
    }

    @Test
    @DisplayName("姓名脱敏：单字返回*")
    void desensitizeNameSingle() {
        assertEquals("*", SensitiveUtils.desensitize("张", SensitiveType.NAME));
    }

    @Test
    @DisplayName("自定义脱敏：全部替换为***")
    void desensitizeCustom() {
        assertEquals("***", SensitiveUtils.desensitize("任意内容", SensitiveType.CUSTOM));
    }

    @Test
    @DisplayName("空值处理：null 返回 null")
    void desensitizeNull() {
        assertNull(SensitiveUtils.desensitize(null, SensitiveType.PHONE));
    }

    @Test
    @DisplayName("空值处理：空字符串原样返回")
    void desensitizeEmpty() {
        assertEquals("", SensitiveUtils.desensitize("", SensitiveType.PHONE));
    }

    @Test
    @DisplayName("空值处理：空白字符串原样返回")
    void desensitizeBlank() {
        assertEquals("   ", SensitiveUtils.desensitize("   ", SensitiveType.PHONE));
    }
}
