package com.qiukai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 银行卡实体
 */
@Data
public class BankCard {

    private Long id;
    private Long userId;
    private String holder;
    private String cardNumber;
    private String bankName;
    private String cardType;
    private String expireDate;
    private String cvv;
    private Integer isDefault;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
