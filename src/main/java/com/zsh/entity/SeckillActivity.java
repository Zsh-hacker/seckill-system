package com.zsh.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_activity")
public class SeckillActivity extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long productId;
    private BigDecimal seckillPrice;
    private BigDecimal originalPrice;
    private Integer totalStock;
    private Integer availableStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status; // 0-未开始 1-进行中 2-已结束 3-已关闭

    @Version
    private Integer version;
}
