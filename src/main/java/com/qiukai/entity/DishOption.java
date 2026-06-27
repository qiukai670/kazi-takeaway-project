package com.qiukai.entity;

import lombok.Data;

/**
 * 菜品规格组实体（如辣度、酱料）
 */
@Data
public class DishOption {

    private Long id;
    private Long dishId;
    private String name;
    private String optionType;
    private Integer sort;
}
