package com.qiukai.vo;

import com.qiukai.entity.Dish;
import lombok.Data;

import java.util.List;

/**
 * 菜品详情响应（含规格选项组）
 */
@Data
public class DishDetailVO {

    private Dish dish;
    private List<DishOptionGroupVO> options;
}
