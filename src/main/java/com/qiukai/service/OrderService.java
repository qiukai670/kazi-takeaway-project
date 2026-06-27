package com.qiukai.service;

import com.qiukai.common.BusinessException;
import com.qiukai.common.OrderStatus;
import com.qiukai.dto.OrderCreateDTO;
import com.qiukai.entity.*;
import com.qiukai.interceptor.UserContext;
import com.qiukai.mapper.*;
import com.qiukai.vo.OrderDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务
 * 提供订单创建（含满减优惠计算）、支付、留言、状态流转管理
 */
@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private MenuService menuService;
    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private UserMapper userMapper;

    private static final int POINTS_PER_YUAN = 1;

    /**
     * 创建订单
     * 1. 校验购物车非空
     * 2. 计算菜品小计
     * 3. 按满减规则计算优惠
     * 4. 计算实付金额 = 小计 - 优惠 + 配送费
     * 5. 生成订单与明细，清空购物车，增加菜品销量
     */
    @Transactional
    public Order createOrder(OrderCreateDTO dto) {
        Long userId = requireLogin();
        List<Cart> cartItems = cartMapper.selectByUserAndMerchant(userId, dto.getMerchantId());
        if (cartItems.isEmpty()) {
            throw new BusinessException("购物车为空，无法下单");
        }

        Merchant merchant = menuService.getMerchant(dto.getMerchantId());

        // 计算小计
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Cart item : cartItems) {
            subtotal = subtotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQty())));
        }

        // 满减优惠计算：取满足条件的最高档优惠
        BigDecimal discount = computeDiscount(dto.getMerchantId(), subtotal);

        // 配送费
        BigDecimal deliveryFee = merchant.getDeliveryFee();

        // 实付金额
        BigDecimal totalAmount = subtotal.subtract(discount).add(deliveryFee);

        // 收货地址快照
        String addressSnapshot = buildAddressSnapshot(userId, dto.getAddressId());

        // 构建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setMerchantId(dto.getMerchantId());
        order.setMerchantName(merchant.getName());
        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING_PAY);
        order.setNote(dto.getNote());
        order.setPayMethod(dto.getPayMethod());
        order.setAddressSnapshot(addressSnapshot);
        orderMapper.insert(order);

        // 构建订单明细
        List<OrderItem> items = new ArrayList<>();
        for (Cart cart : cartItems) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setDishId(cart.getDishId());
            item.setDishName(cart.getDishName());
            item.setDishImage(cart.getDishImage());
            item.setUnitPrice(cart.getUnitPrice());
            item.setQty(cart.getQty());
            item.setOptionsText(cart.getOptionsText());
            item.setSubtotal(cart.getUnitPrice().multiply(BigDecimal.valueOf(cart.getQty())));
            items.add(item);
        }
        orderItemMapper.insertBatch(items);

        // 增加菜品销量
        for (Cart cart : cartItems) {
            dishMapper.increaseSales(cart.getDishId(), cart.getQty());
        }

        // 清空该商家购物车
        cartMapper.deleteByUserAndMerchant(userId, dto.getMerchantId());

        return order;
    }

    /**
     * 满减优惠计算：在满足门槛的规则中取优惠金额最大的一档
     */
    private BigDecimal computeDiscount(Long merchantId, BigDecimal subtotal) {
        List<PromoRule> rules = menuService.getPromoRules(merchantId);
        BigDecimal bestDiscount = BigDecimal.ZERO;
        for (PromoRule rule : rules) {
            if (subtotal.compareTo(rule.getThreshold()) >= 0
                    && rule.getDiscount().compareTo(bestDiscount) > 0) {
                bestDiscount = rule.getDiscount();
            }
        }
        return bestDiscount;
    }

    /**
     * 构建收货地址快照
     */
    private String buildAddressSnapshot(Long userId, Long addressId) {
        Address address = null;
        if (addressId != null) {
            address = addressMapper.selectById(addressId);
        }
        if (address == null) {
            // 取默认地址
            List<Address> addresses = addressMapper.selectByUserId(userId);
            if (!addresses.isEmpty()) {
                address = addresses.get(0);
            }
        }
        if (address == null) {
            return null;
        }
        return address.getName() + " " + address.getPhone() + " " + address.getRegion() + " " + address.getDetail();
    }

    /**
     * 生成订单号：KZ + yyyyMMddHHmmss + 3位随机数
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(100, 1000);
        return "KZ" + timestamp + random;
    }

    /**
     * 支付订单：待支付 -> 已支付
     */
    @Transactional
    public void payOrder(Long orderId, String payMethod) {
        Long userId = requireLogin();
        Order order = getOrderAndCheck(orderId, userId);
        if (!OrderStatus.PENDING_PAY.equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不可支付");
        }
        orderMapper.updatePay(orderId, OrderStatus.PAID, payMethod);
    }

    /**
     * 添加/修改订单留言（商家与骑手可见）
     */
    @Transactional
    public void updateNote(Long orderId, String note) {
        Long userId = requireLogin();
        Order order = getOrderAndCheck(orderId, userId);
        if (OrderStatus.COMPLETED.equals(order.getStatus()) || OrderStatus.CANCELLED.equals(order.getStatus())) {
            throw new BusinessException("订单已结束，无法修改留言");
        }
        orderMapper.updateNote(orderId, note);
    }

    /**
     * 用户确认收货：配送中 -> 已完成
     */
    @Transactional
    public void confirmReceipt(Long orderId) {
        Long userId = requireLogin();
        Order order = getOrderAndCheck(orderId, userId);
        if (!OrderStatus.DELIVERING.equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不可确认收货");
        }
        orderMapper.updateStatus(orderId, OrderStatus.COMPLETED);
        BigDecimal totalAmount = order.getTotalAmount();
        if (totalAmount != null) {
            int points = totalAmount.intValue() * POINTS_PER_YUAN;
            if (points > 0) {
                userMapper.addPoints(userId, points);
            }
        }
    }

    /**
     * 用户取消订单：仅待支付可取消
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Long userId = requireLogin();
        Order order = getOrderAndCheck(orderId, userId);
        if (!OrderStatus.PENDING_PAY.equals(order.getStatus())) {
            throw new BusinessException("仅待支付订单可取消");
        }
        orderMapper.updateStatus(orderId, OrderStatus.CANCELLED);
    }

    /**
     * 用户订单列表（可按状态筛选）
     */
    public List<Order> listUserOrders(String status) {
        Long userId = requireLogin();
        return orderMapper.selectByUserId(userId, status);
    }

    /**
     * 订单详情（含明细）
     */
    public OrderDetailVO getOrderDetail(Long orderId) {
        Long userId = requireLogin();
        Order order = getOrderAndCheck(orderId, userId);
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(order);
        vo.setItems(orderItemMapper.selectByOrderId(orderId));
        return vo;
    }

    // ==================== 管理员操作 ====================

    /**
     * 管理员分配订单给商家：已支付 -> 待确认
     */
    @Transactional
    public void assignOrder(Long orderId) {
        requireAdmin();
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!OrderStatus.PAID.equals(order.getStatus())) {
            throw new BusinessException("仅已支付订单可分配");
        }
        orderMapper.updateStatus(orderId, OrderStatus.PENDING_CONFIRM);
    }

    /**
     * 管理员确认订单：待确认 -> 已确认
     */
    @Transactional
    public void confirmOrder(Long orderId) {
        requireAdmin();
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!OrderStatus.PENDING_CONFIRM.equals(order.getStatus()) && !OrderStatus.PAID.equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不可确认");
        }
        orderMapper.updateStatus(orderId, OrderStatus.CONFIRMED);
    }

    /**
     * 管理员派送：已确认 -> 配送中
     */
    @Transactional
    public void dispatchOrder(Long orderId) {
        requireAdmin();
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!OrderStatus.CONFIRMED.equals(order.getStatus())) {
            throw new BusinessException("仅已确认订单可派送");
        }
        orderMapper.updateStatus(orderId, OrderStatus.DELIVERING);
    }

    /**
     * 管理员订单列表（可按状态筛选）
     */
    public List<Order> listAllOrders(String status) {
        requireAdmin();
        return orderMapper.selectAll(status);
    }

    /**
     * 管理员订单详情
     */
    public OrderDetailVO adminGetOrderDetail(Long orderId) {
        requireAdmin();
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(order);
        vo.setItems(orderItemMapper.selectByOrderId(orderId));
        return vo;
    }

    // ==================== 工具 ====================

    private Order getOrderAndCheck(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private Long requireLogin() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }

    private void requireAdmin() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!UserContext.isAdmin()) {
            throw new BusinessException(403, "无管理员权限");
        }
    }
}
