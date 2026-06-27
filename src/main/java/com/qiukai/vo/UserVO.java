package com.qiukai.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应（不含密码等敏感字段）
 */
@Data
public class UserVO {

    private Long id;
    private String phone;
    private String username;
    private String nickname;
    private String avatar;
    private Integer gender;
    private Integer role;
    private String memberLevel;
    private Integer points;
    private LocalDateTime createTime;
}
