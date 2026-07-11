package com.qiukai.controller;

import com.qiukai.common.Result;
import com.qiukai.dto.DishDTO;
import com.qiukai.dto.MerchantDTO;
import com.qiukai.entity.Dish;
import com.qiukai.entity.Merchant;
import com.qiukai.entity.Order;
import com.qiukai.service.AdminService;
import com.qiukai.service.OrderService;
import com.qiukai.vo.OrderDetailVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 后台管理控制器
 * 系统级管理员可管理全部商家、菜品与订单
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private OrderService orderService;

    // ==================== 商家管理 ====================

    /**
     * 全部商家列表（含休息状态）
     */
    @GetMapping("/merchants")
    public Result<List<Merchant>> listMerchants() {
        return Result.success(adminService.listMerchants());
    }

    /**
     * 新增商家
     */
    @PostMapping("/merchants")
    public Result<Void> addMerchant(@Valid @RequestBody MerchantDTO dto) {
        adminService.addMerchant(dto);
        return Result.success("商家添加成功", null);
    }

    /**
     * 修改商家信息
     */
    @PutMapping("/merchants/{id}")
    public Result<Void> updateMerchant(@PathVariable Long id, @Valid @RequestBody MerchantDTO dto) {
        adminService.updateMerchant(id, dto);
        return Result.success("商家信息已更新", null);
    }

    /**
     * 删除商家（同时删除其菜品）
     */
    @DeleteMapping("/merchants/{id}")
    public Result<Void> deleteMerchant(@PathVariable Long id) {
        adminService.deleteMerchant(id);
        return Result.success("商家已删除", null);
    }

    /**
     * 切换好评商家状态
     */
    @PutMapping("/merchants/{id}/recommended")
    public Result<Void> toggleRecommended(@PathVariable Long id) {
        adminService.toggleRecommended(id);
        return Result.success("好评状态已切换", null);
    }

    /**
     * 批量设置好评商家状态
     * body: { "ids": [1,2,3], "isRecommended": 1 }
     */
    @PutMapping("/merchants/batch-recommended")
    public Result<Void> batchToggleRecommended(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> idInts = (List<Integer>) body.get("ids");
        if (idInts == null || idInts.isEmpty()) {
            return Result.error("请选择至少一个商家");
        }
        List<Long> ids = idInts.stream().map(Integer::longValue).toList();
        Integer isRecommended = (Integer) body.get("isRecommended");
        adminService.batchToggleRecommended(ids, isRecommended);
        return Result.success("批量设置完成", null);
    }

    // ==================== 菜品管理 ====================

    /**
     * 菜品列表（merchantId 为空时返回全部菜品）
     */
    @GetMapping("/dishes")
    public Result<List<Dish>> listDishes(@RequestParam(required = false) Long merchantId) {
        return Result.success(adminService.listDishes(merchantId));
    }

    /**
     * 新增菜品
     */
    @PostMapping("/dishes")
    public Result<Void> addDish(@Valid @RequestBody DishDTO dto) {
        adminService.addDish(dto);
        return Result.success("菜品添加成功", null);
    }

    /**
     * 修改菜品
     */
    @PutMapping("/dishes/{id}")
    public Result<Void> updateDish(@PathVariable Long id, @Valid @RequestBody DishDTO dto) {
        adminService.updateDish(id, dto);
        return Result.success("菜品更新成功", null);
    }

    /**
     * 删除菜品
     */
    @DeleteMapping("/dishes/{id}")
    public Result<Void> deleteDish(@PathVariable Long id) {
        adminService.deleteDish(id);
        return Result.success("菜品已删除", null);
    }

    /**
     * 切换菜品上下架
     */
    @PutMapping("/dishes/{id}/shelf")
    public Result<Void> toggleShelf(@PathVariable Long id) {
        adminService.toggleShelf(id);
        return Result.success("上下架状态已切换", null);
    }

    /**
     * 切换人气菜品标记
     */
    @PutMapping("/dishes/{id}/popular")
    public Result<Void> togglePopular(@PathVariable Long id) {
        adminService.togglePopular(id);
        return Result.success("人气标记已切换", null);
    }

    /**
     * 切换菜品售罄状态
     */
    @PutMapping("/dishes/{id}/sold-out")
    public Result<Void> toggleSoldOut(@PathVariable Long id) {
        adminService.toggleSoldOut(id);
        return Result.success("售罄状态已切换", null);
    }

    // ==================== 订单管理 ====================

    /**
     * 全部订单列表（可按状态筛选）
     */
    @GetMapping("/orders")
    public Result<List<Order>> listOrders(@RequestParam(required = false) String status) {
        return Result.success(orderService.listAllOrders(status));
    }

    /**
     * 订单详情
     */
    @GetMapping("/orders/{id}")
    public Result<OrderDetailVO> orderDetail(@PathVariable Long id) {
        return Result.success(orderService.adminGetOrderDetail(id));
    }

    /**
     * 分配订单给商家：已支付 -> 待确认（订单流转至商家后台）
     */
    @PutMapping("/orders/{id}/assign")
    public Result<Void> assignOrder(@PathVariable Long id) {
        orderService.assignOrder(id);
        return Result.success("订单已确认并流转至商家", null);
    }
}
