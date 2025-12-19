package com.zsh.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillRequestDTO {
    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Min(value = 1, message = "购买数量不能小于1")
    private Integer quantity = 1;

    private String requestId;   // 请求ID，用于幂等性

}
