package com.supermarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户账户表实体
 */
@Data
@TableName("user_info")
public class UserInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，用于登录 */
    private String username;

    /** 用户类型：0-管理员 1-普通用户 */
    private Integer userType;

    /** 密码，BCrypt加密存储 */
    private String password;

    /** 手机号，注销时追加-数字后缀 */
    private String phone;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
