package com.qiukai.mapper;

import com.qiukai.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 收货地址 Mapper
 */
@Mapper
public interface AddressMapper {

    List<Address> selectByUserId(@Param("userId") Long userId);

    Address selectById(@Param("id") Long id);

    int insert(Address address);

    int update(Address address);

    int deleteById(@Param("id") Long id, @Param("userId") Long userId);

    int clearDefault(@Param("userId") Long userId);
}
