package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品批次明细表实体 - 库存核心明细表
 * 每条记录代表一个独立的库存物品，所有库存状态、成本、货架归属都以该表数据为准
 */
@Data
@TableName("product_batch_detail")
public class ProductBatchDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联进货记录ID */
    private Long purchaseRecordId;

    /** 关联商品基础档案ID */
    private Long baseProductId;

    /** 关联货架ID，NULL=未上架 */
    private Long shelfId;

    /** 所在货架层数，NULL=未分配层数 */
    private Integer shelfLayer;

    /** 货架展示名称（上架时填写，下架时清空，进货时不填） */
    private String shelfDisplayName;

    /** 进货成本单价（写入后永久不变） */
    private BigDecimal purchaseUnitPrice;

    /** 销售单价，可在库存管理中修改 */
    private BigDecimal salePrice;

    /** 折扣价格，可为空 */
    private BigDecimal discountPrice;

    /**
     * 商品状态：
     * 0 = 在库未上架
     * 1 = 已上架在售
     * 2 = 已售出
     * 3 = 已损耗
     * 4 = 退货待处理
     * 5 = 待支付锁定（下单后锁定，支付→2，取消→0/1）
     */
    private Integer status;

    /** 关联销售订单详情ID（售出时写入，退货回库时清空以便下次售出使用） */
    private Long belongOrderItemId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
