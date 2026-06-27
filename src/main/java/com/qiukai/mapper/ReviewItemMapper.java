package com.qiukai.mapper;

import com.qiukai.entity.ReviewItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评价菜品明细 Mapper
 */
@Mapper
public interface ReviewItemMapper {

    int insertBatch(@Param("items") List<ReviewItem> items);

    List<ReviewItem> selectByReviewId(@Param("reviewId") Long reviewId);

    List<ReviewItem> selectByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    int deleteByReviewId(@Param("reviewId") Long reviewId);
}
