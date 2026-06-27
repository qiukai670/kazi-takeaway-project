package com.qiukai.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 菜品规格选项值 DTO（管理员配置用）
 */
@Data
public class DishOptionChoiceDTO {

    /** 选项名称（如"微辣"、"少冰"） */
    private String name;

    /** 加价金额 */
    private BigDecimal priceAdd;

    /** 排序 */
    private Integer sort;
}
