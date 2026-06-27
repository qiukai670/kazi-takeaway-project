package com.qiukai.controller;

import com.qiukai.common.Result;
import com.qiukai.dto.CartItemDTO;
import com.qiukai.service.CartService;
import com.qiukai.vo.CartVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 购物车控制器
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 加入购物车
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody CartItemDTO dto) {
        cartService.addToCart(dto);
        return Result.success("已加入购物车", null);
    }

    /**
     * 修改购物车项数量
     */
    @PutMapping("/{id}/qty")
    public Result<Void> updateQty(@PathVariable Long id, @RequestParam Integer qty) {
        cartService.updateQty(id, qty);
        return Result.success("数量已更新", null);
    }

    /**
     * 删除购物车项
     */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        cartService.removeItem(id);
        return Result.success("已移除", null);
    }

    /**
     * 清空指定商家的购物车
     */
    @DeleteMapping("/merchant/{merchantId}")
    public Result<Void> clear(@PathVariable Long merchantId) {
        cartService.clearCart(merchantId);
        return Result.success("购物车已清空", null);
    }

    /**
     * 获取购物车汇总（含实时金额）
     */
    @GetMapping
    public Result<CartVO> getCart(@RequestParam Long merchantId) {
        return Result.success(cartService.getCart(merchantId));
    }
}
