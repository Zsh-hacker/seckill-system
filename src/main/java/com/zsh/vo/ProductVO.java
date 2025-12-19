package com.zsh.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Integer status;
    private Integer availableStock;
    private Long activityId;    // 关联活动ID
}
