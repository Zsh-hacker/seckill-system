package com.zsh.constant;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum ActivityStatus {
    NOT_STARTED(0, "未开始"),
    ACTIVE(1, "进行中"),
    ENDED(2, "已结束"),
    CLOSED(3, "已关闭");

    private final Integer code;
    private final String desc;

    private static final Map<Integer, ActivityStatus> CODE_MAP = Arrays.stream(values()).collect(Collectors.toMap(ActivityStatus::getCode, Function.identity()));

    ActivityStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ActivityStatus getByCode(Integer code) {
        return CODE_MAP.get(code);
    }

    public static boolean isActive(Integer code) {
        return ACTIVE.code.equals(code);
    }
}
