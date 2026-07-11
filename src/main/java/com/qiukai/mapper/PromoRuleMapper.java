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

    PromoRule selectById(@Param("id") Long id);

    int insert(PromoRule rule);

    int update(PromoRule rule);

    int deleteById(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
