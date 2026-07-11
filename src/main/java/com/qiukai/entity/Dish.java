package com.qiukai.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品实体
 */
@Data
public class Dish {

    private Long id;
    private Long merchantId;
    private String name;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private String image;
    private String description;
    private String category;
    private Integer sales;
    private Integer isDiscount;
    private Integer isNew;
    private Integer isPopular;
    private Integer onShelf;
    /** 售罄 0否 1是 */
    private Integer soldOut;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
