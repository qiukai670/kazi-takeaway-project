package com.qiukai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址请求参数
 */
@Data
public class AddressDTO {

    @NotBlank(message = "收件人不能为空")
    @Size(min = 2, max = 32, message = "收件人姓名长度需为2-32位")
    private String name;

    @NotBlank(message = "电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "所在地区不能为空")
    private String region;

    @NotBlank(message = "详细地址不能为空")
    private String detail;

    /** home 家 company 公司 school 学校 */
    private String tag;

    /** 是否设为默认 */
    private Boolean isDefault;
}
