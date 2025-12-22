package com.zsh.factory;

import com.zsh.strategy.StockDeductionStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StockStrategyFactory {

    private final Map<String, StockDeductionStrategy> strategyMap = new ConcurrentHashMap<>();

    @Autowired
    public StockStrategyFactory(Map<String, StockDeductionStrategy> strategies) {
        strategies.forEach((beanName, strategy) -> {
            strategyMap.put(strategy.getType(), strategy);
        });
    }

    public StockDeductionStrategy getStrategy(String type) {
        StockDeductionStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No stock strategy found for type: " + type);
        }
        return strategy;
    }

    public StockDeductionStrategy getDefaultStrategy() {
        return getStrategy("redis");    // 默认使用Redis策略
    }
}
