package com.qiukai.interceptor;

/**
 * 当前登录用户上下文
 * 基于 ThreadLocal 在一次请求内传递登录用户信息，请求结束后由拦截器清理
 */
public class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> CURRENT_ROLE = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setCurrentUser(Long userId, Integer role) {
        CURRENT_USER_ID.set(userId);
        CURRENT_ROLE.set(role);
    }

    public static Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    public static Integer getCurrentRole() {
        return CURRENT_ROLE.get();
    }

    /** 判断当前是否为管理员 */
    public static boolean isAdmin() {
        Integer role = CURRENT_ROLE.get();
        return role != null && role == 1;
    }

    /** 判断当前是否为商家 */
    public static boolean isMerchant() {
        Integer role = CURRENT_ROLE.get();
        return role != null && role == 2;
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_ROLE.remove();
    }
}
