package com.zsh.strategy;

public interface StockDeductionStrategy {

    /**
     * 扣减库存
     */
    boolean deductStock(Long activityId, Integer quantity);

    /**
     * 增加库存
     */
    boolean increaseStock(Long activityId, Integer quantity);

    /**
     * 获取策略类型
     */
    String getType();

    /**
     * 策略描述
     */
    String getDescription();
}
