package com.qiukai.controller;

import com.qiukai.common.Result;
import com.qiukai.dto.*;
import com.qiukai.entity.Address;
import com.qiukai.entity.BankCard;
import com.qiukai.entity.Merchant;
import com.qiukai.entity.Review;
import com.qiukai.service.UserService;
import com.qiukai.vo.LoginVO;
import com.qiukai.vo.UserVO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 * 提供注册、登录、个人信息、银行卡、收货地址、收藏、评价等接口
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    // ==================== 注册 / 登录 ====================

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success("注册成功", null);
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletResponse response) {
        LoginVO vo = userService.login(dto);
        setTokenCookie(response, vo.getToken());
        return Result.success("登录成功", vo);
    }

    @PostMapping("/admin/login")
    public Result<LoginVO> adminLogin(@Valid @RequestBody LoginDTO dto, HttpServletResponse response) {
        LoginVO vo = userService.adminLogin(dto);
        setTokenCookie(response, vo.getToken());
        return Result.success("登录成功", vo);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success("已退出登录", null);
    }

    // ==================== 个人信息 ====================

    @GetMapping("/info")
    public Result<UserVO> info() {
        return Result.success(userService.getCurrentUser());
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        return Result.success(userService.updateProfile(dto));
    }

    @PutMapping("/avatar")
    public Result<UserVO> updateAvatar(@RequestParam String avatar) {
        return Result.success(userService.updateAvatar(avatar));
    }

    // ==================== 银行卡 ====================

    @GetMapping("/bank-cards")
    public Result<List<BankCard>> listBankCards() {
        return Result.success(userService.listBankCards());
    }

    @PostMapping("/bank-cards")
    public Result<Void> bindBankCard(@Valid @RequestBody BankCardDTO dto) {
        userService.bindBankCard(dto);
        return Result.success("银行卡绑定成功", null);
    }

    @DeleteMapping("/bank-cards/{id}")
    public Result<Void> unbindBankCard(@PathVariable Long id) {
        userService.unbindBankCard(id);
        return Result.success("银行卡已解绑", null);
    }

    @PutMapping("/bank-cards/{id}/default")
    public Result<Void> setDefaultBankCard(@PathVariable Long id) {
        userService.setDefaultBankCard(id);
        return Result.success("已设为默认银行卡", null);
    }

    // ==================== 收货地址 ====================

    @GetMapping("/addresses")
    public Result<List<Address>> listAddresses() {
        return Result.success(userService.listAddresses());
    }

    @PostMapping("/addresses")
    public Result<Void> addAddress(@Valid @RequestBody AddressDTO dto) {
        userService.addAddress(dto);
        return Result.success("地址添加成功", null);
    }

    @PutMapping("/addresses/{id}")
    public Result<Void> updateAddress(@PathVariable Long id, @Valid @RequestBody AddressDTO dto) {
        userService.updateAddress(id, dto);
        return Result.success("地址更新成功", null);
    }

    @DeleteMapping("/addresses/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id) {
        userService.deleteAddress(id);
        return Result.success("地址已删除", null);
    }

    // ==================== 收藏 ====================

    @GetMapping("/favorites")
    public Result<List<Merchant>> listFavorites() {
        return Result.success(userService.listFavorites());
    }

    @PostMapping("/favorites/{merchantId}")
    public Result<Void> addFavorite(@PathVariable Long merchantId) {
        userService.addFavorite(merchantId);
        return Result.success("已收藏", null);
    }

    @DeleteMapping("/favorites/{merchantId}")
    public Result<Void> removeFavorite(@PathVariable Long merchantId) {
        userService.removeFavorite(merchantId);
        return Result.success("已取消收藏", null);
    }

    // ==================== 评价 ====================

    @PostMapping("/reviews")
    public Result<Review> addReview(@Valid @RequestBody ReviewDTO dto) {
        Review review = userService.addReview(dto);
        return Result.success("评价提交成功", review);
    }

    @GetMapping("/reviews")
    public Result<List<Review>> listMyReviews() {
        return Result.success(userService.listMyReviews());
    }

    @GetMapping("/reviews/{id}")
    public Result<Review> getReview(@PathVariable Long id) {
        return Result.success(userService.getReview(id));
    }

    @PutMapping("/reviews/{id}")
    public Result<Review> updateReview(@PathVariable Long id, @Valid @RequestBody ReviewDTO dto) {
        Review review = userService.updateReview(id, dto);
        return Result.success("评价更新成功", review);
    }

    @DeleteMapping("/reviews/{id}")
    public Result<Void> deleteReview(@PathVariable Long id) {
        userService.deleteReview(id);
        return Result.success("评价已删除", null);
    }

    // ==================== 工具 ====================

    /**
     * 将 token 写入 HttpOnly Cookie，支撑 30 天自动登录
     */
    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("kazi_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 30);
        response.addCookie(cookie);
    }
}
