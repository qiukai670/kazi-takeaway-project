package com.qiukai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数
 * account 支持手机号（用户登录）或用户名（管理员登录）
 */
@Data
public class LoginDTO {

    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 是否记住我（30天自动登录） */
    private Boolean rememberMe;
}
