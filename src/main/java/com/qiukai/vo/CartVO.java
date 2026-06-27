package com.qiukai.vo;

import com.qiukai.entity.Cart;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车汇总响应（含实时金额计算）
 */
@Data
public class CartVO {

    private Long merchantId;
    private String merchantName;
    private BigDecimal deliveryFee;
    private BigDecimal minOrder;
    private List<Cart> items;
    private Integer totalCount;
    private BigDecimal totalAmount;
    /** 距起送价差额，0 表示已达起送 */
    private BigDecimal remainToMinOrder;
}
