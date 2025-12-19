package com.zsh.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_stock")
public class ProductStock extends BaseEntity{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Integer totalStock;
    private Integer availableStock;
    private Integer lockedStock;

    @Version
    private Integer version;
}
