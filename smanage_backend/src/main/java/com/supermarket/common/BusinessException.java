package com.supermarket.common;

import lombok.Getter;

/**
 * 业务异常类，用于在Service层抛出业务校验异常，由全局异常处理器捕获
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
