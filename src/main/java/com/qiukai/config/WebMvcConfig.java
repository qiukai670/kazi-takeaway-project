package com.qiukai.config;

import com.qiukai.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 1. 注册认证拦截器，拦截 /api/** 请求
 * 2. 将 View 层 HTML 页面（位于 classpath:/com/qiukai/view/）作为静态资源对外提供
 * 3. 配置 CORS，便于前后端联调
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    /**
     * 注册拦截器：仅拦截 API 接口，静态页面与资源放行
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/register",
                        "/api/user/login",
                        "/api/user/admin/login",
                        "/api/merchants/**",
                        "/api/merchant/**",
                        "/api/dishes/**"
                );
    }

    /**
     * 静态资源映射
     * 1. /uploads/** 映射到本地 uploads 目录（运行时上传的图片）
     * 2. /** 映射到 classpath:/com/qiukai/view/（HTML 页面）
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/com/qiukai/view/");
    }

    /**
     * 根路径转发至首页
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/index.html");
    }

    /**
     * CORS 配置：允许前端跨域调用 API
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
