package com.qiukai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（普通用户与管理员共用，通过 role 区分）
 */
@Data
public class User {

    private Long id;
    private String phone;
    private String username;
    private String nickname;
    private String password;
    private String avatar;
    private Integer gender;
    private Integer role;
    private String memberLevel;
    private Integer points;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
