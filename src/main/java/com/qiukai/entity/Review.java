package com.qiukai.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户评价实体
 */
@Data
public class Review {

    private Long id;
    private Long userId;
    private Long orderId;
    private Long merchantId;
    private Integer rating;
    private Integer deliveryRating;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ===== 瞬态字段（不写入数据库，由 service 装配）=====
    private String merchantName;
    private String orderNo;
    private List<ReviewItem> items;
}
