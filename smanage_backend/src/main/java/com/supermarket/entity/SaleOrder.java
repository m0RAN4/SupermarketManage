package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单主表实体
 */
@Data
@TableName("sale_order")
public class SaleOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID（关联user_info） */
    private Long userId;

    /** 订单支付总金额 */
    private BigDecimal payAmount;

    /**
     * 支付状态：
     * 0 = 未支付
     * 1 = 已支付
     * 2 = 已退款（全部退款）
     * 3 = 已取消
     * 4 = 部分退款（有个别商品退款但不是全部）
     */
    private Integer payStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
