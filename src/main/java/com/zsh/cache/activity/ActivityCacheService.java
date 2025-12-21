package com.zsh.cache.activity;

import com.zsh.vo.ActivityVO;

import java.util.List;
import java.util.Map;

public interface ActivityCacheService {

    // 活动信息缓存
    ActivityVO getActivityById(Long activityId);
    ActivityVO getActivityByProductId(Long productId);
    void cacheActivity(ActivityVO activityVO);
    void batchCacheActivities(List<ActivityVO> activities);
    void deleteActivityCache(Long activityId);

    // 活动库存缓存
    Integer getActivityStock(Long activityId);
    void cacheActivityStock(Long activityId, Integer stock);
    boolean decreaseActivityStock(Long activityId, Integer quantity);
    boolean decreaseActivityStockWithLua(Long activityId, Integer quantity);
    void increaseActivityStock(Long activityId, Integer quantity);
    void deleteActivityStockCache(Long activityId);

    // 活动列表缓存
    List<ActivityVO> getActiveActivities();
    List<ActivityVO> getUpcomingActivities(int hours);
    void cacheActiveActivities(List<ActivityVO> activities);

    // 活动状态缓存
    boolean isActivityActive(Long activityId);
    boolean isActivityEnded(Long activityId);
    void updateActivityStatus(Long activityId, Integer status);

    // 批量操作
    Map<Long, ActivityVO> batchGetActivities(List<Long> activityIds);
    Map<Long, Integer> batchGetActivityStocks(List<Long> activityIds);

    // 缓存预热
    void preloadActiveActivities();
    void preloadActivityStocks();

    // 布隆过滤器（防缓存穿透）
    boolean mightContainActivity(Long activityId);
    void addToBloomFilter(Long activityId);

    // 统计信息
    long getActivityCacheHitCount();
    long getActivityCacheMissCount();
}
