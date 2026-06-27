package com.qiukai.mapper;

import com.qiukai.entity.DishOptionChoice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 规格选项值 Mapper
 */
@Mapper
public interface DishOptionChoiceMapper {

    /** 根据规格组ID列表批量查询选项值 */
    List<DishOptionChoice> selectByOptionIds(@Param("optionIds") List<Long> optionIds);

    /** 根据选项值ID列表批量查询（用于购物车加价计算） */
    List<DishOptionChoice> selectByIds(@Param("ids") List<Long> ids);

    /** 删除指定规格组ID下的所有选项值 */
    int deleteByOptionIds(@Param("optionIds") List<Long> optionIds);

    /** 批量新增选项值 */
    int insertBatch(@Param("list") List<DishOptionChoice> choices);
}
