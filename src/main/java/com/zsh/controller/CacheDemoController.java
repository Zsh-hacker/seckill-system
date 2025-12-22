package com.zsh.controller;

import com.zsh.cache.activity.ActivityCacheService;
import com.zsh.cache.product.ProductCacheService;
import com.zsh.vo.ActivityVO;
import com.zsh.vo.ProductVO;
import com.zsh.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/cache-demo")
@RequiredArgsConstructor
public class CacheDemoController {

    private final ProductCacheService productCacheService;
    private final ActivityCacheService activityCacheService;

    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("CacheDemoController");
    }

    @GetMapping("/product/{id}")
    public Result<ProductVO> getProduct(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();

        ProductVO product = productCacheService.getProductById(id);
        Integer stock = productCacheService.getProductStock(id);
        if (product != null && stock != null) {
            product.setAvailableStock(stock);
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("Get product {} took {} ms", id, elapsedTime);

        return Result.success(product);
    }

    @GetMapping("/activity/{id}")
    public Result<ActivityVO> getActivity(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();

        ActivityVO activity = activityCacheService.getActivityById(id);
        Integer stock = activityCacheService.getActivityStock(id);
        if (activity != null && stock != null) {
            // stock在ActivityVO中已有对应字段
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("Get activity {} took {} ms", id, elapsedTime);

        return Result.success(activity);
    }

    @GetMapping("/activities/active")
    public Result<List<ActivityVO>> getActiveActivities() {
        List<ActivityVO> activities = activityCacheService.getActiveActivities();
        return Result.success(activities);
    }

    @PostMapping("/product/{id}/stock/decrease")
    public Result<Boolean> decreaseProductStock(@PathVariable Long id,
                                                @RequestParam Integer quantity) {
        productCacheService.decreaseProductStock(id, quantity);
        return Result.success(true);
    }

    @PostMapping("/activity/{id}/stock/decrease")
    public Result<Boolean> decreaseActivityStock(@PathVariable Long id,
                                                 @RequestParam Integer quantity) {
        boolean success = activityCacheService.decreaseActivityStock(id, quantity);
        return Result.success(success);
    }

    @GetMapping("/product/batch")
    public Result<Map<Long, ProductVO>> batchGetProducts(@RequestParam List<Long> ids) {
        Map<Long, ProductVO> products = productCacheService.batchGetProducts(ids);
        return Result.success(products);
    }

    @GetMapping("/activity/batch")
    public Result<Map<Long, ActivityVO>> batchGetActivities(@RequestParam List<Long> ids) {
        Map<Long, ActivityVO> activities = activityCacheService.batchGetActivities(ids);
        return Result.success(activities);
    }

    @GetMapping("/bloom-filter/{id}")
    public Result<Boolean> checkBloomFilter(@PathVariable Long id) {
        boolean mightContain = activityCacheService.mightContainActivity(id);
        return Result.success(mightContain);
    }

    @PostMapping("/preheat")
    public Result<String> triggerPreheat() {
        new Thread(() -> {
            try {
                productCacheService.preloadHotProducts();
                activityCacheService.preloadActiveActivities();
            } catch (Exception e) {
                log.error("Manual preheat failed", e);
            }
        }).start();

        return Result.success("Preheat started in background");
    }

    @GetMapping("/stats/product")
    public Result<Map<String, Long>> getProductCacheStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("hitCount", productCacheService.getProductCacheHitCount());
        stats.put("missCount", productCacheService.getProductCacheMissCount());
        return Result.success(stats);
    }

    @GetMapping("/stats/activity")
    public Result<Map<String, Long>> getActivityCacheStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("hitCount", activityCacheService.getActivityCacheHitCount());
        stats.put("missCount", activityCacheService.getActivityCacheMissCount());
        return Result.success(stats);
    }
}