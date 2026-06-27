package com.qiukai.vo;

import lombok.Data;

/**
 * 登录成功响应
 */
@Data
public class LoginVO {

    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Integer role;
    private String memberLevel;
    private Integer points;
}
