package com.qiukai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家收藏实体
 */
@Data
public class Favorite {

    private Long id;
    private Long userId;
    private Long merchantId;
    private LocalDateTime createTime;
}
