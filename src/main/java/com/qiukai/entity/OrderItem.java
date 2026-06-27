package com.qiukai.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细实体
 */
@Data
public class OrderItem {

    private Long id;
    private Long orderId;
    private Long dishId;
    private String dishName;
    private String dishImage;
    private BigDecimal unitPrice;
    private Integer qty;
    private String optionsText;
    private BigDecimal subtotal;
}
