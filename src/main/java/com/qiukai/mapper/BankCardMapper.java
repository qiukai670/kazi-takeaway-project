package com.qiukai.mapper;

import com.qiukai.entity.BankCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 银行卡 Mapper
 */
@Mapper
public interface BankCardMapper {

    List<BankCard> selectByUserId(@Param("userId") Long userId);

    BankCard selectById(@Param("id") Long id);

    int insert(BankCard bankCard);

    int deleteById(@Param("id") Long id, @Param("userId") Long userId);

    int clearDefault(@Param("userId") Long userId);

    int setDefault(@Param("id") Long id, @Param("userId") Long userId);
}
