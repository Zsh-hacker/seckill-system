package com.zsh.constant;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),

    // 业务错误码 1000-1999
    ACTIVITY_NOT_FOUND(1000, "活动不存在"),
    ACTIVITY_NOT_STARTED(1001, "活动未开始"),
    ACTIVITY_ENDED(1002, "活动已结束"),
    ACTIVITY_CLOSED(1003, "活动已关闭"),
    STOCK_NOT_ENOUGH(1004, "库存不足"),
    USER_LIMIT_EXCEEDED(1005, "超出用户限购数量"),
    USER_ALREADY_PARTICIPATED(1006, "用户已参与活动"),
    ORDER_NOT_FOUND(1007, "订单不存在"),
    ORDER_STATUS_ERROR(1008, "订单状态错误"),

    // 系统错误码 2000-2999
    SYSTEM_ERROR(2000, "系统错误"),
    DATABASE_ERROR(2001, "数据库错误"),
    CACHE_ERROR(2002, "缓存错误"),
    RATE_LIMIT_EXCEEDED(2003, "请求过于频繁"),
    CIRCUIT_BREAKER_OPEN(2004, "服务熔断"),

    // 第三方错误码 3000-3999
    PAYMENT_ERROR(3000, "支付失败"),
    SMS_SEND_ERROR(3001, "短信发送失败");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
