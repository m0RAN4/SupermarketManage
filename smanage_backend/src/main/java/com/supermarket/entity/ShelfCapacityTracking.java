package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("shelf_capacity_tracking")
public class ShelfCapacityTracking {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联货架ID */
    private Long shelfId;

    /** 层数编号 */
    private Integer shelfLayer;

    /** 该层已使用数量 */
    private Integer usedCount;

    /** 该层最大容量 */
    private Integer maxCapacity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
