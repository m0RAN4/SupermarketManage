package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 货架信息表实体
 */
@Data
@TableName("shelf_info")
public class ShelfInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 货架编号，如A-01、B-03 */
    private String shelfCode;

    /** 所属区域，如食品区、日化区 */
    private String shelfArea;

    /** 货架层数 */
    private Integer shelfLayer;

    /** 每层货物容量（每层能放多少件商品） */
    private Integer layerCapacity;

    /** 货架总容量 = shelfLayer × layerCapacity，自动计算，可保留字段 */
    private Integer totalCapacity;

    /** 启用状态：0-禁用 1-启用 */
    private Integer isEnabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
