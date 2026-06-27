package com.qiukai.mapper;

import com.qiukai.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商家收藏 Mapper
 */
@Mapper
public interface FavoriteMapper {

    /** 查询用户收藏的商家列表（联表查询） */
    List<Merchant> selectFavoriteMerchants(@Param("userId") Long userId);

    /** 判断是否已收藏 */
    Integer countByUserAndMerchant(@Param("userId") Long userId, @Param("merchantId") Long merchantId);

    int insert(@Param("userId") Long userId, @Param("merchantId") Long merchantId);

    int delete(@Param("userId") Long userId, @Param("merchantId") Long merchantId);
}
