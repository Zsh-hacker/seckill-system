package com.zsh.strategy.impl;

import com.zsh.dao.SeckillActivityDao;
import com.zsh.entity.SeckillActivity;
import com.zsh.strategy.StockDeductionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("databaseStockStrategy")
@RequiredArgsConstructor
public class DatabaseStockStrategy implements StockDeductionStrategy {

    private final SeckillActivityDao activityDao;

    @Override
    public boolean deductStock(Long activityId, Integer quantity) {
        int rows = activityDao.deductStock(activityId, quantity);
        return rows > 0;
    }

    @Override
    public boolean increaseStock(Long activityId, Integer quantity) {
        // 数据库增加库存实现
        SeckillActivity activity = activityDao.selectById(activityId);
        if (activity != null) {
            activity.setAvailableStock(activity.getAvailableStock() + quantity);
            activityDao.updateById(activity);
            return true;
        }
        return false;
    }

    @Override
    public String getType() {
        return "database";
    }

    @Override
    public String getDescription() {
        return "数据库乐观锁扣减库存策略";
    }
}
