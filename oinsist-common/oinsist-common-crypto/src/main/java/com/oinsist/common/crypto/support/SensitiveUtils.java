package com.oinsist.common.crypto.support;

import com.oinsist.common.crypto.enums.SensitiveType;

/**
 * 统一脱敏工具类
 * <p>
 * 集中管理所有脱敏规则，供 Jackson 序列化器和操作日志切面共同复用，
 * 避免出现「接口输出一套规则、日志记录另一套规则」的不一致问题。
 * <p>
 * 脱敏策略：保留数据的首尾特征（便于人工识别），中间部分用 * 替换。
 */
public final class SensitiveUtils {

    private SensitiveUtils() {
        // 工具类禁止实例化
    }

    /**
     * 根据脱敏类型对明文进行掩码处理
     *
     * @param value 原始明文
     * @param type  脱敏类型
     * @return 脱敏后的字符串；如果输入为 null 或空则原样返回
     */
    public static String desensitize(String value, SensitiveType type) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return switch (type) {
            case PHONE -> desensitizePhone(value);
            case EMAIL -> desensitizeEmail(value);
            case ID_CARD -> desensitizeIdCard(value);
            case BANK_CARD -> desensitizeBankCard(value);
            case NAME -> desensitizeName(value);
            case CUSTOM -> "***";
        };
    }

    /**
     * 手机号脱敏：保留前3后4，中间用****代替
     * 示例：13812345678 → 138****5678
     */
    private static String desensitizePhone(String phone) {
        if (phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱脱敏：用户名仅保留首字符，@后域名完整保留
     * 示例：test@gmail.com → t***@gmail.com
     */
    private static String desensitizeEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    /**
     * 身份证号脱敏：保留前6后4，中间用********代替
     * 示例：110101199001011234 → 110101********1234
     */
    private static String desensitizeIdCard(String idCard) {
        if (idCard.length() < 10) {
            return "***";
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 银行卡号脱敏：保留前4后4，中间用****代替
     * 示例：6222021234561234 → 6222****1234
     */
    private static String desensitizeBankCard(String bankCard) {
        if (bankCard.length() < 8) {
            return "***";
        }
        return bankCard.substring(0, 4) + "****" + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 姓名脱敏：仅保留姓氏，名用*代替
     * 示例：张三 → 张*，欧阳修 → 欧阳*
     */
    private static String desensitizeName(String name) {
        if (name.length() <= 1) {
            return "*";
        }
        // 处理复姓（2字姓）和单姓
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        // 保留前面字符，最后一个字用*替代
        return name.substring(0, name.length() - 1) + "*";
    }
}
