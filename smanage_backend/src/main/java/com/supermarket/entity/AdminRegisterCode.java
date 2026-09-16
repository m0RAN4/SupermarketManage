package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理员注册校验码表实体
 */
@Data
@TableName("admin_register_code")
public class AdminRegisterCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 管理员注册校验码 */
    private String code;

    /** 总可使用次数 */
    private Integer totalUsableTimes;

    /** 已使用次数 */
    private Integer usedTimes;

    /** 是否有效：0-无效 1-有效 */
    private Integer isValid;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
