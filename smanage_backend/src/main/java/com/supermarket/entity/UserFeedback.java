package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户反馈表实体
 */
@Data
@TableName("user_feedback")
public class UserFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交用户ID */
    private Long userId;

    /** 反馈内容 */
    private String content;

    /** 管理员回复 */
    private String reply;

    /** 处理状态：0=未处理 1=已回复 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
