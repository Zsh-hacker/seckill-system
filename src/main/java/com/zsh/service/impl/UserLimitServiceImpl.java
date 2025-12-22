package com.zsh.service.impl;

import com.zsh.cache.CacheManager;
import com.zsh.constant.CacheKey;
import com.zsh.dao.SeckillActivityDao;
import com.zsh.dao.UserSeckiillRecordDao;
import com.zsh.entity.SeckillActivity;
import com.zsh.entity.UserSeckillRecord;
import com.zsh.service.UserLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLimitServiceImpl implements UserLimitService {

    private final CacheManager cacheManager;
    private final UserSeckiillRecordDao userSeckillRecordDao;
    private final SeckillActivityDao activityDao;

    // 用户限购缓存时间（活动结束后清理）
    private static final long USER_LIMIT_TTL = TimeUnit.HOURS.toSeconds(24);

    @Override
    public boolean canUserPurchase(Long userId, Long activityId, Integer quantity) {
        // 1. 检查用户是否已参与
        if (hasUserParticipated(userId, activityId)) {
            log.warn("User already participated: userId={}, activityId={}", userId, activityId);
            return false;
        }

        // 2. 获取活动限购规则
        SeckillActivity activity = activityDao.selectById(activityId);
        if (activity == null) {
            log.error("Activity not found: {}", activityId);
            return false;
        }

        // 3. 检查用户购买数量
        Integer purchasedQuantity = getUserPurchasedQuantity(userId, activityId);
        if (purchasedQuantity != null && purchasedQuantity + quantity > activity.getLimitPerUser()) {
            log.warn("User limit exceeded: userId={}, activityId={}, purchased={}, limit={}",
                    userId, activityId, purchasedQuantity, activity.getLimitPerUser());
            return false;
        }

        return true;
    }

    @Override
    public void recordUserPurchase(Long userId, Long activityId, Long orderId) {
        // 1. 数据库记录
        UserSeckillRecord record = new UserSeckillRecord();
        record.setUserId(userId);
        record.setActivityId(activityId);
        record.setOrderId(orderId);
        userSeckillRecordDao.insert(record);

        // 2. 缓存记录
        String cacheKey = buildUserLimitKey(userId, activityId);
        Integer purchased = getUserPurchasedQuantity(userId, activityId);
        if (purchased == null) {
            purchased = 0;
        }

        // 这里使用AtomicInteger，但Redis中我们直接存储数字
        cacheManager.set(cacheKey, purchased + 1, USER_LIMIT_TTL, TimeUnit.SECONDS);

        log.debug("Recorded user purchase: userId={}, activityId={}, orderId={}",
                userId, activityId, orderId);
    }

    @Override
    public Integer getUserPurchasedQuantity(Long userId, Long activityId) {
        // 1. 先查缓存
        String cacheKey = buildUserLimitKey(userId, activityId);
        Integer cacheQuantity = cacheManager.get(cacheKey, Integer.class);
        if (cacheQuantity != null) {
            return cacheQuantity;
        }

        // 2. 查数据库
        Integer dbQuantity = userSeckillRecordDao.checkUserParticipated(userId, activityId);

        // 3. 回填缓存
        if (dbQuantity != null) {
            cacheManager.set(cacheKey, dbQuantity, USER_LIMIT_TTL, TimeUnit.SECONDS);
        }

        return dbQuantity;
    }

    @Override
    public void clearUserLimit(Long userId, Long activityId) {
        String cacheKey = buildUserLimitKey(userId, activityId);
        cacheManager.delete(cacheKey);

        // 也可以删除数据库记录，但这里只清缓存
        log.debug("Cleared user limit cache: userId={}, activityId={}", userId, activityId);
    }

    @Override
    public boolean hasUserParticipated(Long userId, Long activityId) {
        Integer purchased = getUserPurchasedQuantity(userId, activityId);
        return purchased != null && purchased > 0;
    }

    private String buildUserLimitKey(Long userId, Long activityId) {
        return CacheKey.format(CacheKey.USER_REQUEST_KEY, userId, activityId);
    }
}
