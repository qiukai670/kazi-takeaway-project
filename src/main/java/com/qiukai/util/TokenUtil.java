package com.qiukai.util;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 登录令牌工具
 * 生成 32 字节安全随机串，转为 64 位十六进制字符串作为 token
 */
public final class TokenUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private TokenUtil() {
    }

    /**
     * 生成安全随机 token
     */
    public static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
