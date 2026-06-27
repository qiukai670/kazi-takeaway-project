package com.qiukai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商家新增/编辑请求参数
 */
@Data
public class MerchantDTO {

    @NotBlank(message = "商家名称不能为空")
    @Size(max = 64, message = "商家名称最长64字")
    private String name;

    /** Logo URL */
    private String logo;

    /** 封面图 URL */
    private String cover;

    @NotBlank(message = "分类不能为空")
    private String category;

    /** 价格档位 ¥/¥¥/¥¥¥ */
    private String priceLevel;

    /** 评分 */
    private BigDecimal rating;

    /** 月售量 */
    private Integer sales;

    /** 配送时长(分钟) */
    private Integer deliveryTime;

    /** 距离(km) */
    private BigDecimal distance;

    /** 起送价 */
    private BigDecimal minOrder;

    /** 配送费 */
    private BigDecimal deliveryFee;

    /** 徽章：品牌/品质优选/新店 */
    private String badge;

    /** 特色标签(逗号分隔) */
    private String tags;

    /** 状态 0营业 1休息 */
    private Integer status;

    /** 好评商家 0否 1是 */
    private Integer isRecommended;
}
