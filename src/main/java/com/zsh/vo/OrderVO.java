package com.zsh.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {
    private String orderNo;
    private Long userId;
    private Long activityId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal seckillPrice;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusDesc;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
}
