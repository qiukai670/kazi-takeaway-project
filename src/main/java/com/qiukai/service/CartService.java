package com.qiukai.service;

import com.qiukai.common.BusinessException;
import com.qiukai.dto.CartItemDTO;
import com.qiukai.entity.Cart;
import com.qiukai.entity.Dish;
import com.qiukai.entity.DishOptionChoice;
import com.qiukai.entity.Merchant;
import com.qiukai.mapper.CartMapper;
import com.qiukai.mapper.DishMapper;
import com.qiukai.mapper.DishOptionChoiceMapper;
import com.qiukai.interceptor.UserContext;
import com.qiukai.vo.CartVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购物车服务
 * 提供菜品加入购物车、数量增减、删除、清空、实时金额计算
 */
@Service
public class CartService {

    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishOptionChoiceMapper dishOptionChoiceMapper;
    @Autowired
    private MenuService menuService;

    /**
     * 加入购物车（同菜品同规格自动合并数量）
     */
    @Transactional
    public void addToCart(CartItemDTO dto) {
        Long userId = requireLogin();
        Dish dish = dishMapper.selectById(dto.getDishId());
        if (dish == null || dish.getOnShelf() != 0) {
            throw new BusinessException("菜品不存在或已下架");
        }
        if (!dish.getMerchantId().equals(dto.getMerchantId())) {
            throw new BusinessException("菜品不属于该商家");
        }

        // 计算单价（基础价 + 规格加价），规格信息从数据库查询，不依赖前端传值
        BigDecimal basePrice = dish.getPrice();
        BigDecimal unitPrice = basePrice;
        StringBuilder optionsText = new StringBuilder();
        StringBuilder snapshot = new StringBuilder();

        if (dto.getChoices() != null && !dto.getChoices().isEmpty()) {
            // 从数据库批量查询选项值，获取真实的名称和加价
            List<Long> choiceIds = dto.getChoices().stream()
                    .map(CartItemDTO.CartOptionChoice::getChoiceId)
                    .collect(Collectors.toList());
            List<DishOptionChoice> dbChoices = dishOptionChoiceMapper.selectByIds(choiceIds);
            Map<Long, DishOptionChoice> choiceMap = dbChoices.stream()
                    .collect(Collectors.toMap(DishOptionChoice::getId, c -> c));

            for (CartItemDTO.CartOptionChoice c : dto.getChoices()) {
                DishOptionChoice dbChoice = choiceMap.get(c.getChoiceId());
                if (dbChoice == null) {
                    throw new BusinessException("规格选项不存在: " + c.getChoiceId());
                }
                if (dbChoice.getPriceAdd() != null) {
                    unitPrice = unitPrice.add(dbChoice.getPriceAdd());
                }
                if (optionsText.length() > 0) {
                    optionsText.append("、");
                }
                optionsText.append(dbChoice.getName());
                if (snapshot.length() > 0) {
                    snapshot.append(",");
                }
                snapshot.append(c.getChoiceId());
            }
        }

        String optionsSnapshot = snapshot.length() > 0 ? snapshot.toString() : null;
        String optText = optionsText.length() > 0 ? optionsText.toString() : null;

        // 查找同菜品同规格的购物车项，存在则合并数量
        Cart existing = cartMapper.selectExact(userId, dto.getMerchantId(), dto.getDishId(), optionsSnapshot);
        if (existing != null) {
            cartMapper.updateQty(existing.getId(), existing.getQty() + dto.getQty());
            return;
        }

        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setMerchantId(dto.getMerchantId());
        cart.setDishId(dto.getDishId());
        cart.setDishName(dish.getName());
        cart.setDishImage(dish.getImage());
        cart.setBasePrice(basePrice);
        cart.setUnitPrice(unitPrice);
        cart.setQty(dto.getQty());
        cart.setOptionsText(optText);
        cart.setOptionsSnapshot(optionsSnapshot);
        cartMapper.insert(cart);
    }

    /**
     * 修改购物车项数量（增减）
     */
    @Transactional
    public void updateQty(Long cartId, Integer qty) {
        Long userId = requireLogin();
        if (qty <= 0) {
            cartMapper.deleteById(cartId, userId);
            return;
        }
        cartMapper.updateQty(cartId, qty);
    }

    /**
     * 删除购物车项
     */
    @Transactional
    public void removeItem(Long cartId) {
        Long userId = requireLogin();
        cartMapper.deleteById(cartId, userId);
    }

    /**
     * 清空指定商家的购物车
     */
    @Transactional
    public void clearCart(Long merchantId) {
        Long userId = requireLogin();
        cartMapper.deleteByUserAndMerchant(userId, merchantId);
    }

    /**
     * 获取购物车汇总（含实时金额计算）
     */
    public CartVO getCart(Long merchantId) {
        Long userId = requireLogin();
        List<Cart> items = cartMapper.selectByUserAndMerchant(userId, merchantId);

        CartVO vo = new CartVO();
        vo.setMerchantId(merchantId);
        vo.setItems(items);

        if (items.isEmpty()) {
            vo.setTotalCount(0);
            vo.setTotalAmount(BigDecimal.ZERO);
            vo.setDeliveryFee(BigDecimal.ZERO);
            vo.setMinOrder(BigDecimal.ZERO);
            vo.setRemainToMinOrder(BigDecimal.ZERO);
            return vo;
        }

        Merchant merchant = menuService.getMerchant(merchantId);
        vo.setMerchantName(merchant.getName());
        vo.setDeliveryFee(merchant.getDeliveryFee());
        vo.setMinOrder(merchant.getMinOrder());

        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Cart item : items) {
            totalCount += item.getQty();
            totalAmount = totalAmount.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQty())));
        }
        vo.setTotalCount(totalCount);
        vo.setTotalAmount(totalAmount);
        BigDecimal remain = merchant.getMinOrder().subtract(totalAmount);
        vo.setRemainToMinOrder(remain.compareTo(BigDecimal.ZERO) > 0 ? remain : BigDecimal.ZERO);
        return vo;
    }

    /**
     * 获取用户购物车中指定商家的全部菜品（供下单使用）
     */
    public List<Cart> getCartItemsForOrder(Long userId, Long merchantId) {
        return cartMapper.selectByUserAndMerchant(userId, merchantId);
    }

    private Long requireLogin() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }
}
