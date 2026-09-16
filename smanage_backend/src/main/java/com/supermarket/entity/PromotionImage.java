package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 宣传图片表实体
 */
@Data
@TableName("promotion_image")
public class PromotionImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 宣传图片URL（picture文件夹下的文件名或完整路径） */
    private String imageUrl;

    /** 图片展示定位数据JSON，如 {"x":10,"y":20,"w":80,"h":60}，百分比 */
    private String cropData;

    /** 使用状态：0-已禁用 1-启用中 */
    private Integer isActive;

    /** 逻辑删除：0-未删除 1-已删除（软删除，MP自动过滤） */
    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
