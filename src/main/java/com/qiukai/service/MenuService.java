package com.qiukai.service;

import com.qiukai.common.BusinessException;
import com.qiukai.entity.*;
import com.qiukai.mapper.*;
import com.qiukai.vo.DishDetailVO;
import com.qiukai.vo.DishOptionGroupVO;
import com.qiukai.vo.MerchantDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单浏览服务
 * 提供商家信息展示、菜品列表（按销量降序、折扣优先）、菜品规格选项
 */
@Service
public class MenuService {

    @Autowired
    private MerchantMapper merchantMapper;
    @Autowired
    private MerchantPromoMapper merchantPromoMapper;
    @Autowired
    private PromoRuleMapper promoRuleMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishOptionMapper dishOptionMapper;
    @Autowired
    private DishOptionChoiceMapper dishOptionChoiceMapper;

    /**
     * 商家列表（可按分类、关键词筛选）
     */
    public List<Merchant> listMerchants(String category, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return merchantMapper.search(keyword.trim());
        }
        if (category != null && !category.isBlank() && !"全部".equals(category)) {
            return merchantMapper.selectByCategory(category);
        }
        return merchantMapper.selectAll();
    }

    /**
     * 好评商家列表（首页"好评商家"板块）
     */
    public List<Merchant> listRecommendedMerchants() {
        return merchantMapper.selectRecommended();
    }

    /**
     * 商家详情（含优惠信息与满减规则）
     */
    public MerchantDetailVO getMerchantDetail(Long id) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        MerchantDetailVO vo = new MerchantDetailVO();
        vo.setMerchant(merchant);
        vo.setPromos(merchantPromoMapper.selectByMerchantId(id));
        vo.setPromoRules(promoRuleMapper.selectByMerchantId(id));
        return vo;
    }

    /**
     * 商家菜品列表（仅上架，折扣优先、按销量降序）
     */
    public List<Dish> listDishes(Long merchantId) {
        if (merchantMapper.selectById(merchantId) == null) {
            throw new BusinessException("商家不存在");
        }
        return dishMapper.selectOnShelfByMerchantId(merchantId);
    }

    /**
     * 人气菜品聚合列表（跨商家，仅上架且 is_popular=1，按销量降序）
     */
    public List<Dish> listPopularDishes() {
        return dishMapper.selectPopular();
    }

    /**
     * 菜品详情（含规格选项组与选项值）
     */
    public DishDetailVO getDishDetail(Long dishId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }
        DishDetailVO vo = new DishDetailVO();
        vo.setDish(dish);

        List<DishOption> options = dishOptionMapper.selectByDishId(dishId);
        if (options.isEmpty()) {
            vo.setOptions(new ArrayList<>());
            return vo;
        }

        List<Long> optionIds = options.stream().map(DishOption::getId).toList();
        List<DishOptionChoice> choices = dishOptionChoiceMapper.selectByOptionIds(optionIds);
        Map<Long, List<DishOptionChoice>> choiceMap = choices.stream()
                .collect(Collectors.groupingBy(DishOptionChoice::getOptionId));

        List<DishOptionGroupVO> groups = options.stream().map(opt -> {
            DishOptionGroupVO group = new DishOptionGroupVO();
            group.setId(opt.getId());
            group.setName(opt.getName());
            group.setOptionType(opt.getOptionType());
            group.setSort(opt.getSort());
            group.setChoices(choiceMap.getOrDefault(opt.getId(), new ArrayList<>()));
            return group;
        }).toList();
        vo.setOptions(groups);
        return vo;
    }

    /**
     * 商家优惠（供订单计算使用）
     */
    public List<PromoRule> getPromoRules(Long merchantId) {
        return promoRuleMapper.selectByMerchantId(merchantId);
    }

    /**
     * 商家信息（供订单使用）
     */
    public Merchant getMerchant(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        return merchant;
    }
}
