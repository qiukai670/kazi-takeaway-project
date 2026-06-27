package com.qiukai.controller;

import com.qiukai.common.Result;
import com.qiukai.entity.Dish;
import com.qiukai.entity.Merchant;
import com.qiukai.service.MenuService;
import com.qiukai.vo.DishDetailVO;
import com.qiukai.vo.MerchantDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单浏览控制器
 * 提供商家列表、商家详情、菜品列表、菜品详情接口
 */
@RestController
@RequestMapping("/api")
public class MenuController {

    @Autowired
    private MenuService menuService;

    /**
     * 商家列表（支持分类与关键词筛选）
     */
    @GetMapping("/merchants")
    public Result<List<Merchant>> listMerchants(@RequestParam(required = false) String category,
                                                @RequestParam(required = false) String keyword) {
        return Result.success(menuService.listMerchants(category, keyword));
    }

    /**
     * 好评商家列表（首页"好评商家"板块专用）
     */
    @GetMapping("/merchants/recommended")
    public Result<List<Merchant>> listRecommendedMerchants() {
        return Result.success(menuService.listRecommendedMerchants());
    }

    /**
     * 商家详情（含优惠与满减规则）
     */
    @GetMapping("/merchant/{id}")
    public Result<MerchantDetailVO> getMerchantDetail(@PathVariable Long id) {
        return Result.success(menuService.getMerchantDetail(id));
    }

    /**
     * 商家菜品列表（折扣优先、按销量降序）
     */
    @GetMapping("/dishes")
    public Result<List<Dish>> listDishes(@RequestParam Long merchantId) {
        return Result.success(menuService.listDishes(merchantId));
    }

    /**
     * 人气菜品聚合列表（跨商家，仅上架且 is_popular=1，按销量降序）
     */
    @GetMapping("/dishes/popular")
    public Result<List<Dish>> listPopularDishes() {
        return Result.success(menuService.listPopularDishes());
    }

    /**
     * 菜品详情（含规格选项）
     */
    @GetMapping("/dishes/{id}")
    public Result<DishDetailVO> getDishDetail(@PathVariable Long id) {
        return Result.success(menuService.getDishDetail(id));
    }
}
