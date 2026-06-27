package com.qiukai.interceptor;

import com.qiukai.entity.User;
import com.qiukai.entity.UserToken;
import com.qiukai.mapper.UserMapper;
import com.qiukai.mapper.UserTokenMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器
 * 从 Cookie 或 Authorization 头中提取 token，校验有效后写入 UserContext
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String COOKIE_NAME = "kazi_token";
    public static final String HEADER_NAME = "Authorization";

    @Autowired
    private UserTokenMapper userTokenMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = extractToken(request);
        if (token == null) {
            // 未携带 token，放行至 Controller 由具体接口决定是否需要登录
            return true;
        }
        UserToken userToken = userTokenMapper.selectByToken(token);
        if (userToken == null) {
            return true;
        }
        User user = userMapper.selectById(userToken.getUserId());
        if (user != null && user.getStatus() == 0) {
            UserContext.setCurrentUser(user.getId(), user.getRole());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    /**
     * 提取 token：优先 Authorization 头，其次 Cookie
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_NAME);
        if (header != null && !header.isBlank()) {
            // 兼容 "Bearer xxx" 格式
            return header.startsWith("Bearer ") ? header.substring(7) : header;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
