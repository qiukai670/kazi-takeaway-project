package com.qiukai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商家注册请求参数
 */
@Data
public class MerchantRegisterDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "商家名称不能为空")
    @Size(max = 64, message = "商家名称最多64个字符")
    private String merchantName;

    @NotBlank(message = "注册人姓名不能为空")
    @Size(max = 32, message = "注册人姓名最多32个字符")
    private String registrantName;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需为3-20位")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需为8-32位")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /** Logo 图片URL */
    private String logo;

    /** 封面图URL */
    private String cover;

    @NotBlank(message = "商家分类不能为空")
    private String category;
}
