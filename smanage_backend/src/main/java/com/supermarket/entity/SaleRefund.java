package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售退货记录表实体 - 每条退货记录对应一个订单项的退货
 */
@Data
@TableName("sale_refund")
public class SaleRefund {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联原订单ID */
    private Long orderId;

    /** 关联原订单详情ID */
    private Long orderItemId;

    /** 关联批次明细ID */
    private Long batchDetailId;

    /** 退货操作人ID */
    private Long operatorId;

    /** 本次退货数量 */
    private Integer refundQuantity;

    /** 退货单价（按原成交价退款） */
    private BigDecimal refundUnitPrice;

    /** 本次退款总金额 */
    private BigDecimal refundTotalAmount;

    /** 退货原因 */
    private String refundReason;

    /** 处理状态：0=待审核 1=已同意 2=已拒绝 */
    private Integer refundHandleStatus;

    /** 管理员审核时填写的回库数量 */
    private Integer backStockQuantity;

    /** 管理员审核时填写的转损耗数量 */
    private Integer toLossQuantity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
