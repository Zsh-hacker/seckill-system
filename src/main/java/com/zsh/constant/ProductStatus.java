package com.zsh.constant;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum ProductStatus {
    OFFLINE(0, "下架"),
    ONLINE(1, "上架");

    private final Integer code;
    private final String desc;

    private static final Map<Integer, ProductStatus> CODE_MAP = Arrays.stream(values()).collect(Collectors.toMap(ProductStatus::getCode, Function.identity()));

    ProductStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ProductStatus getByCode(Integer code) {
        return CODE_MAP.get(code);
    }
}
