package com.zsh.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_order")
public class SeckillOrder extends BaseEntity{
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long activityId;
    private Long productId;
    private Integer quantity;
    private BigDecimal seckillPrice;
    private BigDecimal totalAmount;
    private Integer status; // 0-待支付 1-已支付 2-已取消 3-已退款
    private LocalDateTime payTime;
}
