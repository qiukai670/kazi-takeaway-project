package com.qiukai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 评价请求参数
 */
@Data
public class ReviewDTO {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "整体服务评分不能为空")
    @Min(value = 1, message = "评分最低1分")
    @Max(value = 5, message = "评分最高5分")
    private Integer rating;

    @Min(value = 1, message = "配送评分最低1分")
    @Max(value = 5, message = "配送评分最高5分")
    private Integer deliveryRating;

    @Size(max = 500, message = "评价内容最长500字")
    private String content;

    @Valid
    private List<ReviewItemDTO> items;
}
