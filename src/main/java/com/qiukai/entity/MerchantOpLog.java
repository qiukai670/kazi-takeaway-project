package com.qiukai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家操作日志实体
 */
@Data
public class MerchantOpLog {
    private Long id;
    private Long merchantId;
    private String merchantName;
    private Long operatorId;
    private String operatorName;
    private String action;
    private String oldValue;
    private String newValue;
    private LocalDateTime createTime;
}
