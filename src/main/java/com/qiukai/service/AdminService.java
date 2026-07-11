package com.qiukai.service;

import com.qiukai.common.BusinessException;
import com.qiukai.dto.DishDTO;
import com.qiukai.dto.DishOptionChoiceDTO;
import com.qiukai.dto.DishOptionDTO;
import com.qiukai.dto.MerchantDTO;
import com.qiukai.entity.Dish;
import com.qiukai.entity.DishOption;
import com.qiukai.entity.DishOptionChoice;
import com.qiukai.entity.Merchant;
import com.qiukai.entity.MerchantOpLog;
import com.qiukai.interceptor.UserContext;
import com.qiukai.mapper.DishMapper;
import com.qiukai.mapper.DishOptionChoiceMapper;
import com.qiukai.mapper.DishOptionMapper;
import com.qiukai.mapper.MerchantMapper;
import com.qiukai.mapper.MerchantOpLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 后台管理服务
 * 系统级管理员可管理全部商家、菜品与订单
 */
@Service
public class AdminService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private MerchantMapper merchantMapper;
    @Autowired
    private DishOptionMapper dishOptionMapper;
    @Autowired
    private DishOptionChoiceMapper dishOptionChoiceMapper;
    @Autowired
    private MerchantOpLogMapper merchantOpLogMapper;

    // ==================== 商家管理 ====================

    /**
     * 查询全部商家（含休息状态）
     */
    public List<Merchant> listMerchants() {
        requireAdmin();
        return merchantMapper.selectAllForAdmin();
    }

    /**
     * 新增商家
     */
    @Transactional
    public void addMerchant(MerchantDTO dto) {
        requireAdmin();
        Merchant m = new Merchant();
        m.setName(dto.getName());
        m.setLogo(dto.getLogo());
        m.setCover(dto.getCover());
        m.setCategory(dto.getCategory());
        m.setPriceLevel(dto.getPriceLevel() != null ? dto.getPriceLevel() : "¥¥");
        m.setRating(dto.getRating() != null ? dto.getRating() : new BigDecimal("4.8"));
        m.setSales(dto.getSales() != null ? dto.getSales() : 0);
        m.setDeliveryTime(dto.getDeliveryTime() != null ? dto.getDeliveryTime() : 30);
        m.setDistance(dto.getDistance() != null ? dto.getDistance() : new BigDecimal("1.0"));
        m.setMinOrder(dto.getMinOrder() != null ? dto.getMinOrder() : new BigDecimal("20.00"));
        m.setDeliveryFee(dto.getDeliveryFee() != null ? dto.getDeliveryFee() : new BigDecimal("3.00"));
        m.setBadge(dto.getBadge());
        m.setTags(dto.getTags());
        m.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);
        m.setIsRecommended(dto.getIsRecommended() != null ? dto.getIsRecommended() : 0);
        merchantMapper.insert(m);
    }

    /**
     * 修改商家信息
     */
    @Transactional
    public void updateMerchant(Long id, MerchantDTO dto) {
        requireAdmin();
        Merchant m = merchantMapper.selectById(id);
        if (m == null) {
            throw new BusinessException("商家不存在");
        }
        m.setName(dto.getName());
        m.setLogo(dto.getLogo());
        m.setCover(dto.getCover());
        m.setCategory(dto.getCategory());
        if (dto.getPriceLevel() != null) m.setPriceLevel(dto.getPriceLevel());
        if (dto.getRating() != null) m.setRating(dto.getRating());
        if (dto.getSales() != null) m.setSales(dto.getSales());
        if (dto.getDeliveryTime() != null) m.setDeliveryTime(dto.getDeliveryTime());
        if (dto.getDistance() != null) m.setDistance(dto.getDistance());
        if (dto.getMinOrder() != null) m.setMinOrder(dto.getMinOrder());
        if (dto.getDeliveryFee() != null) m.setDeliveryFee(dto.getDeliveryFee());
        if (dto.getBadge() != null) m.setBadge(dto.getBadge());
        if (dto.getTags() != null) m.setTags(dto.getTags());
        if (dto.getStatus() != null) m.setStatus(dto.getStatus());
        merchantMapper.update(m);
    }

    /**
     * 删除商家（同时删除其菜品）
     */
    @Transactional
    public void deleteMerchant(Long id) {
        requireAdmin();
        if (merchantMapper.selectById(id) == null) {
            throw new BusinessException("商家不存在");
        }
        // 删除商家下的全部菜品（含规格选项）
        List<Dish> dishes = dishMapper.selectAllByMerchantId(id);
        for (Dish d : dishes) {
            deleteDishOptions(d.getId());
            dishMapper.deleteById(d.getId());
        }
        merchantMapper.deleteById(id);
    }

    /**
     * 切换好评商家状态
     */
    @Transactional
    public void toggleRecommended(Long id) {
        requireAdmin();
        Merchant m = merchantMapper.selectById(id);
        if (m == null) {
            throw new BusinessException("商家不存在");
        }
        int oldVal = m.getIsRecommended() != null ? m.getIsRecommended() : 0;
        int newVal = oldVal == 1 ? 0 : 1;
        merchantMapper.updateRecommended(id, newVal);
        saveOpLog(m, oldVal, newVal, "toggle_recommended");
    }

    /**
     * 批量设置好评商家状态
     */
    @Transactional
    public void batchToggleRecommended(List<Long> ids, Integer isRecommended) {
        requireAdmin();
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择至少一个商家");
        }
        if (isRecommended == null || (isRecommended != 0 && isRecommended != 1)) {
            throw new BusinessException("参数 isRecommended 仅允许 0 或 1");
        }
        for (Long id : ids) {
            Merchant m = merchantMapper.selectById(id);
            if (m == null) continue;
            int oldVal = m.getIsRecommended() != null ? m.getIsRecommended() : 0;
            if (oldVal == isRecommended) continue;
            merchantMapper.updateRecommended(id, isRecommended);
            saveOpLog(m, oldVal, isRecommended, "batch_toggle_recommended");
        }
    }

    /**
     * 记录操作日志
     */
    private void saveOpLog(Merchant m, int oldVal, int newVal, String action) {
        MerchantOpLog log = new MerchantOpLog();
        log.setMerchantId(m.getId());
        log.setMerchantName(m.getName());
        log.setOperatorId(UserContext.getCurrentUserId());
        log.setAction(action);
        log.setOldValue(String.valueOf(oldVal));
        log.setNewValue(String.valueOf(newVal));
        merchantOpLogMapper.insert(log);
    }

    // ==================== 菜品管理 ====================

    /**
     * 查询菜品列表
     * merchantId 为 null 时返回全部菜品，否则返回指定商家菜品
     */
    public List<Dish> listDishes(Long merchantId) {
        requireAdmin();
        if (merchantId == null) {
            return dishMapper.selectAllForAdmin();
        }
        return dishMapper.selectAllByMerchantId(merchantId);
    }

    /**
     * 新增菜品
     */
    @Transactional
    public void addDish(DishDTO dto) {
        requireAdmin();
        Dish dish = new Dish();
        dish.setMerchantId(dto.getMerchantId() != null ? dto.getMerchantId() : 1L);
        if (merchantMapper.selectById(dish.getMerchantId()) == null) {
            throw new BusinessException("商家不存在");
        }
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

        // 保存口味定制选项
        saveDishOptions(dish.getId(), dto.getOptions());
    }

    /**
     * 修改菜品
     */
    @Transactional
    public void updateDish(Long id, DishDTO dto) {
        requireAdmin();
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }
        if (dto.getMerchantId() != null) dish.setMerchantId(dto.getMerchantId());
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

        // 更新口味定制选项：options==null 表示不修改；空数组表示清空；非空数组表示替换
        if (dto.getOptions() != null) {
            deleteDishOptions(id);
            if (!dto.getOptions().isEmpty()) {
                saveDishOptions(id, dto.getOptions());
            }
        }
    }

    /**
     * 删除菜品（同时级联删除规格选项）
     */
    @Transactional
    public void deleteDish(Long id) {
        requireAdmin();
        deleteDishOptions(id);
        dishMapper.deleteById(id);
    }

    /**
     * 切换上下架状态
     */
    @Transactional
    public void toggleShelf(Long id) {
        requireAdmin();
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }
        dish.setOnShelf(dish.getOnShelf() == 0 ? 1 : 0);
        dishMapper.update(dish);
    }

    /**
     * 切换人气菜品标记
     */
    @Transactional
    public void togglePopular(Long id) {
        requireAdmin();
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }
        dish.setIsPopular(dish.getIsPopular() != null && dish.getIsPopular() == 1 ? 0 : 1);
        dishMapper.update(dish);
    }

    /**
     * 切换菜品售罄状态
     */
    @Transactional
    public void toggleSoldOut(Long id) {
        requireAdmin();
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }
        int newVal = (dish.getSoldOut() != null && dish.getSoldOut() == 1) ? 0 : 1;
        dishMapper.updateSoldOut(id, newVal);
    }

    // ==================== 口味定制选项辅助方法 ====================

    /**
     * 保存菜品规格选项（先插规格组获取自增ID，再批量插选项值）
     */
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

    /**
     * 删除菜品规格选项（先删选项值再删规格组）
     */
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

    private void requireAdmin() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!UserContext.isAdmin()) {
            throw new BusinessException(403, "无管理员权限");
        }
    }
}