package com.qiukai.service;

import com.qiukai.common.BusinessException;
import com.qiukai.dto.*;
import com.qiukai.entity.*;
import com.qiukai.interceptor.UserContext;
import com.qiukai.mapper.*;
import com.qiukai.util.PasswordUtil;
import com.qiukai.util.TokenUtil;
import com.qiukai.vo.LoginVO;
import com.qiukai.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务
 * 涵盖注册、登录、令牌管理、个人信息、银行卡、收货地址、收藏、评价
 */
@Service
public class UserService {

    /** 30 天自动登录有效期 */
    private static final int TOKEN_EXPIRE_DAYS = 30;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserTokenMapper userTokenMapper;
    @Autowired
    private BankCardMapper bankCardMapper;
    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private FavoriteMapper favoriteMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private ReviewItemMapper reviewItemMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private MerchantMapper merchantMapper;

    // ==================== 注册 ====================

    /**
     * 用户注册：基于手机号，密码 PBKDF2 加密存储
     */
    @Transactional
    public void register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (userMapper.selectByPhone(dto.getPhone()) != null) {
            throw new BusinessException("该手机号已注册");
        }
        if (userMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BusinessException("该用户名已存在");
        }
        User user = new User();
        user.setPhone(dto.getPhone());
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        user.setAvatar(dto.getAvatar());
        user.setGender(0);
        user.setRole(0);
        user.setPoints(0);
        user.setStatus(0);
        userMapper.insert(user);
    }

    // ==================== 登录 ====================

    /**
     * 用户登录：手机号 + 密码，返回 token（30天自动登录）
     */
    @Transactional
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectByPhone(dto.getAccount());
        if (user == null) {
            // 兼容用户名登录
            user = userMapper.selectByUsername(dto.getAccount());
        }
        if (user == null || user.getRole() != 0) {
            throw new BusinessException("账号或密码错误");
        }
        if (user.getStatus() != 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        return buildLoginVO(user, dto.getRememberMe());
    }

    /**
     * 管理员登录：用户名 + 密码
     */
    @Transactional
    public LoginVO adminLogin(LoginDTO dto) {
        User user = userMapper.selectByUsername(dto.getAccount());
        if (user == null || user.getRole() != 1) {
            throw new BusinessException("管理员账号或密码错误");
        }
        if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("管理员账号或密码错误");
        }
        return buildLoginVO(user, true);
    }

    /**
     * 商家注册：创建 role=2 用户 + 关联商家记录
     */
    @Transactional
    public void merchantRegister(MerchantRegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (userMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BusinessException("该用户名已存在");
        }
        if (userMapper.selectByPhone(dto.getPhone()) != null) {
            throw new BusinessException("该手机号已注册");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getRegistrantName());
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setGender(0);
        user.setRole(2);
        user.setPoints(0);
        user.setStatus(0);
        userMapper.insert(user);

        Merchant m = new Merchant();
        m.setUserId(user.getId());
        m.setName(dto.getMerchantName());
        m.setLogo(dto.getLogo());
        m.setCover(dto.getCover());
        m.setCategory(dto.getCategory());
        m.setPriceLevel("¥¥");
        m.setRating(new BigDecimal("4.8"));
        m.setSales(0);
        m.setDeliveryTime(30);
        m.setDistance(new BigDecimal("1.0"));
        m.setMinOrder(new BigDecimal("20.00"));
        m.setDeliveryFee(new BigDecimal("3.00"));
        m.setStatus(1);
        m.setIsRecommended(0);
        merchantMapper.insert(m);
    }

    /**
     * 商家登录：支持手机号或用户名 + 密码，role=2
     */
    @Transactional
    public LoginVO merchantLogin(LoginDTO dto) {
        User user = userMapper.selectByPhone(dto.getAccount());
        if (user == null) {
            user = userMapper.selectByUsername(dto.getAccount());
        }
        if (user == null || user.getRole() != 2) {
            throw new BusinessException("商家账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("商家账号或密码错误");
        }
        return buildLoginVO(user, dto.getRememberMe());
    }

    /**
     * 构建登录响应并签发 token
     */
    private LoginVO buildLoginVO(User user, Boolean rememberMe) {
        // 登出旧令牌，避免令牌堆积
        userTokenMapper.deleteByUserId(user.getId());

        UserToken userToken = new UserToken();
        userToken.setUserId(user.getId());
        userToken.setToken(TokenUtil.generateToken());
        int days = Boolean.TRUE.equals(rememberMe) ? TOKEN_EXPIRE_DAYS : 1;
        userToken.setExpireTime(LocalDateTime.now().plusDays(days));
        userTokenMapper.insert(userToken);

        LoginVO vo = new LoginVO();
        vo.setToken(userToken.getToken());
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setMemberLevel(user.getMemberLevel());
        vo.setPoints(user.getPoints());
        return vo;
    }

    /**
     * 登出：删除当前用户令牌
     */
    public void logout() {
        Long userId = UserContext.getCurrentUserId();
        if (userId != null) {
            userTokenMapper.deleteByUserId(userId);
        }
    }

    // ==================== 个人信息 ====================

    /**
     * 获取当前登录用户信息
     */
    public UserVO getCurrentUser() {
        Long userId = requireLogin();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserVO(user);
    }

    /**
     * 修改个人信息（用户名、昵称、性别、头像）
     */
    @Transactional
    public UserVO updateProfile(UpdateProfileDTO dto) {
        Long userId = requireLogin();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 用户名变更需校验唯一性
        if (!user.getUsername().equals(dto.getUsername())
                && userMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BusinessException("该用户名已存在");
        }
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setGender(dto.getGender() != null ? dto.getGender() : 0);
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        userMapper.updateProfile(user);
        return toUserVO(userMapper.selectById(userId));
    }

    /**
     * 更新头像
     */
    @Transactional
    public UserVO updateAvatar(String avatar) {
        Long userId = requireLogin();
        userMapper.updateAvatar(userId, avatar);
        return toUserVO(userMapper.selectById(userId));
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    // ==================== 银行卡 ====================

    public List<BankCard> listBankCards() {
        return bankCardMapper.selectByUserId(requireLogin());
    }

    @Transactional
    public void bindBankCard(BankCardDTO dto) {
        Long userId = requireLogin();
        BankCard card = new BankCard();
        BeanUtils.copyProperties(dto, card);
        card.setUserId(userId);
        card.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()) ? 1 : 0);
        if (card.getIsDefault() == 1) {
            bankCardMapper.clearDefault(userId);
        }
        bankCardMapper.insert(card);
    }

    @Transactional
    public void unbindBankCard(Long cardId) {
        Long userId = requireLogin();
        bankCardMapper.deleteById(cardId, userId);
    }

    @Transactional
    public void setDefaultBankCard(Long cardId) {
        Long userId = requireLogin();
        bankCardMapper.clearDefault(userId);
        bankCardMapper.setDefault(cardId, userId);
    }

    // ==================== 收货地址 ====================

    public List<Address> listAddresses() {
        return addressMapper.selectByUserId(requireLogin());
    }

    @Transactional
    public void addAddress(AddressDTO dto) {
        Long userId = requireLogin();
        Address address = new Address();
        BeanUtils.copyProperties(dto, address);
        address.setUserId(userId);
        address.setTag(dto.getTag() != null ? dto.getTag() : "home");
        address.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()) ? 1 : 0);
        if (address.getIsDefault() == 1) {
            addressMapper.clearDefault(userId);
        }
        addressMapper.insert(address);
    }

    @Transactional
    public void updateAddress(Long id, AddressDTO dto) {
        Long userId = requireLogin();
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        BeanUtils.copyProperties(dto, address);
        address.setId(id);
        address.setUserId(userId);
        address.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()) ? 1 : 0);
        if (address.getIsDefault() == 1) {
            addressMapper.clearDefault(userId);
        }
        addressMapper.update(address);
    }

    @Transactional
    public void deleteAddress(Long id) {
        Long userId = requireLogin();
        addressMapper.deleteById(id, userId);
    }

    // ==================== 收藏 ====================

    public List<Merchant> listFavorites() {
        return favoriteMapper.selectFavoriteMerchants(requireLogin());
    }

    @Transactional
    public void addFavorite(Long merchantId) {
        Long userId = requireLogin();
        if (favoriteMapper.countByUserAndMerchant(userId, merchantId) > 0) {
            return; // 已收藏，幂等处理
        }
        favoriteMapper.insert(userId, merchantId);
    }

    @Transactional
    public void removeFavorite(Long merchantId) {
        Long userId = requireLogin();
        favoriteMapper.delete(userId, merchantId);
    }

    // ==================== 评价 ====================

    @Transactional
    public Review addReview(ReviewDTO dto) {
        Long userId = requireLogin();
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new BusinessException("仅已完成订单可评价");
        }
        if (reviewMapper.selectByOrderId(dto.getOrderId()) != null) {
            throw new BusinessException("该订单已评价");
        }
        Review review = new Review();
        review.setUserId(userId);
        review.setOrderId(dto.getOrderId());
        review.setMerchantId(order.getMerchantId());
        review.setRating(dto.getRating());
        review.setDeliveryRating(dto.getDeliveryRating());
        review.setContent(dto.getContent());
        reviewMapper.insert(review);
        // 装配菜品明细
        List<ReviewItem> items = mapItems(dto.getItems(), review.getId());
        if (!items.isEmpty()) {
            reviewItemMapper.insertBatch(items);
        }
        review.setItems(items);
        review.setMerchantName(order.getMerchantName());
        review.setOrderNo(order.getOrderNo());
        return review;
    }

    public List<Review> listMyReviews() {
        Long userId = requireLogin();
        List<Review> reviews = reviewMapper.selectByUserId(userId);
        if (reviews == null || reviews.isEmpty()) {
            return reviews;
        }
        attachItems(reviews);
        return reviews;
    }

    public Review getReview(Long id) {
        Long userId = requireLogin();
        Review review = reviewMapper.selectById(id);
        if (review == null || !review.getUserId().equals(userId)) {
            throw new BusinessException(404, "评价不存在");
        }
        List<ReviewItem> items = reviewItemMapper.selectByReviewId(id);
        items.forEach(this::populateImageList);
        review.setItems(items);
        return review;
    }

    @Transactional
    public Review updateReview(Long id, ReviewDTO dto) {
        Long userId = requireLogin();
        Review review = reviewMapper.selectById(id);
        if (review == null || !review.getUserId().equals(userId)) {
            throw new BusinessException(404, "评价不存在");
        }
        assertWithin24h(review);
        review.setRating(dto.getRating());
        review.setDeliveryRating(dto.getDeliveryRating());
        review.setContent(dto.getContent());
        reviewMapper.update(review);
        // 删旧 items 后插新 items（同事务回滚保护）
        reviewItemMapper.deleteByReviewId(id);
        List<ReviewItem> items = mapItems(dto.getItems(), id);
        if (!items.isEmpty()) {
            reviewItemMapper.insertBatch(items);
        }
        review.setItems(items);
        return review;
    }

    @Transactional
    public void deleteReview(Long id) {
        Long userId = requireLogin();
        Review review = reviewMapper.selectById(id);
        if (review == null || !review.getUserId().equals(userId)) {
            throw new BusinessException(404, "评价不存在");
        }
        assertWithin24h(review);
        reviewItemMapper.deleteByReviewId(id);
        reviewMapper.delete(id);
    }

    // ==================== 评价工具 ====================

    /** 24 小时内可编辑/删除，超时抛异常；以 create_time 计算，边界 now==deadline 仍可操作 */
    private void assertWithin24h(Review review) {
        LocalDateTime deadline = review.getCreateTime().plusHours(24);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new BusinessException("评价提交超过24小时，不可操作");
        }
    }

    /** DTO.items → 实体列表（rating null→5，images List→csv） */
    private List<ReviewItem> mapItems(List<ReviewItemDTO> dtos, Long reviewId) {
        if (dtos == null || dtos.isEmpty()) {
            return new ArrayList<>();
        }
        List<ReviewItem> items = new ArrayList<>(dtos.size());
        for (ReviewItemDTO d : dtos) {
            ReviewItem it = new ReviewItem();
            it.setReviewId(reviewId);
            it.setDishId(d.getDishId());
            it.setDishName(d.getDishName());
            it.setDishImage(d.getDishImage());
            it.setRating(d.getRating() == null ? 5 : d.getRating());
            it.setContent(d.getContent());
            it.setImages(toCsv(d.getImages()));
            items.add(it);
        }
        return items;
    }

    /** images List → 逗号分隔字符串，取前 3 条 */
    private String toCsv(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream().limit(3).collect(Collectors.joining(","));
    }

    /** images csv → imageList */
    private void populateImageList(ReviewItem item) {
        if (item.getImages() == null || item.getImages().isEmpty()) {
            item.setImageList(new ArrayList<>());
            return;
        }
        List<String> list = new ArrayList<>();
        for (String s : item.getImages().split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                list.add(t);
            }
        }
        item.setImageList(list);
    }

    /** 批量为 reviews 装配 items（按 reviewId 分组），空集合提前返回避免 IN () */
    private void attachItems(List<Review> reviews) {
        List<Long> reviewIds = reviews.stream().map(Review::getId).collect(Collectors.toList());
        List<ReviewItem> all = reviewItemMapper.selectByReviewIds(reviewIds);
        Map<Long, List<ReviewItem>> group = all.stream().collect(Collectors.groupingBy(ReviewItem::getReviewId));
        for (Review r : reviews) {
            List<ReviewItem> items = group.get(r.getId());
            if (items != null) {
                items.forEach(this::populateImageList);
                r.setItems(items);
            } else {
                r.setItems(new ArrayList<>());
            }
        }
    }

    // ==================== 工具 ====================

    /**
     * 校验登录态，返回用户ID，未登录抛异常
     */
    public Long requireLogin() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }
}
