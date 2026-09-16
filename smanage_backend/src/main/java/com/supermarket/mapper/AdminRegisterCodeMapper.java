package com.supermarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.entity.AdminRegisterCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminRegisterCodeMapper extends BaseMapper<AdminRegisterCode> {

    /** 查询有效且可用次数未用完的校验码 */
    @Select("SELECT * FROM admin_register_code WHERE code = #{code} AND is_valid = 1 AND used_times < total_usable_times")
    AdminRegisterCode selectValidByCode(@Param("code") String code);
}
