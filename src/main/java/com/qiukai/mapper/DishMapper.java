package com.qiukai.mapper;

import com.qiukai.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜品 Mapper
 */
@Mapper
public interface DishMapper {

    Dish selectById(@Param("id") Long id);

    /** 查询商家上架菜品（折扣优先，再按销量降序） */
    List<Dish> selectOnShelfByMerchantId(@Param("merchantId") Long merchantId);

    /** 管理员查询商家全部菜品（含下架） */
    List<Dish> selectAllByMerchantId(@Param("merchantId") Long merchantId);

    /** 管理员查询全部菜品（跨商家，含下架） */
    List<Dish> selectAllForAdmin();

    /** 查询人气菜品（跨商家，仅上架且 is_popular=1） */
    List<Dish> selectPopular();

    int insert(Dish dish);

    int update(Dish dish);

    int deleteById(@Param("id") Long id);

    /** 销量自增 */
    int increaseSales(@Param("id") Long id, @Param("qty") Integer qty);

    /** 切换售罄状态 */
    int updateSoldOut(@Param("id") Long id, @Param("soldOut") Integer soldOut);
}
