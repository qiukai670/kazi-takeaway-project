package com.qiukai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 评价菜品明细请求参数
 */
@Data
public class ReviewItemDTO {

    private Long dishId;
    private String dishName;
    private String dishImage;

    @Min(value = 1, message = "菜品评分最低1分")
    @Max(value = 5, message = "菜品评分最高5分")
    private Integer rating;

    @Size(max = 500, message = "菜品评价内容最长500字")
    private String content;

    @Size(max = 3, message = "每道菜最多上传3张图片")
    private List<String> images;
}
