package com.qiukai.controller;

import com.qiukai.common.Result;
import com.qiukai.dto.DishDTO;
import com.qiukai.dto.MerchantDTO;
import com.qiukai.entity.*;
import com.qiukai.service.MerchantService;
import com.qiukai.vo.OrderDetailVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家后台控制器
 * 商家管理自有店铺信息、菜品、订单与优惠策略
 */
@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    @Autowired
    private MerchantService merchantService;

    // ==================== 店铺信息 ====================

    @GetMapping("/info")
    public Result<Merchant> getMyMerchant() {
        return Result.success(merchantService.getMyMerchant());
    }

    @PutMapping("/info")
    public Result<Void> updateMyMerchant(@Valid @RequestBody MerchantDTO dto) {
        merchantService.updateMyMerchant(dto);
        return Result.success("店铺信息已更新", null);
    }

    // ==================== 菜品管理 ====================

    @GetMapping("/dishes")
    public Result<List<Dish>> listMyDishes() {
        return Result.success(merchantService.listMyDishes());
    }

    @PostMapping("/dishes")
    public Result<Void> addMyDish(@Valid @RequestBody DishDTO dto) {
        merchantService.addMyDish(dto);
        return Result.success("菜品添加成功", null);
    }

    @PutMapping("/dishes/{id}")
    public Result<Void> updateMyDish(@PathVariable Long id, @Valid @RequestBody DishDTO dto) {
        merchantService.updateMyDish(id, dto);
        return Result.success("菜品更新成功", null);
    }

    @DeleteMapping("/dishes/{id}")
    public Result<Void> deleteMyDish(@PathVariable Long id) {
        merchantService.deleteMyDish(id);
        return Result.success("菜品已删除", null);
    }

    @PutMapping("/dishes/{id}/shelf")
    public Result<Void> toggleMyDishShelf(@PathVariable Long id) {
        merchantService.toggleMyDishShelf(id);
        return Result.success("上下架状态已切换", null);
    }

    @PutMapping("/dishes/{id}/sold-out")
    public Result<Void> toggleMyDishSoldOut(@PathVariable Long id) {
        merchantService.toggleMyDishSoldOut(id);
        return Result.success("售罄状态已切换", null);
    }

    // ==================== 订单管理 ====================

    @GetMapping("/orders")
    public Result<List<Order>> listMyOrders(@RequestParam(required = false) String status) {
        return Result.success(merchantService.listMyOrders(status));
    }

    @GetMapping("/orders/{id}")
    public Result<OrderDetailVO> getMyOrderDetail(@PathVariable Long id) {
        return Result.success(merchantService.getMyOrderDetail(id));
    }

    @PutMapping("/orders/{id}/confirm")
    public Result<Void> confirmMyOrder(@PathVariable Long id) {
        merchantService.confirmMyOrder(id);
        return Result.success("已接单", null);
    }

    @PutMapping("/orders/{id}/dispatch")
    public Result<Void> dispatchMyOrder(@PathVariable Long id) {
        merchantService.dispatchMyOrder(id);
        return Result.success("已开始派送", null);
    }

    // ==================== 优惠管理 - MerchantPromo ====================

    @GetMapping("/promos")
    public Result<List<MerchantPromo>> listMyPromos() {
        return Result.success(merchantService.listMyPromos());
    }

    @PostMapping("/promos")
    public Result<Void> addMyPromo(@RequestBody MerchantPromo promo) {
        merchantService.addMyPromo(promo);
        return Result.success("优惠添加成功", null);
    }

    @PutMapping("/promos/{id}")
    public Result<Void> updateMyPromo(@PathVariable Long id, @RequestBody MerchantPromo promo) {
        merchantService.updateMyPromo(id, promo);
        return Result.success("优惠已更新", null);
    }

    @PutMapping("/promos/{id}/status")
    public Result<Void> toggleMyPromoStatus(@PathVariable Long id) {
        merchantService.toggleMyPromoStatus(id);
        return Result.success("优惠状态已切换", null);
    }

    @DeleteMapping("/promos/{id}")
    public Result<Void> deleteMyPromo(@PathVariable Long id) {
        merchantService.deleteMyPromo(id);
        return Result.success("优惠已删除", null);
    }

    // ==================== 满减规则 - PromoRule ====================

    @GetMapping("/promo-rules")
    public Result<List<PromoRule>> listMyPromoRules() {
        return Result.success(merchantService.listMyPromoRules());
    }

    @PostMapping("/promo-rules")
    public Result<Void> addMyPromoRule(@RequestBody PromoRule rule) {
        merchantService.addMyPromoRule(rule);
        return Result.success("满减规则添加成功", null);
    }

    @PutMapping("/promo-rules/{id}")
    public Result<Void> updateMyPromoRule(@PathVariable Long id, @RequestBody PromoRule rule) {
        merchantService.updateMyPromoRule(id, rule);
        return Result.success("满减规则已更新", null);
    }

    @PutMapping("/promo-rules/{id}/status")
    public Result<Void> toggleMyPromoRuleStatus(@PathVariable Long id) {
        merchantService.toggleMyPromoRuleStatus(id);
        return Result.success("规则状态已切换", null);
    }

    @DeleteMapping("/promo-rules/{id}")
    public Result<Void> deleteMyPromoRule(@PathVariable Long id) {
        merchantService.deleteMyPromoRule(id);
        return Result.success("满减规则已删除", null);
    }
}
