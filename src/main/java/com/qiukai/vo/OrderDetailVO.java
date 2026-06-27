package com.qiukai.vo;

import com.qiukai.entity.Order;
import com.qiukai.entity.OrderItem;
import lombok.Data;

import java.util.List;

/**
 * 订单详情响应（含订单明细）
 */
@Data
public class OrderDetailVO {

    private Order order;
    private List<OrderItem> items;
}
