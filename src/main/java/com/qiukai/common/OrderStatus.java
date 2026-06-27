package com.qiukai.common;

/**
 * 订单状态常量
 * 状态流转：待支付 -> 已支付 -> 待确认 -> 已确认 -> 配送中 -> 已完成
 */
public final class OrderStatus {

    private OrderStatus() {
    }

    /** 待支付 */
    public static final String PENDING_PAY = "PENDING_PAY";
    /** 已支付 */
    public static final String PAID = "PAID";
    /** 待确认（已分配商家，等待商家确认） */
    public static final String PENDING_CONFIRM = "PENDING_CONFIRM";
    /** 已确认（商家已接单，制作中） */
    public static final String CONFIRMED = "CONFIRMED";
    /** 配送中 */
    public static final String DELIVERING = "DELIVERING";
    /** 已完成 */
    public static final String COMPLETED = "COMPLETED";
    /** 已取消 */
    public static final String CANCELLED = "CANCELLED";
}
