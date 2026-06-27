package com.qiukai.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
public class Order {

    private Long id;
    private String orderNo;
    private Long userId;
    private String username;
    private Long merchantId;
    private String merchantName;
    private String merchantLogo;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private String status;
    private String note;
    private String payMethod;
    private String addressSnapshot;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
