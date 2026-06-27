package com.qiukai.mapper;

import com.qiukai.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户评价 Mapper
 */
@Mapper
public interface ReviewMapper {

    int insert(Review review);

    int update(Review review);

    int delete(@Param("id") Long id);

    Review selectByOrderId(@Param("orderId") Long orderId);

    Review selectById(@Param("id") Long id);

    List<Review> selectByMerchantId(@Param("merchantId") Long merchantId);

    List<Review> selectByUserId(@Param("userId") Long userId);
}
