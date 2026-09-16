package com.supermarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.entity.SaleRefund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface SaleRefundMapper extends BaseMapper<SaleRefund> {

    /** 月度退款总金额 */
    @Select("SELECT COALESCE(SUM(refund_total_amount), 0) FROM sale_refund " +
            "WHERE refund_handle_status = 1 AND YEAR(create_time) = #{year} AND MONTH(create_time) = #{month}")
    BigDecimal selectMonthlyRefundTotal(@Param("year") Integer year, @Param("month") Integer month);

    /** 按订单详情ID查询所有退款记录 */
    @Select("SELECT * FROM sale_refund WHERE order_item_id = #{orderItemId} ORDER BY create_time DESC")
    java.util.List<SaleRefund> selectByOrderItemId(@Param("orderItemId") Long orderItemId);

    /** 月度退货商品进货成本合计（从已同意的退款关联批次成本计算） */
    @Select("SELECT COALESCE(SUM(bd.purchase_unit_price * sr.refund_quantity), 0) " +
            "FROM sale_refund sr " +
            "JOIN product_batch_detail bd ON sr.batch_detail_id = bd.id " +
            "WHERE sr.refund_handle_status = 1 " +
            "AND YEAR(sr.update_time) = #{year} AND MONTH(sr.update_time) = #{month}")
    BigDecimal selectMonthlyReturnCostTotal(@Param("year") Integer year, @Param("month") Integer month);

    /** 月度退货转损耗进货成本合计（已同意的退款中转为损耗部分的进货成本） */
    @Select("SELECT COALESCE(SUM(bd.purchase_unit_price * sr.to_loss_quantity), 0) " +
            "FROM sale_refund sr " +
            "JOIN product_batch_detail bd ON sr.batch_detail_id = bd.id " +
            "WHERE sr.refund_handle_status = 1 AND sr.to_loss_quantity > 0 " +
            "AND YEAR(sr.update_time) = #{year} AND MONTH(sr.update_time) = #{month}")
    BigDecimal selectMonthlyReturnLossCostTotal(@Param("year") Integer year, @Param("month") Integer month);
}
