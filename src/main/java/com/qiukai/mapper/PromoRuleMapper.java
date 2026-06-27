package com.qiukai.mapper;

import com.qiukai.entity.PromoRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 满减规则 Mapper
 */
@Mapper
public interface PromoRuleMapper {

    List<PromoRule> selectByMerchantId(@Param("merchantId") Long merchantId);
}
