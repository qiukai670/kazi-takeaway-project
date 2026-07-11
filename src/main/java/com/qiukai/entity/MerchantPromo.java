package com.qiukai.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家优惠实体
 */
@Data
public class MerchantPromo {

    private Long id;
    private Long merchantId;
    private String type;
    private String description;
    private String name;
    private Integer discountRatio;
    private BigDecimal minSpend;
    private Integer priority;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 0启用 1停用 */
    private Integer status;
    private LocalDateTime createTime;
}
