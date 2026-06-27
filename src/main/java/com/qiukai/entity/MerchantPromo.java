package com.qiukai.entity;

import lombok.Data;

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
    private LocalDateTime createTime;
}
