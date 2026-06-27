package com.qiukai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 加入购物车请求参数
 */
@Data
public class CartItemDTO {

    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @NotNull(message = "菜品ID不能为空")
    private Long dishId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer qty;

    /** 已选规格选项（无规格菜品为空） */
    private List<CartOptionChoice> choices;

    /**
     * 规格选项选择项
     */
    @Data
    public static class CartOptionChoice {
        private Long optionId;
        private Long choiceId;
        private String optionName;
        private String choiceName;
        private java.math.BigDecimal priceAdd;
    }
}
