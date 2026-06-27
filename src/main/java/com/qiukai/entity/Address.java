package com.qiukai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收货地址实体
 */
@Data
public class Address {

    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String region;
    private String detail;
    private String tag;
    private Integer isDefault;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
