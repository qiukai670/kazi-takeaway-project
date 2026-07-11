package com.qiukai.service;

import com.qiukai.common.BusinessException;
import com.qiukai.dto.DishDTO;
import com.qiukai.dto.DishOptionChoiceDTO;
import com.qiukai.dto.DishOptionDTO;
import com.qiukai.dto.MerchantDTO;
import com.qiukai.entity.*;
import com.qiukai.interceptor.UserContext;
import com.qiukai.mapper.*;
import com.qiukai.vo.OrderDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商家后台服务
 * 商家管理自有店铺信息、菜品、订单与优惠策略，所有操作校验归属权
 */
@Service
public class MerchantService {

    @Autowired
    private MerchantMapper merchantMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishOptionMapper dishOptionMapper;
    @Autowired
    private DishOptionChoiceMapper dishOptionChoiceMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private MerchantPromoMapper merchantPromoMapper;
    @Autowired
    private PromoRuleMapper promoRuleMapper;
    @Autowired
    private OrderService orderService;

    // ==================== 店铺信息 ====================

    public Merchant getMyMerchant() {
        Long mid = getMyMerchantId();
        Merchant m = merchantMapper.selectById(mid);
        if (m == null) {
            throw new BusinessException("店铺信息不存在");
        }
        return m;
    }

    @Transactional
    public void updateMyMerchant(MerchantDTO dto) {
        Long mid = getMyMerchantId();
        Merchant m = merchantMapper.selectById(mid);
        if (m == null) {
            throw new BusinessException("店铺信息不存在");
        }
        m.setName(dto.getName());
        m.setLogo(dto.getLogo());
        m.setCover(dto.getCover());
        m.setCategory(dto.getCategory());
        if (dto.getPriceLevel() != null) m.setPriceLevel(dto.getPriceLevel());
        if (dto.getDeliveryTime() != null) m.setDeliveryTime(dto.getDeliveryTime());
        if (dto.getMinOrder() != null) m.setMinOrder(dto.getMinOrder());
        if (dto.getDeliveryFee() != null) m.setDeliveryFee(dto.getDeliveryFee());
        if (dto.getTags() != null) m.setTags(dto.getTags());
        if (dto.getStatus() != null) m.setStatus(dto.getStatus());
        // 禁止商家修改 isRecommended（好评商家）和 badge（徽章）
        merchantMapper.update(m);
    }

    // ==================== 菜品管理 ====================

    public List<Dish> listMyDishes() {
        Long mid = getMyMerchantId();
        return dishMapper.selectAllByMerchantId(mid);
    }

    @Transactional
    public void addMyDish(DishDTO dto) {
        Long mid = getMyMerchantId();
        Dish dish = new Dish();
        dish.setMerchantId(mid);
        dish.setName(dto.getName());
        dish.setPrice(dto.getPrice());
        dish.setOldPrice(dto.getOldPrice());
        dish.setImage(dto.getImage());
        dish.setDescription(dto.getDescription());
        dish.setCategory(dto.getCategory());
        dish.setSales(0);
        dish.setIsDiscount(dto.getIsDiscount() != null ? dto.getIsDiscount() : 0);
        dish.setIsNew(dto.getIsNew() != null ? dto.getIsNew() : 0);
        dish.setIsPopular(0);
        dish.setOnShelf(dto.getOnShelf() != null ? dto.getOnShelf() : 0);
        dish.setSoldOut(dto.getSoldOut() != null ? dto.getSoldOut() : 0);
        dishMapper.insert(dish);
        saveDishOptions(dish.getId(), dto.getOptions());
    }

    @Transactional
    public void updateMyDish(Long id, DishDTO dto) {
        Long mid = getMyMerchantId();
        Dish dish = dishMapper.selectById(id);
        if (dish == null || !mid.equals(dish.getMerchantId())) {
            throw new BusinessException(403, "无权操作此菜品");
        }
        dish.setName(dto.getName());
        dish.setPrice(dto.getPrice());
        dish.setOldPrice(dto.getOldPrice());
        dish.setImage(dto.getImage());
        dish.setDescription(dto.getDescription());
        dish.setCategory(dto.getCategory());
        if (dto.getIsDiscount() != null) dish.setIsDiscount(dto.getIsDiscount());
        if (dto.getIsNew() != null) dish.setIsNew(dto.getIsNew());
        if (dto.getOnShelf() != null) dish.setOnShelf(dto.getOnShelf());
        if (dto.getSoldOut() != null) dish.setSoldOut(dto.getSoldOut());
        dishMapper.update(dish);
        if (dto.getOptions() != null) {
            deleteDishOptions(id);
            if (!dto.getOptions().isEmpty()) {
                saveDishOptions(id, dto.getOptions());
            }
        }
    }

    @Transactional
    public void deleteMyDish(Long id) {
        Long mid = getMyMerchantId();
        Dish dish = dishMapper.selectById(id);
        if (dish == null || !mid.equals(dish.getMerchantId())) {
            throw new BusinessException(403, "无权操作此菜品");
        }
        deleteDishOptions(id);
        dishMapper.deleteById(id);
    }

    @Transactional
    public void toggleMyDishShelf(Long id) {
        Long mid = getMyMerchantId();
        Dish dish = dishMapper.selectById(id);
        if (dish == null || !mid.equals(dish.getMerchantId())) {
            throw new BusinessException(403, "无权操作此菜品");
        }
        dish.setOnShelf(dish.getOnShelf() == 0 ? 1 : 0);
        dishMapper.update(dish);
    }

    @Transactional
    public void toggleMyDishSoldOut(Long id) {
        Long mid = getMyMerchantId();
        Dish dish = dishMapper.selectById(id);
        if (dish == null || !mid.equals(dish.getMerchantId())) {
            throw new BusinessException(403, "无权操作此菜品");
        }
        int newVal = (dish.getSoldOut() != null && dish.getSoldOut() == 1) ? 0 : 1;
        dishMapper.updateSoldOut(id, newVal);
    }

    // ==================== 订单管理 ====================

    public List<Order> listMyOrders(String status) {
        Long mid = getMyMerchantId();
        return orderMapper.selectByMerchantId(mid, status);
    }

    public OrderDetailVO getMyOrderDetail(Long orderId) {
        Long mid = getMyMerchantId();
        Order order = orderMapper.selectById(orderId);
        if (order == null || !mid.equals(order.getMerchantId())) {
            throw new BusinessException(403, "无权查看此订单");
        }
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(order);
        vo.setItems(orderItemMapper.selectByOrderId(orderId));
        return vo;
    }

    @Transactional
    public void confirmMyOrder(Long orderId) {
        Long mid = getMyMerchantId();
        orderService.merchantConfirmOrder(orderId, mid);
    }

    @Transactional
    public void dispatchMyOrder(Long orderId) {
        Long mid = getMyMerchantId();
        orderService.merchantDispatchOrder(orderId, mid);
    }

    // ==================== 优惠管理 - MerchantPromo ====================

    public List<MerchantPromo> listMyPromos() {
        Long mid = getMyMerchantId();
        return merchantPromoMapper.selectByMerchantId(mid);
    }

    @Transactional
    public void addMyPromo(MerchantPromo promo) {
        Long mid = getMyMerchantId();
        validatePromo(promo);
        promo.setId(null);
        promo.setMerchantId(mid);
        if (promo.getPriority() == null) promo.setPriority(0);
        if (promo.getStatus() == null) promo.setStatus(0);
        merchantPromoMapper.insert(promo);
    }

    @Transactional
    public void updateMyPromo(Long id, MerchantPromo promo) {
        Long mid = getMyMerchantId();
        MerchantPromo existing = merchantPromoMapper.selectById(id);
        if (existing == null || !mid.equals(existing.getMerchantId())) {
            throw new BusinessException(403, "无权操作此优惠");
        }
        // 禁止修改 type，保持原有类型；按入参非 null 字段覆盖
        if (promo.getDescription() != null) existing.setDescription(promo.getDescription());
        if (promo.getName() != null) existing.setName(promo.getName());
        if (promo.getDiscountRatio() != null) existing.setDiscountRatio(promo.getDiscountRatio());
        if (promo.getMinSpend() != null) existing.setMinSpend(promo.getMinSpend());
        if (promo.getPriority() != null) existing.setPriority(promo.getPriority());
        if (promo.getStartTime() != null) existing.setStartTime(promo.getStartTime());
        if (promo.getEndTime() != null) existing.setEndTime(promo.getEndTime());
        if (promo.getStatus() != null) existing.setStatus(promo.getStatus());
        validatePromo(existing);
        merchantPromoMapper.update(existing);
    }

    /**
     * 优惠活动字段校验（按类型）
     * fullcut: description 必填
     * discount: name 必填，discountRatio 必填 0-99，minSpend 选填 >=0
     * freefee: name 必填，minSpend 必填 >0
     * 通用: start/end 都填时 start < end
     */
    private void validatePromo(MerchantPromo promo) {
        String type = promo.getType();
        if (type == null) {
            throw new BusinessException("优惠类型不能为空");
        }
        if ("fullcut".equals(type)) {
            if (promo.getDescription() == null || promo.getDescription().isBlank()) {
                throw new BusinessException("满减优惠描述不能为空");
            }
        } else if ("discount".equals(type)) {
            if (promo.getName() == null || promo.getName().isBlank()) {
                throw new BusinessException("折扣活动名称不能为空");
            }
            Integer ratio = promo.getDiscountRatio();
            if (ratio == null) {
                throw new BusinessException("折扣率不能为空");
            }
            if (ratio < 0 || ratio > 99) {
                throw new BusinessException("折扣率需在0-99之间（80=打8折，0=免单）");
            }
            if (promo.getMinSpend() != null && promo.getMinSpend().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("最低消费金额不能为负数");
            }
        } else if ("freefee".equals(type)) {
            if (promo.getName() == null || promo.getName().isBlank()) {
                throw new BusinessException("免配送费活动名称不能为空");
            }
            if (promo.getMinSpend() == null || promo.getMinSpend().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("免配送费门槛金额需大于0");
            }
        } else {
            throw new BusinessException("不支持的优惠类型：" + type);
        }
        if (promo.getStartTime() != null && promo.getEndTime() != null
                && !promo.getStartTime().isBefore(promo.getEndTime())) {
            throw new BusinessException("生效时间需早于失效时间");
        }
    }

    @Transactional
    public void toggleMyPromoStatus(Long id) {
        Long mid = getMyMerchantId();
        MerchantPromo existing = merchantPromoMapper.selectById(id);
        if (existing == null || !mid.equals(existing.getMerchantId())) {
            throw new BusinessException(403, "无权操作此优惠");
        }
        int newVal = (existing.getStatus() != null && existing.getStatus() == 0) ? 1 : 0;
        merchantPromoMapper.updateStatus(id, newVal);
    }

    @Transactional
    public void deleteMyPromo(Long id) {
        Long mid = getMyMerchantId();
        MerchantPromo existing = merchantPromoMapper.selectById(id);
        if (existing == null || !mid.equals(existing.getMerchantId())) {
            throw new BusinessException(403, "无权操作此优惠");
        }
        merchantPromoMapper.deleteById(id);
    }

    // ==================== 满减规则 - PromoRule ====================

    public List<PromoRule> listMyPromoRules() {
        Long mid = getMyMerchantId();
        return promoRuleMapper.selectByMerchantId(mid);
    }

    @Transactional
    public void addMyPromoRule(PromoRule rule) {
        Long mid = getMyMerchantId();
        rule.setId(null);
        rule.setMerchantId(mid);
        if (rule.getStatus() == null) rule.setStatus(0);
        promoRuleMapper.insert(rule);
    }

    @Transactional
    public void updateMyPromoRule(Long id, PromoRule rule) {
        Long mid = getMyMerchantId();
        PromoRule existing = promoRuleMapper.selectById(id);
        if (existing == null || !mid.equals(existing.getMerchantId())) {
            throw new BusinessException(403, "无权操作此规则");
        }
        existing.setThreshold(rule.getThreshold());
        existing.setDiscount(rule.getDiscount());
        if (rule.getStatus() != null) existing.setStatus(rule.getStatus());
        promoRuleMapper.update(existing);
    }

    @Transactional
    public void toggleMyPromoRuleStatus(Long id) {
        Long mid = getMyMerchantId();
        PromoRule existing = promoRuleMapper.selectById(id);
        if (existing == null || !mid.equals(existing.getMerchantId())) {
            throw new BusinessException(403, "无权操作此规则");
        }
        int newVal = (existing.getStatus() != null && existing.getStatus() == 0) ? 1 : 0;
        promoRuleMapper.updateStatus(id, newVal);
    }

    @Transactional
    public void deleteMyPromoRule(Long id) {
        Long mid = getMyMerchantId();
        PromoRule existing = promoRuleMapper.selectById(id);
        if (existing == null || !mid.equals(existing.getMerchantId())) {
            throw new BusinessException(403, "无权操作此规则");
        }
        promoRuleMapper.deleteById(id);
    }

    // ==================== 工具方法 ====================

    private Long getMyMerchantId() {
        requireMerchant();
        Long userId = UserContext.getCurrentUserId();
        Merchant m = merchantMapper.selectByUserId(userId);
        if (m == null) {
            throw new BusinessException("未找到关联店铺，请联系管理员");
        }
        return m.getId();
    }

    private void requireMerchant() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!UserContext.isMerchant()) {
            throw new BusinessException(403, "无商家权限");
        }
    }

    private void saveDishOptions(Long dishId, List<DishOptionDTO> options) {
        if (options == null || options.isEmpty()) {
            return;
        }
        int sort = 1;
        for (DishOptionDTO opt : options) {
            if (opt.getName() == null || opt.getName().isBlank()) {
                continue;
            }
            DishOption entity = new DishOption();
            entity.setDishId(dishId);
            entity.setName(opt.getName());
            entity.setOptionType(opt.getOptionType() != null ? opt.getOptionType() : "single");
            entity.setSort(opt.getSort() != null ? opt.getSort() : sort);
            dishOptionMapper.insert(entity);

            List<DishOptionChoiceDTO> choices = opt.getChoices();
            if (choices != null && !choices.isEmpty()) {
                List<DishOptionChoice> choiceEntities = new ArrayList<>();
                int cSort = 1;
                for (DishOptionChoiceDTO c : choices) {
                    if (c.getName() == null || c.getName().isBlank()) {
                        continue;
                    }
                    DishOptionChoice ce = new DishOptionChoice();
                    ce.setOptionId(entity.getId());
                    ce.setName(c.getName());
                    ce.setPriceAdd(c.getPriceAdd() != null ? c.getPriceAdd() : BigDecimal.ZERO);
                    ce.setSort(c.getSort() != null ? c.getSort() : cSort);
                    choiceEntities.add(ce);
                    cSort++;
                }
                if (!choiceEntities.isEmpty()) {
                    dishOptionChoiceMapper.insertBatch(choiceEntities);
                }
            }
            sort++;
        }
    }

    private void deleteDishOptions(Long dishId) {
        List<DishOption> options = dishOptionMapper.selectByDishId(dishId);
        if (options == null || options.isEmpty()) {
            return;
        }
        List<Long> optionIds = new ArrayList<>();
        for (DishOption o : options) {
            optionIds.add(o.getId());
        }
        dishOptionChoiceMapper.deleteByOptionIds(optionIds);
        dishOptionMapper.deleteByDishId(dishId);
    }
}
