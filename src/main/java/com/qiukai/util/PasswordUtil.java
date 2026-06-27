package com.qiukai.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 密码加密工具
 * 采用 PBKDF2WithHmacSHA256 算法，随机盐值 + 高迭代次数，安全存储用户密码
 * 存储格式：pbkdf2:{iterations}:{saltHex}:{hashHex}
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 10000;
    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final String PREFIX = "pbkdf2";

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /**
     * 对明文密码加密，返回存储格式字符串
     */
    public static String encode(String rawPassword) {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        byte[] hash = computeHash(rawPassword, salt, ITERATIONS);
        return PREFIX + ":" + ITERATIONS + ":" + HexFormat.of().formatHex(salt) + ":" + HexFormat.of().formatHex(hash);
    }

    /**
     * 校验明文密码与存储的加密串是否匹配
     */
    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || !storedPassword.startsWith(PREFIX + ":")) {
            return false;
        }
        String[] parts = storedPassword.split(":");
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = HexFormat.of().parseHex(parts[2]);
            byte[] expectedHash = HexFormat.of().parseHex(parts[3]);
            byte[] actualHash = computeHash(rawPassword, salt, iterations);
            return constantTimeEquals(expectedHash, actualHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 计算 PBKDF2 哈希
     */
    private static byte[] computeHash(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("密码加密失败", e);
        }
    }

    /**
     * 常量时间比较，防止时序攻击
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
