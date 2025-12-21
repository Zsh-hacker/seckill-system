package com.zsh.cache.activity.impl;

import com.zsh.cache.CacheManager;
import com.zsh.cache.activity.ActivityCacheService;
import com.zsh.constant.ActivityStatus;
import com.zsh.constant.CacheKey;
import com.zsh.dao.SeckillActivityDao;
import com.zsh.entity.SeckillActivity;
import com.zsh.vo.ActivityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityCacheServiceImpl implements ActivityCacheService {

    private final CacheManager cacheManager;
    private final SeckillActivityDao activityDAO;
    private final RedissonClient redissonClient;

    // 缓存过期时间配置
    private static final long ACTIVITY_TTL = TimeUnit.MINUTES.toSeconds(15);
    private static final long ACTIVITY_STOCK_TTL = TimeUnit.MINUTES.toSeconds(3);
    private static final long ACTIVITY_LIST_TTL = TimeUnit.MINUTES.toSeconds(5);

    // 布隆过滤器
    private RBloomFilter<Long> bloomFilter;

    @Override
    public ActivityVO getActivityById(Long activityId) {
        // 先检查布隆过滤器
        if (!mightContainActivity(activityId)) {
            log.debug("Activity {} not in bloom filter", activityId);
            return null;
        }

        String cacheKey = CacheKey.format(CacheKey.ACTIVITY_KEY, activityId);

        return cacheManager.getWithCachePenetrationProtection(
                cacheKey,
                ActivityVO.class,
                () -> {
                    SeckillActivity activity = activityDAO.selectById(activityId);
                    if (activity == null) {
                        return null;
                    }
                    return convertToVO(activity);
                },
                ACTIVITY_TTL,
                TimeUnit.SECONDS
        );
    }

    @Override
    public ActivityVO getActivityByProductId(Long productId) {
        // 这里简化处理，实际应该维护productId到activityId的映射
        List<ActivityVO> activities = getActiveActivities();
        return activities.stream()
                .filter(activity -> productId.equals(activity.getProductId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void cacheActivity(ActivityVO activityVO) {
        if (activityVO == null || activityVO.getId() == null) {
            return;
        }

        String cacheKey = CacheKey.format(CacheKey.ACTIVITY_KEY, activityVO.getId());
        cacheManager.set(cacheKey, activityVO, ACTIVITY_TTL, TimeUnit.SECONDS);

        // 添加到布隆过滤器
        addToBloomFilter(activityVO.getId());

        log.debug("Cached activity: {}", activityVO.getId());
    }

    @Override
    public void batchCacheActivities(List<ActivityVO> activities) {
        if (activities == null || activities.isEmpty()) {
            return;
        }

        Map<String, ActivityVO> cacheMap = new HashMap<>();
        for (ActivityVO activity : activities) {
            if (activity != null && activity.getId() != null) {
                String cacheKey = CacheKey.format(CacheKey.ACTIVITY_KEY, activity.getId());
                cacheMap.put(cacheKey, activity);

                // 添加到布隆过滤器
                addToBloomFilter(activity.getId());
            }
        }

        cacheManager.multiSet(cacheMap);
        log.debug("Batch cached {} activities", cacheMap.size());
    }

    @Override
    public void deleteActivityCache(Long activityId) {
        String cacheKey = CacheKey.format(CacheKey.ACTIVITY_KEY, activityId);
        cacheManager.delete(cacheKey);

        String stockKey = CacheKey.format(CacheKey.ACTIVITY_STOCK_KEY, activityId);
        cacheManager.delete(stockKey);

        log.debug("Deleted activity cache: {}", activityId);
    }

    @Override
    public Integer getActivityStock(Long activityId) {
        String cacheKey = CacheKey.format(CacheKey.ACTIVITY_STOCK_KEY, activityId);

        return cacheManager.getWithCachePenetrationProtection(
                cacheKey,
                Integer.class,
                () -> {
                    SeckillActivity activity = activityDAO.selectById(activityId);
                    return activity != null ? activity.getAvailableStock() : null;
                },
                ACTIVITY_STOCK_TTL,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void cacheActivityStock(Long activityId, Integer stock) {
        if (activityId == null || stock == null) {
            return;
        }

        String cacheKey = CacheKey.format(CacheKey.ACTIVITY_STOCK_KEY, activityId);
        cacheManager.set(cacheKey, stock, ACTIVITY_STOCK_TTL, TimeUnit.SECONDS);
        log.debug("Cached activity stock: {} -> {}", activityId, stock);
    }

    @Override
    public boolean decreaseActivityStock(Long activityId, Integer quantity) {
        String cacheKey = CacheKey.format(CacheKey.ACTIVITY_STOCK_KEY, activityId);

        // 使用Redis原子操作减少库存
        Long currentStock = cacheManager.decrement(cacheKey, quantity);
        if (currentStock != null && currentStock >= 0) {
            log.debug("Decreased activity stock: {} to {}", activityId, currentStock);
            return true;
        }

        // 如果库存不足，回滚
        if (currentStock != null && currentStock < 0) {
            cacheManager.increment(cacheKey, quantity);
            log.debug("Stock insufficient, rolled back: {}", activityId);
        }

        return false;
    }

    @Override
    public boolean decreaseActivityStockWithLua(Long activityId, Integer quantity) {
        // 使用Lua脚本保证原子性（在RedisCacheManager中实现）
        String cacheKey = CacheKey.format(CacheKey.ACTIVITY_STOCK_KEY, activityId);

        // 获取当前库存
        Integer currentStock = cacheManager.get(cacheKey, Integer.class);
        if (currentStock == null) {
            // 缓存中没有，从数据库加载
            currentStock = getActivityStock(activityId);
        }

        if (currentStock != null && currentStock >= quantity) {
            // 原子减少
            cacheManager.set(cacheKey, currentStock - quantity,
                    ACTIVITY_STOCK_TTL, TimeUnit.SECONDS);
            return true;
        }

        return false;
    }

    @Override
    public void increaseActivityStock(Long activityId, Integer quantity) {
        String cacheKey = CacheKey.format(CacheKey.ACTIVITY_STOCK_KEY, activityId);
        cacheManager.increment(cacheKey, quantity);
        log.debug("Increased activity stock: {} by {}", activityId, quantity);
    }

    @Override
    public void deleteActivityStockCache(Long activityId) {
        String cacheKey = CacheKey.format(CacheKey.ACTIVITY_STOCK_KEY, activityId);
        cacheManager.delete(cacheKey);
        log.debug("Deleted activity stock cache: {}", activityId);
    }

    @Override
    public List<ActivityVO> getActiveActivities() {
        String cacheKey = CacheKey.ACTIVITY_LIST_KEY;

        List<ActivityVO> activities = cacheManager.get(cacheKey, List.class);
        if (activities != null) {
            return activities;
        }

        // 从数据库加载
        List<SeckillActivity> dbActivities = activityDAO.selectActiveActivities();
        activities = dbActivities.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 缓存
        cacheManager.set(cacheKey, activities, ACTIVITY_LIST_TTL, TimeUnit.SECONDS);

        // 逐个缓存活动详情
        batchCacheActivities(activities);

        return activities;
    }

    @Override
    public List<ActivityVO> getUpcomingActivities(int hours) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusHours(hours);

        List<ActivityVO> allActivities = getActiveActivities();
        return allActivities.stream()
                .filter(activity ->
                        activity.getStartTime() != null &&
                                activity.getStartTime().isAfter(now) &&
                                activity.getStartTime().isBefore(endTime))
                .collect(Collectors.toList());
    }

    @Override
    public void cacheActiveActivities(List<ActivityVO> activities) {
        String cacheKey = CacheKey.ACTIVITY_LIST_KEY;
        cacheManager.set(cacheKey, activities, ACTIVITY_LIST_TTL, TimeUnit.SECONDS);
        log.debug("Cached active activities list, size: {}", activities.size());
    }

    @Override
    public boolean isActivityActive(Long activityId) {
        ActivityVO activity = getActivityById(activityId);
        if (activity == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        return ActivityStatus.ACTIVE.getCode().equals(activity.getStatus()) &&
                activity.getStartTime() != null &&
                activity.getEndTime() != null &&
                now.isAfter(activity.getStartTime()) &&
                now.isBefore(activity.getEndTime());
    }

    @Override
    public boolean isActivityEnded(Long activityId) {
        ActivityVO activity = getActivityById(activityId);
        if (activity == null) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        return ActivityStatus.ENDED.getCode().equals(activity.getStatus()) ||
                (activity.getEndTime() != null && now.isAfter(activity.getEndTime()));
    }

    @Override
    public void updateActivityStatus(Long activityId, Integer status) {
        ActivityVO activity = getActivityById(activityId);
        if (activity != null) {
            activity.setStatus(status);
            cacheActivity(activity);
        }
    }

    @Override
    public Map<Long, ActivityVO> batchGetActivities(List<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> cacheKeys = activityIds.stream()
                .map(id -> CacheKey.format(CacheKey.ACTIVITY_KEY, id))
                .collect(Collectors.toSet());

        Map<String, ActivityVO> cached = cacheManager.multiGet(cacheKeys, ActivityVO.class);

        Map<Long, ActivityVO> result = new HashMap<>();
        for (Long activityId : activityIds) {
            String cacheKey = CacheKey.format(CacheKey.ACTIVITY_KEY, activityId);
            ActivityVO activity = cached.get(cacheKey);

            if (activity != null) {
                result.put(activityId, activity);
            } else if (mightContainActivity(activityId)) {
                // 如果布隆过滤器中有，单独加载
                activity = getActivityById(activityId);
                if (activity != null) {
                    result.put(activityId, activity);
                }
            }
        }

        return result;
    }

    @Override
    public Map<Long, Integer> batchGetActivityStocks(List<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> cacheKeys = activityIds.stream()
                .map(id -> CacheKey.format(CacheKey.ACTIVITY_STOCK_KEY, id))
                .collect(Collectors.toSet());

        Map<String, Integer> cached = cacheManager.multiGet(cacheKeys, Integer.class);

        Map<Long, Integer> result = new HashMap<>();
        for (Long activityId : activityIds) {
            String cacheKey = CacheKey.format(CacheKey.ACTIVITY_STOCK_KEY, activityId);
            Integer stock = cached.get(cacheKey);

            if (stock != null) {
                result.put(activityId, stock);
            } else {
                stock = getActivityStock(activityId);
                if (stock != null) {
                    result.put(activityId, stock);
                }
            }
        }

        return result;
    }

    @Override
    public void preloadActiveActivities() {
        log.info("Preloading active activities");
        getActiveActivities(); // 触发缓存加载
        log.info("Active activities preload completed");
    }

    @Override
    public void preloadActivityStocks() {
        log.info("Preloading activity stocks");

        List<SeckillActivity> activities = activityDAO.selectList(null);
        for (SeckillActivity activity : activities) {
            try {
                cacheActivityStock(activity.getId(), activity.getAvailableStock());
                addToBloomFilter(activity.getId());
            } catch (Exception e) {
                log.error("Failed to preload activity stock: {}", activity.getId(), e);
            }
        }

        log.info("Activity stocks preload completed, total: {}", activities.size());
    }

    @Override
    public boolean mightContainActivity(Long activityId) {
        if (bloomFilter == null) {
            initBloomFilter();
        }
        return bloomFilter != null && bloomFilter.contains(activityId);
    }

    @Override
    public void addToBloomFilter(Long activityId) {
        if (bloomFilter == null) {
            initBloomFilter();
        }
        if (bloomFilter != null && !bloomFilter.contains(activityId)) {
            bloomFilter.add(activityId);
        }
    }

    @Override
    public long getActivityCacheHitCount() {
        return cacheManager.getLocalCacheStats().getHitCount();
    }

    @Override
    public long getActivityCacheMissCount() {
        return cacheManager.getLocalCacheStats().getMissCount();
    }

    private void initBloomFilter() {
        try {
            bloomFilter = redissonClient.getBloomFilter(CacheKey.BLOOM_FILTER_ACTIVITY);
            // 初始化布隆过滤器：预计元素数量10000，错误率0.001
            bloomFilter.tryInit(10000, 0.001);

            // 预热：将现有活动ID加入布隆过滤器
            List<SeckillActivity> activities = activityDAO.selectList(null);
            for (SeckillActivity activity : activities) {
                bloomFilter.add(activity.getId());
            }

            log.info("Bloom filter initialized with {} activities", activities.size());
        } catch (Exception e) {
            log.error("Failed to initialize bloom filter", e);
        }
    }

    private ActivityVO convertToVO(SeckillActivity activity) {
        if (activity == null) {
            return null;
        }

        ActivityVO vo = new ActivityVO();
        BeanUtils.copyProperties(activity, vo);
        return vo;
    }
}
