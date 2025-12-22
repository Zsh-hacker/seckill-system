package com.zsh.strategy.impl;

import com.zsh.cache.activity.ActivityCacheService;
import com.zsh.strategy.StockDeductionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("redisStockStrategy")
@RequiredArgsConstructor
public class RedisStockStrategy implements StockDeductionStrategy {

    private final ActivityCacheService activityCacheService;

    @Override
    public boolean deductStock(Long activityId, Integer quantity) {
        return activityCacheService.decreaseActivityStock(activityId, quantity);
    }

    @Override
    public boolean increaseStock(Long activityId, Integer quantity) {
        activityCacheService.increaseActivityStock(activityId, quantity);
        return true;
    }

    @Override
    public String getType() {
        return "redis";
    }

    @Override
    public String getDescription() {
        return "Redis缓存扣减库存策略";
    }
}
