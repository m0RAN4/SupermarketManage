package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单详情表实体 - 每个订单项关联具体批次明细
 */
@Data
@TableName("sale_order_item")
public class SaleOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联订单ID */
    private Long orderId;

    /** 关联商品基础档案ID */
    private Long baseProductId;

    /** 关联批次明细ID，用于成本追溯 */
    private Long batchDetailId;

    /** 购买数量 */
    private Integer buyQuantity;

    /** 成交单价快照 */
    private BigDecimal buyUnitPrice;

    /** 单项总金额 */
    private BigDecimal itemTotalAmount;

    /** 累计退货数量 */
    private Integer refundQuantity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
