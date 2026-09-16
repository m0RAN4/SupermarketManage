package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品基础登记表实体
 */
@Data
@TableName("product_base")
public class ProductBase {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联商品分类ID */
    private Long categoryId;

    /** 商品标准名称 */
    private String productName;

    /** 商品图片URL（picture文件夹下的文件名或完整路径） */
    private String imageUrl;

    /** 图片展示定位数据JSON，如 {"x":10,"y":20,"w":80,"h":60}，百分比 */
    private String cropData;

    /** 启用状态：0-禁用 1-启用 */
    private Integer isEnabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
