package com.qiukai.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 满减规则实体（一档一行）
 */
@Data
public class PromoRule {

    private Long id;
    private Long merchantId;
    private BigDecimal threshold;
    private BigDecimal discount;
}
