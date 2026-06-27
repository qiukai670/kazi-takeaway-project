package com.qiukai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录令牌实体，支撑 30 天自动登录
 */
@Data
public class UserToken {

    private Long id;
    private Long userId;
    private String token;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
