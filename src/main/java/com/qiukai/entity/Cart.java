package com.qiukai.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车实体
 */
@Data
public class Cart {

    private Long id;
    private Long userId;
    private Long merchantId;
    private Long dishId;
    private String dishName;
    private String dishImage;
    private BigDecimal basePrice;
    private BigDecimal unitPrice;
    private Integer qty;
    private String optionsText;
    private String optionsSnapshot;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
