package com.qiukai.controller;

import com.qiukai.common.Result;
import com.qiukai.dto.OrderCreateDTO;
import com.qiukai.entity.Order;
import com.qiukai.service.OrderService;
import com.qiukai.vo.OrderDetailVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器（用户侧）
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping
    public Result<Order> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success("订单创建成功", orderService.createOrder(dto));
    }

    /**
     * 支付订单
     */
    @PutMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id, @RequestParam(required = false) String payMethod) {
        orderService.payOrder(id, payMethod);
        return Result.success("支付成功", null);
    }

    /**
     * 修改订单留言
     */
    @PutMapping("/{id}/note")
    public Result<Void> updateNote(@PathVariable Long id, @RequestParam String note) {
        orderService.updateNote(id, note);
        return Result.success("留言已更新", null);
    }

    /**
     * 确认收货
     */
    @PutMapping("/{id}/confirm")
    public Result<Void> confirmReceipt(@PathVariable Long id) {
        orderService.confirmReceipt(id);
        return Result.success("已确认收货", null);
    }

    /**
     * 取消订单
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.success("订单已取消", null);
    }

    /**
     * 用户订单列表
     */
    @GetMapping
    public Result<List<Order>> list(@RequestParam(required = false) String status) {
        return Result.success(orderService.listUserOrders(status));
    }

    /**
     * 订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(orderService.getOrderDetail(id));
    }
}
