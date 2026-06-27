package com.qiukai.mapper;

import com.qiukai.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 购物车 Mapper
 */
@Mapper
public interface CartMapper {

    /** 查询用户在某商家的购物车 */
    List<Cart> selectByUserAndMerchant(@Param("userId") Long userId, @Param("merchantId") Long merchantId);

    /** 查询用户全部购物车 */
    List<Cart> selectByUserId(@Param("userId") Long userId);

    /** 精确查找同菜品同规格的购物车项 */
    Cart selectExact(@Param("userId") Long userId, @Param("merchantId") Long merchantId,
                     @Param("dishId") Long dishId, @Param("optionsSnapshot") String optionsSnapshot);

    int insert(Cart cart);

    int updateQty(@Param("id") Long id, @Param("qty") Integer qty);

    int deleteById(@Param("id") Long id, @Param("userId") Long userId);

    int deleteByUserAndMerchant(@Param("userId") Long userId, @Param("merchantId") Long merchantId);
}
