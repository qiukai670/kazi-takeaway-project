package com.qiukai.dto;

import lombok.Data;

import java.util.List;

/**
 * 菜品规格组 DTO（管理员配置用）
 */
@Data
public class DishOptionDTO {

    /** 规格组名称（如"辣度"、"冰度"） */
    private String name;

    /** 选项类型：single 单选 / multi 多选 */
    private String optionType;

    /** 排序 */
    private Integer sort;

    /** 选项值列表 */
    private List<DishOptionChoiceDTO> choices;
}
