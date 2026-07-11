package com.qiukai.controller;

import com.qiukai.common.Result;
import com.qiukai.dto.DishDTO;
import com.qiukai.dto.MerchantDTO;
import com.qiukai.entity.*;
import com.qiukai.service.ShopService;
import com.qiukai.vo.OrderDetailVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家后台控制器
 * 所有端点需登录态（/api/shop/** 不在 AuthInterceptor 放行列表），
 * 服务层内部校验商家身份与数据归属
 */
@RestController
@RequestMapping("/api/shop")
public class ShopController {

    @Autowired
    private ShopService shopService;

    // ==================== 店铺管理 ====================

    /** 查询当前商家店铺信息 */
    @GetMapping("/merchant")
    public Result<Merchant> getMerchant() {
        return Result.success(shopService.getMyMerchant());
    }

    /** 修改店铺信息（禁止改 isRecommended 和 badge） */
    @PutMapping("/merchant")
    public Result<Void> updateMerchant(@Valid @RequestBody MerchantDTO dto) {
        shopService.updateMyMerchant(dto);
        return Result.success("店铺信息已更新", null);
    }

    // ==================== 菜品管理 ====================

    @GetMapping("/dishes")
    public Result<List<Dish>> listDishes() {
        return Result.success(shopService.listMyDishes());
    }

    @PostMapping("/dishes")
    public Result<Void> addDish(@Valid @RequestBody DishDTO dto) {
        shopService.addMyDish(dto);
        return Result.success("菜品添加成功", null);
    }

    @PutMapping("/dishes/{id}")
    public Result<Void> updateDish(@PathVariable Long id, @Valid @RequestBody DishDTO dto) {
        shopService.updateMyDish(id, dto);
        return Result.success("菜品更新成功", null);
    }

    @DeleteMapping("/dishes/{id}")
    public Result<Void> deleteDish(@PathVariable Long id) {
        shopService.deleteMyDish(id);
        return Result.success("菜品已删除", null);
    }

    @PutMapping("/dishes/{id}/shelf")
    public Result<Void> toggleShelf(@PathVariable Long id) {
        shopService.toggleMyShelf(id);
        return Result.success("上下架状态已切换", null);
    }

    @PutMapping("/dishes/{id}/popular")
    public Result<Void> togglePopular(@PathVariable Long id) {
        shopService.toggleMyPopular(id);
        return Result.success("人气标记已切换", null);
    }

    @PutMapping("/dishes/{id}/sold-out")
    public Result<Void> toggleSoldOut(@PathVariable Long id) {
        shopService.toggleMySoldOut(id);
        return Result.success("售罄状态已切换", null);
    }

    // ==================== 优惠描述管理（MerchantPromo） ====================

    @GetMapping("/promos")
    public Result<List<MerchantPromo>> listPromos() {
        return Result.success(shopService.listMyPromos());
    }

    @PostMapping("/promos")
    public Result<Void> addPromo(@RequestBody MerchantPromo promo) {
        shopService.addMyPromo(promo);
        return Result.success("优惠添加成功", null);
    }

    @PutMapping("/promos/{id}")
    public Result<Void> updatePromo(@PathVariable Long id, @RequestBody MerchantPromo promo) {
        shopService.updateMyPromo(id, promo);
        return Result.success("优惠已更新", null);
    }

    @DeleteMapping("/promos/{id}")
    public Result<Void> deletePromo(@PathVariable Long id) {
        shopService.deleteMyPromo(id);
        return Result.success("优惠已删除", null);
    }

    @PutMapping("/promos/{id}/status")
    public Result<Void> togglePromoStatus(@PathVariable Long id, @RequestParam Integer status) {
        shopService.toggleMyPromoStatus(id, status);
        return Result.success("优惠状态已切换", null);
    }

    // ==================== 满减规则管理（PromoRule） ====================

    @GetMapping("/promo-rules")
    public Result<List<PromoRule>> listPromoRules() {
        return Result.success(shopService.listMyPromoRules());
    }

    @PostMapping("/promo-rules")
    public Result<Void> addPromoRule(@RequestBody PromoRule rule) {
        shopService.addMyPromoRule(rule);
        return Result.success("满减规则添加成功", null);
    }

    @PutMapping("/promo-rules/{id}")
    public Result<Void> updatePromoRule(@PathVariable Long id, @RequestBody PromoRule rule) {
        shopService.updateMyPromoRule(id, rule);
        return Result.success("满减规则已更新", null);
    }

    @DeleteMapping("/promo-rules/{id}")
    public Result<Void> deletePromoRule(@PathVariable Long id) {
        shopService.deleteMyPromoRule(id);
        return Result.success("满减规则已删除", null);
    }

    @PutMapping("/promo-rules/{id}/status")
    public Result<Void> togglePromoRuleStatus(@PathVariable Long id, @RequestParam Integer status) {
        shopService.toggleMyPromoRuleStatus(id, status);
        return Result.success("规则状态已切换", null);
    }

    // ==================== 订单管理 ====================

    @GetMapping("/orders")
    public Result<List<Order>> listOrders(@RequestParam(required = false) String status) {
        return Result.success(shopService.listMyOrders(status));
    }

    @GetMapping("/orders/{id}")
    public Result<OrderDetailVO> orderDetail(@PathVariable Long id) {
        return Result.success(shopService.getMyOrderDetail(id));
    }

    /** 商家接单：待确认 -> 已确认 */
    @PutMapping("/orders/{id}/confirm")
    public Result<Void> confirmOrder(@PathVariable Long id) {
        shopService.confirmMyOrder(id);
        return Result.success("已接单", null);
    }

    /** 商家派送：已确认 -> 配送中 */
    @PutMapping("/orders/{id}/dispatch")
    public Result<Void> dispatchOrder(@PathVariable Long id) {
        shopService.dispatchMyOrder(id);
        return Result.success("已派送", null);
    }
}
