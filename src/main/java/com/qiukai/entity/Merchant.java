package com.qiukai.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家实体
 */
@Data
public class Merchant {

    private Long id;
    /** 关联用户ID(role=2) */
    private Long userId;
    private String name;
    private String logo;
    private String cover;
    private String category;
    private String priceLevel;
    private BigDecimal rating;
    private Integer sales;
    private Integer deliveryTime;
    private BigDecimal distance;
    private BigDecimal minOrder;
    private BigDecimal deliveryFee;
    private String badge;
    private String tags;
    private Integer status;
    /** 好评商家 0否 1是 */
    private Integer isRecommended;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
