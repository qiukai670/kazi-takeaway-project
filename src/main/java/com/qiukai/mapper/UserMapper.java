package com.qiukai.mapper;

import com.qiukai.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户表 Mapper
 */
@Mapper
public interface UserMapper {

    /** 根据主键查询 */
    User selectById(@Param("id") Long id);

    /** 根据手机号查询 */
    User selectByPhone(@Param("phone") String phone);

    /** 根据用户名查询 */
    User selectByUsername(@Param("username") String username);

    /** 新增用户 */
    int insert(User user);

    /** 更新用户基本信息 */
    int updateProfile(User user);

    /** 更新头像 */
    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar);

    /** 更新密码 */
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /** 累加积分（原子自增） */
    int addPoints(@Param("id") Long id, @Param("delta") int delta);
}
