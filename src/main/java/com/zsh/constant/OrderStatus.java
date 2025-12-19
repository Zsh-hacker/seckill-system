package com.zsh.constant;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum OrderStatus {
    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    CANCELED(2, "已取消"),
    REFUNDED(3, "已退款");

    private final Integer code;
    private final String desc;

    private static final Map<Integer, OrderStatus> CODE_MAP = Arrays.stream(values()).collect(Collectors.toMap(OrderStatus::getCode, Function.identity()));

    OrderStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatus getByCode(Integer code) {
        return CODE_MAP.get(code);
    }

    public static boolean isValid(Integer code) {
        return CODE_MAP.containsKey(code);
    }

}
