package com.supermarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.entity.SaleOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface SaleOrderMapper extends BaseMapper<SaleOrder> {

    /** 月度已支付/部分退款/已退款订单总金额 */
    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM sale_order " +
            "WHERE pay_status IN (1, 2, 4) AND YEAR(create_time) = #{year} AND MONTH(create_time) = #{month}")
    BigDecimal selectMonthlyPaidTotal(@Param("year") Integer year, @Param("month") Integer month);
}
