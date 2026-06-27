package com.qiukai.mapper;

import com.qiukai.entity.UserToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 登录令牌表 Mapper
 */
@Mapper
public interface UserTokenMapper {

    /** 根据 token 查询有效令牌（未过期） */
    UserToken selectByToken(@Param("token") String token);

    /** 新增令牌 */
    int insert(UserToken userToken);

    /** 根据用户删除令牌（登出） */
    int deleteByUserId(@Param("userId") Long userId);

    /** 删除过期令牌（清理） */
    int deleteExpired();
}
