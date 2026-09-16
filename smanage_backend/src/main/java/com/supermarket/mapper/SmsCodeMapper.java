package com.supermarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.entity.SmsCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SmsCodeMapper extends BaseMapper<SmsCode> {

    @Update("UPDATE sms_code SET is_used = 1 WHERE phone = #{phone} AND is_used = 0 AND expire_time > NOW()")
    int invalidateOldCodes(@Param("phone") String phone);

    @Select("SELECT * FROM sms_code WHERE phone = #{phone} AND is_used = 0 AND expire_time > NOW() " +
            "ORDER BY create_time DESC LIMIT 1")
    SmsCode selectLatestValidByPhone(@Param("phone") String phone);
}
