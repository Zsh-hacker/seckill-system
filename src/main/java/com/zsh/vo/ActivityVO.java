package com.zsh.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ActivityVO {
    private Long id;
    private String name;
    private Long productId;
    private String productName;
    private BigDecimal seckillPrice;
    private BigDecimal originalPrice;
    private Integer totalStock;
    private Integer availableStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private  String statusDesc;
    private Integer limitPerUser;
    private LocalDateTime createTime;

    /**
     * 计算属性
     */
    public boolean isActive() {
        return status == 1;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }

    public boolean isStarted() {
        return LocalDateTime.now().isAfter(startTime);
    }
}
