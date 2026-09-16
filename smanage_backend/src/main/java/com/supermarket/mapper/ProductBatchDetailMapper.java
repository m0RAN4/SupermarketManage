package com.supermarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.entity.ProductBatchDetail;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ProductBatchDetailMapper extends BaseMapper<ProductBatchDetail> {

    @Select("SELECT * FROM product_batch_detail WHERE base_product_id = #{baseProductId} AND status = #{status} ORDER BY create_time ASC")
    List<ProductBatchDetail> selectByProductAndStatus(@Param("baseProductId") Long baseProductId, @Param("status") Integer status);

    @Select("<script>" +
            "SELECT * FROM product_batch_detail WHERE base_product_id = #{baseProductId} " +
            "AND status IN <foreach collection='statuses' item='s' open='(' separator=',' close=')'>#{s}</foreach> " +
            "ORDER BY create_time ASC" +
            "</script>")
    List<ProductBatchDetail> selectByProductAndStatuses(@Param("baseProductId") Long baseProductId, @Param("statuses") List<Integer> statuses);

    @Select("SELECT * FROM product_batch_detail WHERE base_product_id = #{baseProductId} ORDER BY create_time ASC")
    List<ProductBatchDetail> selectByBaseProductId(@Param("baseProductId") Long baseProductId);

    @Select("SELECT * FROM product_batch_detail WHERE base_product_id = #{baseProductId} AND status IN (0, 1) ORDER BY create_time ASC LIMIT #{limit}")
    List<ProductBatchDetail> selectEarliestAvailable(@Param("baseProductId") Long baseProductId, @Param("limit") Integer limit);

    @Select("SELECT COALESCE(COUNT(*), 0) FROM product_batch_detail WHERE base_product_id = #{baseProductId} AND status = #{status}")
    Integer countByProductAndStatus(@Param("baseProductId") Long baseProductId, @Param("status") Integer status);

    @Select("SELECT * FROM product_batch_detail WHERE shelf_id = #{shelfId} AND status = #{status}")
    List<ProductBatchDetail> selectByShelfAndStatus(@Param("shelfId") Long shelfId, @Param("status") Integer status);

    @Select("SELECT * FROM product_batch_detail WHERE belong_order_item_id = #{orderItemId} AND status = 2")
    List<ProductBatchDetail> selectByOrderItemId(@Param("orderItemId") Long orderItemId);

    @Update("<script>" +
            "UPDATE product_batch_detail SET status = #{status}, update_time = NOW() " +
            "WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);

    @Update("UPDATE product_batch_detail SET shelf_id = #{shelfId}, shelf_layer = #{shelfLayer}, " +
            "shelf_display_name = #{shelfDisplayName}, sale_price = #{salePrice}, " +
            "discount_price = #{discountPrice}, status = 1, update_time = NOW() " +
            "WHERE id = #{id} AND status = 0")
    int shelfOn(@Param("id") Long id, @Param("shelfId") Long shelfId,
                @Param("shelfLayer") Integer shelfLayer,
                @Param("shelfDisplayName") String shelfDisplayName,
                @Param("salePrice") BigDecimal salePrice,
                @Param("discountPrice") BigDecimal discountPrice);

    @Update("UPDATE product_batch_detail SET shelf_id = NULL, shelf_layer = NULL, " +
            "shelf_display_name = NULL, status = 0, update_time = NOW() " +
            "WHERE id = #{id} AND status = 1")
    int shelfOff(@Param("id") Long id);

    @Update("UPDATE product_batch_detail SET sale_price = #{salePrice}, " +
            "discount_price = #{discountPrice}, update_time = NOW() WHERE id = #{id}")
    int updatePrice(@Param("id") Long id, @Param("salePrice") BigDecimal salePrice,
                    @Param("discountPrice") BigDecimal discountPrice);

    @Update("UPDATE product_batch_detail SET shelf_id = #{shelfId}, shelf_layer = #{shelfLayer}, " +
            "update_time = NOW() WHERE id = #{id} AND status = 1")
    int changeShelf(@Param("id") Long id, @Param("shelfId") Long shelfId,
                    @Param("shelfLayer") Integer shelfLayer);

    @Select("SELECT * FROM product_batch_detail WHERE purchase_record_id = #{purchaseRecordId}")
    List<ProductBatchDetail> selectByPurchaseRecordId(@Param("purchaseRecordId") Long purchaseRecordId);

    @Update("UPDATE product_batch_detail SET status = 2, belong_order_item_id = #{orderItemId}, update_time = NOW() WHERE id = #{id} AND status IN (0, 1, 5)")
    int markAsSold(@Param("id") Long id, @Param("orderItemId") Long orderItemId);

    /** 下单锁定：状态 0/1 → 5，记录关联订单明细ID */
    @Update("UPDATE product_batch_detail SET status = 5, belong_order_item_id = #{orderItemId}, update_time = NOW() WHERE id = #{id} AND status IN (0, 1)")
    int markAsLocked(@Param("id") Long id, @Param("orderItemId") Long orderItemId);

    /** 取消订单解锁：恢复上架状态（5→1），保留货架信息和位置 */
    @Update("UPDATE product_batch_detail SET status = 1, belong_order_item_id = NULL, update_time = NOW() WHERE id = #{id} AND status = 5")
    int markAsUnlocked(@Param("id") Long id);

    /** 收银台点击加入购物车：状态 1 → 5（锁定，不关联订单） */
    @Update("UPDATE product_batch_detail SET status = 5, update_time = NOW() WHERE id = #{id} AND status = 1")
    int reserveForCart(@Param("id") Long id);

    /** 收银台从购物车移除：状态 5 → 1（解锁，不关联订单） */
    @Update("UPDATE product_batch_detail SET status = 1, update_time = NOW() WHERE id = #{id} AND status = 5")
    int releaseFromCart(@Param("id") Long id);

    /** 下单时将购物车锁定批次关联到订单明细 */
    @Update("UPDATE product_batch_detail SET belong_order_item_id = #{orderItemId}, update_time = NOW() WHERE id = #{id} AND status = 5")
    int bindToOrderItem(@Param("id") Long id, @Param("orderItemId") Long orderItemId);

    @Update("UPDATE product_batch_detail SET status = 3, update_time = NOW() WHERE id = #{id} AND status IN (0, 1, 2)")
    int markAsLoss(@Param("id") Long id);

    @Update("UPDATE product_batch_detail SET status = 1, belong_order_item_id = NULL, update_time = NOW() WHERE id = #{id} AND status = 2")
    int markAsReturnedToStock(@Param("id") Long id);

    @Select("SELECT status, COUNT(*) as cnt FROM product_batch_detail WHERE base_product_id = #{baseProductId} GROUP BY status")
    List<java.util.Map<String, Object>> countGroupByStatus(@Param("baseProductId") Long baseProductId);

    @Select("SELECT COALESCE(SUM(purchase_unit_price), 0) FROM product_batch_detail " +
            "WHERE status = 2 AND YEAR(update_time) = #{year} AND MONTH(update_time) = #{month}")
    BigDecimal selectMonthlySoldCostTotal(@Param("year") Integer year, @Param("month") Integer month);

    @Select("SELECT COALESCE(SUM(purchase_unit_price), 0) FROM product_batch_detail " +
            "WHERE status = 3 AND YEAR(update_time) = #{year} AND MONTH(update_time) = #{month}")
    BigDecimal selectMonthlyLossCostTotal(@Param("year") Integer year, @Param("month") Integer month);
}
