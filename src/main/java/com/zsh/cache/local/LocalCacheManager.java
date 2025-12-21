package com.zsh.cache.local;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zsh.cache.CacheStats;
import com.zsh.cache.CacheService;
import com.zsh.config.CacheProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Slf4j
@Component
public class LocalCacheManager implements CacheService {

    private Cache<String, Object> cache;
    private final CacheProperties properties;

    // 统计信息
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong putCount = new AtomicLong();

    // 热点key标记
    private final Map<String, AtomicLong> hotKeyAccessCount = new ConcurrentHashMap<>();
    private static final int HOT_KEY_THRESHOLD = 1000;

    public LocalCacheManager(CacheProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        this.cache = Caffeine.newBuilder()
                .initialCapacity(properties.getLocal().getInitialCapacity())
                .maximumSize(properties.getLocal().getMaximumSize())
                .expireAfterWrite(properties.getLocal().getExpireAfterWrite().toMillis(), TimeUnit.MILLISECONDS)
                .expireAfterAccess(properties.getLocal().getExpireAfterAccess().toMillis(), TimeUnit.MILLISECONDS)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    log.debug("Local cache removed: key={}, cause={}", key, cause);
                })
                .build();
    }

    @Override
    public <T> void set(String key, T value) {
        cache.put(key, value);
        putCount.incrementAndGet();
        markHotKey(key);
    }

    @Override
    public <T> void set(String key, T value, long ttl, TimeUnit unit) {
        // Caffeine不支持单独的TTL，使用统一的过期时间
        set(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = cache.getIfPresent(key);
        if (value != null) {
            hitCount.incrementAndGet();
            markHotKey(key);
            return (T) value;
        }
        missCount.incrementAndGet();
        return null;
    }

    @Override
    public Boolean delete(String key) {
        cache.invalidate(key);
        hotKeyAccessCount.remove(key);
        return true;
    }

    @Override
    public Boolean hasKey(String key) {
        return cache.getIfPresent(key) != null;
    }

    @Override
    public Boolean expire(String key, long ttl, TimeUnit unit) {
        // Caffeine不支持单独的TTL
        return true;
    }

    @Override
    public Long increment(String key) {
        return increment(key, 1);
    }

    @Override
    public Long increment(String key, long delta) {
        AtomicLong counter = (AtomicLong) cache.get(key, k -> new AtomicLong(0));
        return counter.addAndGet(delta);
    }

    @Override
    public Long decrement(String key) {
        return decrement(key, 1);
    }

    @Override
    public Long decrement(String key, long delta) {
        return increment(key, -delta);
    }

    @Override
    public <T> void multiSet(Map<String, T> map) {
        cache.putAll(map);
        putCount.addAndGet(map.size());
        map.keySet().forEach(this::markHotKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> multiGet(Set<String> keys, Class<T> type) {
        Map<String, T> result = new HashMap<>();
        Map<String, Object> all = cache.getAllPresent(keys);
        for (Map.Entry<String, Object> entry : all.entrySet()) {
            result.put(entry.getKey(), (T) entry.getValue());
            markHotKey(entry.getKey());
        }
        return result;
    }

    @Override
    public <T> T getWithCachePenetrationProtection(String key, Class<T> type, Supplier<T> loader, long ttl, TimeUnit unit) {
        T value = get(key, type);
        // 检查是否是空值标记
        if (value != null) {
            if (isNullValue(value)) {
                return null;
            }
            return value;
        }
        // 从loader加载
        value = loader.get();
        if (value != null) {
            set(key, value, ttl, unit);
        } else if (properties.getRedis().isCacheNullValues()) {
            // 防止空值缓存穿透
            set(key, getNullValueMarker(), properties.getRedis().getNullValueTtl().toMillis(), TimeUnit.MILLISECONDS);
        }

        return value;
    }

    @Override
    public <T> T getWithCacheBreakdownProtection(String key, Class<T> type, Supplier<T> loader, long ttl, TimeUnit unit, long lockTimeout, TimeUnit lockUnit) {
        // 本地缓存简单实现，分布式环境下需要分布式锁
        return getWithCachePenetrationProtection(key, type, loader, ttl, unit);
    }

    @Override
    public CacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = cache.stats();

        return CacheStats.builder()
                .hitCount(stats.hitCount())
                .missCount(stats.missCount())
                .putCount(putCount.get())
                .evictionCount(stats.evictionCount())
                .totalLoadTime(stats.totalLoadTime())
                .hotKeyCount(hotKeyAccessCount.size())
                .build();
    }

    @Override
    public void clearStats() {
        hitCount.set(0);
        missCount.set(0);
        putCount.set(0);
        hotKeyAccessCount.clear();
    }

    private void markHotKey(String key) {
        AtomicLong counter = hotKeyAccessCount.computeIfAbsent(key, k -> new AtomicLong());
        long count = counter.incrementAndGet();
        if (count >= HOT_KEY_THRESHOLD) {
            log.info("Detected hot key: {}, access count: {}", key, count);
        }
    }

    private boolean isNullValue(Object value) {
        return value instanceof NullValueMarker;
    }

    private Object getNullValueMarker() {
        return NullValueMarker.INSTANCE;
    }

    private static class NullValueMarker {
        static final NullValueMarker INSTANCE = new NullValueMarker();

        @Override
        public String toString() {
            return "NULL_VALUE";
        }
    }
}
