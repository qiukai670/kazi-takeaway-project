package com.qiukai.mapper;

import com.qiukai.entity.MerchantPromo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商家优惠 Mapper
 */
@Mapper
public interface MerchantPromoMapper {

    List<MerchantPromo> selectByMerchantId(@Param("merchantId") Long merchantId);

    MerchantPromo selectById(@Param("id") Long id);

    int insert(MerchantPromo promo);

    int update(MerchantPromo promo);

    int deleteById(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
