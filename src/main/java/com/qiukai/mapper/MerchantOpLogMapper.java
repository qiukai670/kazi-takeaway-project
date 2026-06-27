package com.qiukai.mapper;

import com.qiukai.entity.MerchantOpLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商家操作日志 Mapper
 */
@Mapper
public interface MerchantOpLogMapper {

    int insert(MerchantOpLog log);
}
