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
 * 商家管理自有店铺、菜品、优惠与订单，所有操作限定在自身店铺范围内
 */
@Service
public class ShopService {

    @Autowired
    private MerchantMapper merchantMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishOptionMapper dishOptionMapper;
    @Autowired
    private DishOptionChoiceMapper dishOptionChoiceMapper;
    @Autowired
    private MerchantPromoMapper merchantPromoMapper;
    @Autowired
    private PromoRuleMapper promoRuleMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderService orderService;

    // ==================== 通用 ====================

    /** 校验当前登录用户为商家 */
    private void requireMerchant() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!UserContext.isMerchant()) {
            throw new BusinessException(403, "无商家权限");
        }
    }

    /** 获取当前商家ID（基于登录用户关联的店铺） */
    private Long getMyMerchantId() {
        requireMerchant();
        Merchant m = merchantMapper.selectByUserId(UserContext.getCurrentUserId());
        if (m == null) {
            throw new BusinessException("未找到关联店铺");
        }
        return m.getId();
    }

    // ==================== 店铺管理 ====================

    /** 查询当前商家店铺信息 */
    public Merchant getMyMerchant() {
        return merchantMapper.selectById(getMyMerchantId());
    }

    /**
     * 修改店铺信息
     * 禁止商家修改 isRecommended（好评商家）和 badge（徽章）
     */
    @Transactional
    public void updateMyMerchant(MerchantDTO dto) {
        Long merchantId = getMyMerchantId();
        Merchant m = merchantMapper.selectById(merchantId);
        if (m == null) {
            throw new BusinessException("店铺不存在");
        }
        if (dto.getName() != null) m.setName(dto.getName());
        if (dto.getLogo() != null) m.setLogo(dto.getLogo());
        if (dto.getCover() != null) m.setCover(dto.getCover());
        if (dto.getCategory() != null) m.setCategory(dto.getCategory());
        if (dto.getPriceLevel() != null) m.setPriceLevel(dto.getPriceLevel());
        if (dto.getRating() != null) m.setRating(dto.getRating());
        if (dto.getSales() != null) m.setSales(dto.getSales());
        if (dto.getDeliveryTime() != null) m.setDeliveryTime(dto.getDeliveryTime());
        if (dto.getDistance() != null) m.setDistance(dto.getDistance());
        if (dto.getMinOrder() != null) m.setMinOrder(dto.getMinOrder());
        if (dto.getDeliveryFee() != null) m.setDeliveryFee(dto.getDeliveryFee());
        if (dto.getTags() != null) m.setTags(dto.getTags());
        if (dto.getStatus() != null) m.setStatus(dto.getStatus());
        // 显式不拷贝 isRecommended 和 badge
        merchantMapper.update(m);
    }

    // ==================== 菜品管理 ====================

    /** 查询当前商家全部菜品（含下架） */
    public List<Dish> listMyDishes() {
        return dishMapper.selectAllByMerchantId(getMyMerchantId());
    }

    /** 新增菜品（强制归属当前商家） */
    @Transactional
    public void addMyDish(DishDTO dto) {
        Long merchantId = getMyMerchantId();
        Dish dish = new Dish();
        dish.setMerchantId(merchantId);
        dish.setName(dto.getName());
        dish.setPrice(dto.getPrice());
        dish.setOldPrice(dto.getOldPrice());
        dish.setImage(dto.getImage());
        dish.setDescription(dto.getDescription());
        dish.setCategory(dto.getCategory());
        dish.setSales(0);
        dish.setIsDiscount(dto.getIsDiscount() != null ? dto.getIsDiscount() : 0);
        dish.setIsNew(dto.getIsNew() != null ? dto.getIsNew() : 0);
        dish.setIsPopular(dto.getIsPopular() != null ? dto.getIsPopular() : 0);
        dish.setOnShelf(dto.getOnShelf() != null ? dto.getOnShelf() : 0);
        dish.setSoldOut(dto.getSoldOut() != null ? dto.getSoldOut() : 0);
        dishMapper.insert(dish);
        saveDishOptions(dish.getId(), dto.getOptions());
    }

    /** 修改菜品（校验归属） */
    @Transactional
    public void updateMyDish(Long id, DishDTO dto) {
        Long merchantId = getMyMerchantId();
        Dish dish = dishMapper.selectById(id);
        if (dish == null || !merchantId.equals(dish.getMerchantId())) {
            throw new BusinessException("菜品不存在或无权操作");
        }
        dish.setName(dto.getName());
        dish.setPrice(dto.getPrice());
        dish.setOldPrice(dto.getOldPrice());
        dish.setImage(dto.getImage());
        dish.setDescription(dto.getDescription());
        dish.setCategory(dto.getCategory());
        if (dto.getIsDiscount() != null) dish.setIsDiscount(dto.getIsDiscount());
        if (dto.getIsNew() != null) dish.setIsNew(dto.getIsNew());
        if (dto.getIsPopular() != null) dish.setIsPopular(dto.getIsPopular());
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

    /** 删除菜品（校验归属，级联删除规格选项） */
    @Transactional
    public void deleteMyDish(Long id) {
        Long merchantId = getMyMerchantId();
        Dish dish = dishMapper.selectById(id);
        if (dish == null || !merchantId.equals(dish.getMerchantId())) {
            throw new BusinessException("菜品不存在或无权操作");
        }
        deleteDishOptions(id);
        dishMapper.deleteById(id);
    }

    @Transactional
    public void toggleMyShelf(Long id) {
        Dish dish = requireMyDish(id);
        dish.setOnShelf(dish.getOnShelf() == 0 ? 1 : 0);
        dishMapper.update(dish);
    }

    @Transactional
    public void toggleMyPopular(Long id) {
        Dish dish = requireMyDish(id);
        dish.setIsPopular(dish.getIsPopular() != null && dish.getIsPopular() == 1 ? 0 : 1);
        dishMapper.update(dish);
    }

    @Transactional
    public void toggleMySoldOut(Long id) {
        Long merchantId = getMyMerchantId();
        Dish dish = dishMapper.selectById(id);
        if (dish == null || !merchantId.equals(dish.getMerchantId())) {
            throw new BusinessException("菜品不存在或无权操作");
        }
        int newVal = (dish.getSoldOut() != null && dish.getSoldOut() == 1) ? 0 : 1;
        dishMapper.updateSoldOut(id, newVal);
    }

    /** 校验菜品归属当前商家并返回 */
    private Dish requireMyDish(Long id) {
        Long merchantId = getMyMerchantId();
        Dish dish = dishMapper.selectById(id);
        if (dish == null || !merchantId.equals(dish.getMerchantId())) {
            throw new BusinessException("菜品不存在或无权操作");
        }
        return dish;
    }

    // ==================== 优惠描述管理（MerchantPromo） ====================

    public List<MerchantPromo> listMyPromos() {
        return merchantPromoMapper.selectByMerchantId(getMyMerchantId());
    }

    @Transactional
    public void addMyPromo(MerchantPromo promo) {
        promo.setMerchantId(getMyMerchantId());
        promo.setStatus(promo.getStatus() != null ? promo.getStatus() : 0);
        merchantPromoMapper.insert(promo);
    }

    @Transactional
    public void updateMyPromo(Long id, MerchantPromo promo) {
        MerchantPromo exist = merchantPromoMapper.selectById(id);
        Long merchantId = getMyMerchantId();
        if (exist == null || !merchantId.equals(exist.getMerchantId())) {
            throw new BusinessException("优惠不存在或无权操作");
        }
        promo.setId(id);
        promo.setMerchantId(merchantId);
        merchantPromoMapper.update(promo);
    }

    @Transactional
    public void deleteMyPromo(Long id) {
        MerchantPromo exist = merchantPromoMapper.selectById(id);
        Long merchantId = getMyMerchantId();
        if (exist == null || !merchantId.equals(exist.getMerchantId())) {
            throw new BusinessException("优惠不存在或无权操作");
        }
        merchantPromoMapper.deleteById(id);
    }

    @Transactional
    public void toggleMyPromoStatus(Long id, Integer status) {
        MerchantPromo exist = merchantPromoMapper.selectById(id);
        Long merchantId = getMyMerchantId();
        if (exist == null || !merchantId.equals(exist.getMerchantId())) {
            throw new BusinessException("优惠不存在或无权操作");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态参数仅允许 0 或 1");
        }
        merchantPromoMapper.updateStatus(id, status);
    }

    // ==================== 满减规则管理（PromoRule） ====================

    public List<PromoRule> listMyPromoRules() {
        return promoRuleMapper.selectByMerchantId(getMyMerchantId());
    }

    @Transactional
    public void addMyPromoRule(PromoRule rule) {
        rule.setMerchantId(getMyMerchantId());
        rule.setStatus(rule.getStatus() != null ? rule.getStatus() : 0);
        promoRuleMapper.insert(rule);
    }

    @Transactional
    public void updateMyPromoRule(Long id, PromoRule rule) {
        PromoRule exist = promoRuleMapper.selectById(id);
        Long merchantId = getMyMerchantId();
        if (exist == null || !merchantId.equals(exist.getMerchantId())) {
            throw new BusinessException("规则不存在或无权操作");
        }
        rule.setId(id);
        rule.setMerchantId(merchantId);
        promoRuleMapper.update(rule);
    }

    @Transactional
    public void deleteMyPromoRule(Long id) {
        PromoRule exist = promoRuleMapper.selectById(id);
        Long merchantId = getMyMerchantId();
        if (exist == null || !merchantId.equals(exist.getMerchantId())) {
            throw new BusinessException("规则不存在或无权操作");
        }
        promoRuleMapper.deleteById(id);
    }

    @Transactional
    public void toggleMyPromoRuleStatus(Long id, Integer status) {
        PromoRule exist = promoRuleMapper.selectById(id);
        Long merchantId = getMyMerchantId();
        if (exist == null || !merchantId.equals(exist.getMerchantId())) {
            throw new BusinessException("规则不存在或无权操作");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态参数仅允许 0 或 1");
        }
        promoRuleMapper.updateStatus(id, status);
    }

    // ==================== 订单管理 ====================

    /** 商家订单列表（可按状态筛选） */
    public List<Order> listMyOrders(String status) {
        return orderMapper.selectByMerchantId(getMyMerchantId(), status);
    }

    /** 商家订单详情（校验归属） */
    public OrderDetailVO getMyOrderDetail(Long id) {
        Long merchantId = getMyMerchantId();
        Order order = orderMapper.selectById(id);
        if (order == null || !merchantId.equals(order.getMerchantId())) {
            throw new BusinessException("订单不存在或无权查看");
        }
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(order);
        vo.setItems(orderItemMapper.selectByOrderId(id));
        return vo;
    }

    /** 商家接单：待确认 -> 已确认 */
    @Transactional
    public void confirmMyOrder(Long id) {
        orderService.merchantConfirmOrder(id, getMyMerchantId());
    }

    /** 商家派送：已确认 -> 配送中 */
    @Transactional
    public void dispatchMyOrder(Long id) {
        orderService.merchantDispatchOrder(id, getMyMerchantId());
    }

    // ==================== 菜品规格选项辅助方法 ====================

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
