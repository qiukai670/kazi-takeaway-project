package com.qiukai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人信息请求参数
 */
@Data
public class UpdateProfileDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需为3-20位")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称最长32位")
    private String nickname;

    /** 性别 0保密 1男 2女 */
    private Integer gender;

    /** 头像URL */
    private String avatar;
}
