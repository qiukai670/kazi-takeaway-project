package com.qiukai.mapper;

import com.qiukai.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper {

    int insert(Order order);

    Order selectById(@Param("id") Long id);

    Order selectByOrderNo(@Param("orderNo") String orderNo);

    /** 用户订单列表（可按状态筛选） */
    List<Order> selectByUserId(@Param("userId") Long userId, @Param("status") String status);

    /** 商家订单列表（可按状态筛选） */
    List<Order> selectByMerchantId(@Param("merchantId") Long merchantId, @Param("status") String status);

    /** 全部订单（管理员，可按状态筛选） */
    List<Order> selectAll(@Param("status") String status);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int updateNote(@Param("id") Long id, @Param("note") String note);

    int updatePay(@Param("id") Long id, @Param("status") String status, @Param("payMethod") String payMethod);
}
