package com.zsh.strategy.impl;

import com.zsh.cache.activity.ActivityCacheService;
import com.zsh.strategy.StockDeductionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("luaStockStrategy")
@RequiredArgsConstructor
public class LuaStockStrategy implements StockDeductionStrategy {

    private final ActivityCacheService activityCacheService;

    @Override
    public boolean deductStock(Long activityId, Integer quantity) {
        return activityCacheService.decreaseActivityStockWithLua(activityId, quantity);
    }

    @Override
    public boolean increaseStock(Long activityId, Integer quantity) {
        activityCacheService.increaseActivityStock(activityId, quantity);
        return true;
    }

    @Override
    public String getType() {
        return "lua";
    }

    @Override
    public String getDescription() {
        return "Lua脚本原子扣减库存策略";
    }
}
