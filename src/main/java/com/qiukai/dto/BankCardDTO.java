package com.qiukai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 银行卡绑定请求参数
 */
@Data
public class BankCardDTO {

    @NotBlank(message = "持卡人姓名不能为空")
    @Size(max = 32, message = "持卡人姓名最长32位")
    private String holder;

    @NotBlank(message = "卡号不能为空")
    @Pattern(regexp = "^\\d{16,19}$", message = "卡号须为16-19位数字")
    private String cardNumber;

    @NotBlank(message = "所属银行不能为空")
    private String bankName;

    /** debit 储蓄卡 credit 信用卡 */
    @NotBlank(message = "卡类型不能为空")
    private String cardType;

    @Pattern(regexp = "^\\d{2}/\\d{2}$", message = "有效期格式须为 MM/YY")
    private String expireDate;

    @Pattern(regexp = "^\\d{3}$", message = "CVV须为3位数字")
    private String cvv;

    /** 是否设为默认 */
    private Boolean isDefault;
}
