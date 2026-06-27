package com.qiukai.mapper;

import com.qiukai.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商家 Mapper
 */
@Mapper
public interface MerchantMapper {

    Merchant selectById(@Param("id") Long id);

    List<Merchant> selectAll();

    /** 管理员查询全部商家（含休息状态） */
    List<Merchant> selectAllForAdmin();

    /** 按分类查询 */
    List<Merchant> selectByCategory(@Param("category") String category);

    /** 按关键词搜索（名称/分类/标签） */
    List<Merchant> search(@Param("keyword") String keyword);

    /** 查询好评商家（is_recommended=1 且营业中） */
    List<Merchant> selectRecommended();

    /** 更新好评商家状态 */
    int updateRecommended(@Param("id") Long id, @Param("isRecommended") Integer isRecommended);

    int insert(Merchant merchant);

    int update(Merchant merchant);

    int deleteById(@Param("id") Long id);
}
