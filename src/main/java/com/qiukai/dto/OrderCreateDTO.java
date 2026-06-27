package com.qiukai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建订单请求参数
 */
@Data
public class OrderCreateDTO {

    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    /** 订单留言（商家和骑手可见） */
    @Size(max = 200, message = "留言最长200字")
    private String note;

    /** 支付方式 wechat alipay bank */
    private String payMethod;

    /** 收货地址ID（取默认地址或指定地址） */
    private Long addressId;
}
