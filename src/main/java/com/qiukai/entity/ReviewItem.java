package com.qiukai.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价菜品明细实体
 */
@Data
public class ReviewItem {

    private Long id;
    private Long reviewId;
    private Long dishId;
    private String dishName;
    private String dishImage;
    private Integer rating;
    private String content;
    private String images;
    private LocalDateTime createTime;

    // ===== 瞬态字段（不写入数据库，由 service 装配）=====
    private List<String> imageList;
}
