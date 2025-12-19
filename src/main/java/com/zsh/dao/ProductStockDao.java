package com.zsh.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zsh.entity.ProductStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductStockDao extends BaseMapper<ProductStock> {
    /**
     * 扣减可用库存（使用乐观锁）
     */
    @Update("UPDATE product_stock SET available_stock = available_stock - #{quantity}, " +
    "version = version + 1 WHERE product_id = #{product_id} AND available_stock >= #{quantity}")
    int deductAvailableStock(@Param("product_id") Long productId,
                             @Param("quantity") Integer quantity);

    /**
     * 增加锁定库存
     */
    @Update("UPDATE product_stock SET locked_stock = locked_stock + #{quantity}, " +
    "version = version + 1 WHERE product_id = #{productId}")
    int increaseLockedStock(@Param("productId") Long productId,
                            @Param("quantity") Integer quantity);

    /**
     * 释放锁定库存（回滚时使用）
     */
    @Update("UPDATE product_stock SET locked_stock = locked_stock - #{quantity}, " +
    "available_stock = available_stock + #{quantity}, " +
    "version = version + 1 WHERE product_id = #{productId} AND locked_stock >= #{quantity}")
    int releaseLockedStock(@Param("productId") Long productId,
                           @Param("quantity") Integer quantity);
}
