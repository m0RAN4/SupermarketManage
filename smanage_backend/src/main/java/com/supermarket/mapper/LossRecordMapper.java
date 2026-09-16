package com.supermarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.entity.LossRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface LossRecordMapper extends BaseMapper<LossRecord> {

    /** 月度损耗总金额 */
    @Select("SELECT COALESCE(SUM(loss_total_amount), 0) FROM loss_record " +
            "WHERE YEAR(create_time) = #{year} AND MONTH(create_time) = #{month}")
    BigDecimal selectMonthlyLossTotal(@Param("year") Integer year, @Param("month") Integer month);
}
