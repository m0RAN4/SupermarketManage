package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 进货入库记录表实体
 */
@Data
@TableName("purchase_record")
public class PurchaseRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联商品基础档案ID */
    private Long baseProductId;

    /** 进货数量 */
    private Integer purchaseQuantity;

    /** 进货单价（成本价） */
    private BigDecimal purchaseUnitPrice;

    /** 进货总金额 */
    private BigDecimal purchaseTotalAmount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
