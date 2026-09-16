package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品损耗统计表实体
 */
@Data
@TableName("loss_record")
public class LossRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联批次明细ID，损耗成本从对应批次进货单价取值 */
    private Long batchDetailId;

    /** 损耗原因，如破损、过期、丢失 */
    private String lossReason;

    /** 损耗数量 */
    private Integer lossQuantity;

    /** 损耗单价（按成本价计算） */
    private BigDecimal lossUnitPrice;

    /** 损耗总金额 */
    private BigDecimal lossTotalAmount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
