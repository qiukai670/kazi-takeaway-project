package com.qiukai.mapper;

import com.qiukai.entity.DishOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜品规格组 Mapper
 */
@Mapper
public interface DishOptionMapper {

    List<DishOption> selectByDishId(@Param("dishId") Long dishId);

    /** 删除某菜品的所有规格组 */
    int deleteByDishId(@Param("dishId") Long dishId);

    /** 新增规格组，返回自增ID */
    int insert(DishOption dishOption);
}
