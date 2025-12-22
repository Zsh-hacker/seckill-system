package com.zsh.runner;

import com.zsh.cache.activity.ActivityCacheService;
import com.zsh.cache.product.ProductCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)   // 优先级，越小优先级越高
@RequiredArgsConstructor
public class CachePreheatRunner implements CommandLineRunner {

    private final ProductCacheService productCacheService;
    private final ActivityCacheService activityCacheService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting cache preheating...");
        long startTime = System.currentTimeMillis();

        try {
            // 预热商品缓存
            log.info("Preheating product cache...");
            productCacheService.preloadHotProducts();
            productCacheService.preloadProductStocks();

            // 预热活动缓存
            log.info("Preheating activity cache...");
            activityCacheService.preloadActiveActivities();
            activityCacheService.preloadActivityStocks();

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("Cache preheating completed in {} ms", elapsedTime);
        } catch (Exception e) {
            log.error("Cache preheating failed", e);
        }
    }
}
