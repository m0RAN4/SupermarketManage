package com.supermarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    /** 查询某手机号前缀下已注销手机号的最大后缀序号 */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(phone, LOCATE('-', phone) + 1) AS UNSIGNED)), 0) " +
            "FROM user_info WHERE phone REGEXP CONCAT(#{phonePattern}, '-[0-9]+$')")
    Integer selectMaxSuffixByPhonePrefix(@Param("phonePattern") String phonePattern);
}
