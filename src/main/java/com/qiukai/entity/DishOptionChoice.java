package com.qiukai.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 规格选项值实体（如微辣、中辣，可加价）
 */
@Data
public class DishOptionChoice {

    private Long id;
    private Long optionId;
    private String name;
    private BigDecimal priceAdd;
    private Integer sort;
}
