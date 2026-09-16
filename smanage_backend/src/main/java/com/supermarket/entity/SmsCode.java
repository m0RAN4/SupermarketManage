package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 短信验证码表实体
 */
@Data
@TableName("sms_code")
public class SmsCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 11位纯手机号 */
    private String phone;

    /** 关联用户ID，未注册用户可为空 */
    private Long userId;

    /** 6位数字验证码 */
    private String code;

    /** 验证码过期时间 */
    private LocalDateTime expireTime;

    /** 是否已使用：0-未使用 1-已使用 */
    private Integer isUsed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
