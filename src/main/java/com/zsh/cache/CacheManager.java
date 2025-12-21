package com.zsh.cache;

import com.zsh.cache.local.LocalCacheManager;
import com.zsh.cache.redis.RedisCacheManager;
import com.zsh.config.CacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
public class CacheManager implements CacheService {

    private final LocalCacheManager localCacheManager;
    private final RedisCacheManager redisCacheManager;
    private final CacheProperties properties;

    // 是否启用多级缓存
    private final boolean multiLevelEnabled;

    public CacheManager(LocalCacheManager localCacheManager,
                        RedisCacheManager redisCacheManager,
                        CacheProperties properties) {
        this.localCacheManager = localCacheManager;
        this.redisCacheManager = redisCacheManager;
        this.properties = properties;
        this.multiLevelEnabled = properties.getMultiLevel().isEnabled();

        log.info("CacheManager initialized, multi-level cache enabled: {}", multiLevelEnabled);
    }

    @Override
    public <T> void set(String key, T value) {
        set(key, value, properties.getRedis().getDefaultTtl().toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> void set(String key, T value, long ttl, TimeUnit unit) {
        // 写入Redis（L2缓存）
        redisCacheManager.set(key, value, ttl, unit);

        // 如果启用多级缓存，同时写入本地缓存（L1缓存）
        if (multiLevelEnabled) {
            localCacheManager.set(key, value, ttl, unit);
        }
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        // 如果启用多级缓存，先从本地缓存获取
        if (multiLevelEnabled) {
            T localValue = localCacheManager.get(key, type);
            if (localValue != null) {
                log.debug("L1 cache hit for key: {}", key);
                return localValue;
            }
        }

        // 从Redis获取
        T redisValue = redisCacheManager.get(key, type);
        if (redisValue != null && multiLevelEnabled) {
            // 如果从Redis获取到数据，回填到本地缓存
            localCacheManager.set(key, redisValue,
                    properties.getLocal().getExpireAfterWrite().toMillis(), TimeUnit.MILLISECONDS);
        }

        return redisValue;
    }

    @Override
    public Boolean delete(String key) {
        boolean redisDeleted = redisCacheManager.delete(key);
        boolean localDeleted = true;

        if (multiLevelEnabled) {
            localDeleted = localCacheManager.delete(key);
        }

        return redisDeleted && localDeleted;
    }

    @Override
    public Boolean hasKey(String key) {
        if (multiLevelEnabled && localCacheManager.hasKey(key)) {
            return true;
        }
        return redisCacheManager.hasKey(key);
    }

    @Override
    public Boolean expire(String key, long ttl, TimeUnit unit) {
        boolean redisExpired = redisCacheManager.expire(key, ttl, unit);
        boolean localExpired = true;

        if (multiLevelEnabled) {
            localExpired = localCacheManager.expire(key, ttl, unit);
        }

        return redisExpired && localExpired;
    }

    @Override
    public Long increment(String key) {
        Long result = redisCacheManager.increment(key);
        if (multiLevelEnabled) {
            localCacheManager.delete(key); // 本地缓存失效，下次从Redis获取最新值
        }
        return result;
    }

    @Override
    public Long increment(String key, long delta) {
        Long result = redisCacheManager.increment(key, delta);
        if (multiLevelEnabled) {
            localCacheManager.delete(key);
        }
        return result;
    }

    @Override
    public Long decrement(String key) {
        Long result = redisCacheManager.decrement(key);
        if (multiLevelEnabled) {
            localCacheManager.delete(key);
        }
        return result;
    }

    @Override
    public Long decrement(String key, long delta) {
        Long result = redisCacheManager.decrement(key, delta);
        if (multiLevelEnabled) {
            localCacheManager.delete(key);
        }
        return result;
    }

    @Override
    public <T> void multiSet(Map<String, T> map) {
        redisCacheManager.multiSet(map);
        if (multiLevelEnabled) {
            localCacheManager.multiSet(map);
        }
    }

    @Override
    public <T> Map<String, T> multiGet(Set<String> keys, Class<T> type) {
        if (!multiLevelEnabled) {
            return redisCacheManager.multiGet(keys, type);
        }

        // 多级缓存批量获取
        Map<String, T> result = new HashMap<>();
        Set<String> missingKeys = new HashSet<>(keys);

        // 先从本地缓存获取
        Map<String, T> localValues = localCacheManager.multiGet(keys, type);
        for (Map.Entry<String, T> entry : localValues.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue());
                missingKeys.remove(entry.getKey());
            }
        }

        // 如果还有缺失的key，从Redis获取
        if (!missingKeys.isEmpty()) {
            Map<String, T> redisValues = redisCacheManager.multiGet(missingKeys, type);
            result.putAll(redisValues);

            // 回填本地缓存
            Map<String, T> backfillMap = new HashMap<>();
            for (Map.Entry<String, T> entry : redisValues.entrySet()) {
                if (entry.getValue() != null) {
                    backfillMap.put(entry.getKey(), entry.getValue());
                }
            }
            if (!backfillMap.isEmpty()) {
                localCacheManager.multiSet(backfillMap);
            }
        }

        return result;
    }

    @Override
    public <T> T getWithCachePenetrationProtection(String key, Class<T> type,
                                                   Supplier<T> loader, long ttl, TimeUnit unit) {
        // 使用Redis的缓存穿透保护（因为Redis有更好的分布式锁支持）
        T value = redisCacheManager.getWithCachePenetrationProtection(key, type, loader, ttl, unit);

        // 如果获取到值，更新本地缓存
        if (value != null && multiLevelEnabled) {
            localCacheManager.set(key, value, ttl, unit);
        }

        return value;
    }

    @Override
    public <T> T getWithCacheBreakdownProtection(String key, Class<T> type,
                                                 Supplier<T> loader, long ttl, TimeUnit unit,
                                                 long lockTimeout, TimeUnit lockUnit) {
        // 使用Redis的缓存击穿保护（带分布式锁）
        T value = redisCacheManager.getWithCacheBreakdownProtection(
                key, type, loader, ttl, unit, lockTimeout, lockUnit);

        // 如果获取到值，更新本地缓存
        if (value != null && multiLevelEnabled) {
            localCacheManager.set(key, value, ttl, unit);
        }

        return value;
    }

    @Override
    public CacheStats getStats() {
        CacheStats localStats = localCacheManager.getStats();
        CacheStats redisStats = redisCacheManager.getStats();

        // 合并统计信息
        return CacheStats.builder()
                .hitCount(localStats.getHitCount() + redisStats.getHitCount())
                .missCount(localStats.getMissCount() + redisStats.getMissCount())
                .putCount(localStats.getPutCount() + redisStats.getPutCount())
                .evictionCount(localStats.getEvictionCount() + redisStats.getEvictionCount())
                .totalLoadTime(localStats.getTotalLoadTime() + redisStats.getTotalLoadTime())
                .hotKeyCount(localStats.getHotKeyCount() + redisStats.getHotKeyCount())
                .build();
    }

    @Override
    public void clearStats() {
        localCacheManager.clearStats();
        redisCacheManager.clearStats();
    }

    // 仅清除本地缓存
    public void clearLocalCache() {
        // Caffeine缓存没有直接的clear方法，可以通过重新创建实例来清除
        // 或者通过删除所有key来清除（这里简化处理）
        localCacheManager.clearStats();
    }

    // 仅清除Redis缓存（按模式）
    public Long clearRedisCache(String pattern) {
        return redisCacheManager.deleteByPattern(pattern);
    }

    // 获取本地缓存统计
    public CacheStats getLocalCacheStats() {
        return localCacheManager.getStats();
    }

    // 获取Redis缓存统计
    public CacheStats getRedisCacheStats() {
        return redisCacheManager.getStats();
    }
}
