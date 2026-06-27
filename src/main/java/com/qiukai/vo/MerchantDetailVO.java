package com.qiukai.vo;

import com.qiukai.entity.Merchant;
import com.qiukai.entity.MerchantPromo;
import com.qiukai.entity.PromoRule;
import lombok.Data;

import java.util.List;

/**
 * 商家详情响应（含优惠与满减规则）
 */
@Data
public class MerchantDetailVO {

    private Merchant merchant;
    private List<MerchantPromo> promos;
    private List<PromoRule> promoRules;
}
