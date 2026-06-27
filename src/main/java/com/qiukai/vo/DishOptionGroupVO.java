package com.qiukai.vo;

import com.qiukai.entity.DishOptionChoice;
import lombok.Data;

import java.util.List;

/**
 * 菜品规格组（含选项值）
 */
@Data
public class DishOptionGroupVO {

    private Long id;
    private String name;
    private String optionType;
    private Integer sort;
    private List<DishOptionChoice> choices;
}
