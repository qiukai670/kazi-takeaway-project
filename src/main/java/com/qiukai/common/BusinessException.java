package com.qiukai.common;

/**
 * 业务异常
 * 用于在业务逻辑中抛出可预期的错误，由全局异常处理器统一捕获并返回友好提示
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
