package com.qiukai.mapper;

import com.qiukai.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单明细 Mapper
 */
@Mapper
public interface OrderItemMapper {

    int insertBatch(@Param("items") List<OrderItem> items);

    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);
}
