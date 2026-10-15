package com.gaia.domain.service.treasure;

import java.security.SecureRandom;

/**
 * 夺宝码生成器（纯领域服务）。
 *
 * <p>规则：6 位大写字母 {@code [A-Z]{6}}，空间 26^6 ≈ 3.08 亿。</p>
 */
public class TreasureCodeGenerator {

    /** 码长度 */
    public static final int CODE_LENGTH = 6;

    /** 字母表 A-Z */
    public static final char[] ALPHABET;
    static {
        ALPHABET = new char[26];
        for (int i = 0; i < 26; i++) {
            ALPHABET[i] = (char) ('A' + i);
        }
    }

    private final SecureRandom random = new SecureRandom();

    /**
     * 生成一个 6 位大写字母随机串。
     */
    public String generateOne() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    /**
     * 验证是否是合法格式：6 位 A-Z。
     */
    public boolean isValid(String code) {
        if (code == null || code.length() != CODE_LENGTH) {
            return false;
        }
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c < 'A' || c > 'Z') {
                return false;
            }
        }
        return true;
    }
}