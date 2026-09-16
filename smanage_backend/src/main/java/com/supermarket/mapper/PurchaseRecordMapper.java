package com.supermarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.entity.PurchaseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface PurchaseRecordMapper extends BaseMapper<PurchaseRecord> {

    /** 查询某商品最近一次进货单价 */
    @Select("SELECT purchase_unit_price FROM purchase_record WHERE base_product_id = #{baseProductId} ORDER BY create_time DESC LIMIT 1")
    BigDecimal selectLatestPurchasePrice(@Param("baseProductId") Long baseProductId);

    /** 月度进货总金额 */
    @Select("SELECT COALESCE(SUM(purchase_total_amount), 0) FROM purchase_record " +
            "WHERE YEAR(create_time) = #{year} AND MONTH(create_time) = #{month}")
    BigDecimal selectMonthlyPurchaseTotal(@Param("year") Integer year, @Param("month") Integer month);
}
