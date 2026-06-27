package com.qiukai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品新增/编辑请求参数
 */
@Data
public class DishDTO {

    /** 所属商家ID（新增时必填，默认1） */
    private Long merchantId;

    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 30, message = "菜品名称最长30字")
    private String name;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    /** 原价（折扣商品填写） */
    private BigDecimal oldPrice;

    /** 图片URL */
    private String image;

    /** 描述 */
    @Size(max = 200, message = "描述最长200字")
    private String description;

    /** 子分类 */
    private String category;

    /** 是否折扣 0否 1是 */
    private Integer isDiscount;

    /** 是否新品 0否 1是 */
    private Integer isNew;

    /** 是否人气菜品 0否 1是 */
    private Integer isPopular;

    /** 上下架 0上架 1下架 */
    private Integer onShelf;

    /** 口味定制选项组列表（为空表示不支持定制） */
    private List<DishOptionDTO> options;
}
