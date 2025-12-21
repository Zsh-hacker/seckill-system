package com.zsh.cache.redis;

import com.zsh.cache.CacheService;
import com.zsh.cache.CacheStats;
import com.zsh.config.CacheProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Slf4j
@Component
public class RedisCacheManager implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheProperties properties;

    // 统计信息
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong putCount = new AtomicLong();

    // Lua脚本
    private final DefaultRedisScript<Boolean> setNxScript;
    private final DefaultRedisScript<Long> incrementScript;

    // 空值标记
    private static final String  NULL_VALUE = "NULL_VALUE";

    public RedisCacheManager(RedisTemplate<String, Object> redisTemplate, CacheProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;

        // 初始化Lua脚本
        this.setNxScript = new DefaultRedisScript<>();
        this.setNxScript.setScriptText(
                "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then " +
                "redis.call('expire', KEYS[1], ARGV[2]) " +
                "return true " +
                "else " +
                "return false " +
                "end"
        );
        this.setNxScript.setResultType(Boolean.class);
        this.incrementScript = new DefaultRedisScript<>();
        this.incrementScript.setScriptText(
                "local current = redis.call('get', KEYS[1]) " +
                "if not current then " +
                "redis.call('set', KEYS[1], ARGV[1]) " +
                "return tonumber(ARGV[1]) " +
                "else " +
                "local new = tonumber(current) + tonumber(ARGV[1]) " +
                "redis.call('set', KEYS[1], new) " +
                "return new " +
                "end"
        );
        this.incrementScript.setResultType(Long.class);
    }

    @PostConstruct
    public void init() {
        log.info("RedisCacheManager initialized with default TTL: {}", properties.getRedis().getDefaultTtl());
    }

    @Override
    public <T> void set(String key, T value) {
        set(key, value, properties.getRedis().getDefaultTtl().toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> void set(String key, T value, long ttl, TimeUnit unit) {
        try {
            if (value == null && properties.getRedis().isCacheNullValues()) {
                redisTemplate.opsForValue().set(key, NULL_VALUE, ttl, unit);
            } else {
                redisTemplate.opsForValue().set(key, value, ttl, unit);
            }
            putCount.incrementAndGet();
        } catch (Exception e) {
            log.error("Redis set operation failed, key: {}", key, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                hitCount.incrementAndGet();
                // 检查是否是空值标记
                if (NULL_VALUE.equals(value)) {
                    return null;
                }
                return (T) value;
            }
            missCount.incrementAndGet();
            return null;
        } catch (Exception e) {
            log.error("Redis get operation failed, key: {}", key, e);
            missCount.incrementAndGet();
            return null;
        }
    }

    @Override
    public Boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            return result != null && result;
        } catch (Exception e) {
            log.error("Redis delete operation failed, key: {}", key, e);
            return false;
        }
    }

    @Override
    public Boolean hasKey(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return result != null & result;
        } catch (Exception e) {
            log.error("Redis hasKey operation failed, key: {}", key, e);
            return false;
        }
    }

    @Override
    public Boolean expire(String key, long ttl, TimeUnit unit) {
        try {
            Boolean result = redisTemplate.expire(key, ttl, unit);
            return result != null && result;
        } catch (Exception e) {
            log.error("Redis expire operation failed, key: {}", key, e);
            return false;
        }
    }

    @Override
    public Long increment(String key) {
        return increment(key, 1);
    }

    @Override
    public Long increment(String key, long delta) {
        try {
            List<String> keys = Collections.singletonList(key);
            Long result = redisTemplate.execute(incrementScript, keys, String.valueOf(delta));
            return result != null ? result : delta;
        } catch (Exception e) {
            log.error("Redis increment operation failed, key: {}, delta: {}", key, delta, e);
            // 降级处理：使用普通方式
            return redisTemplate.opsForValue().increment(key, delta);
        }
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
        try {
            Map<String, Object> redisMap = new HashMap<>();
            for (Map.Entry<String, T> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (value != null && properties.getRedis().isCacheNullValues()) {
                    redisMap.put(entry.getKey(), NULL_VALUE);
                } else {
                    redisMap.put(entry.getKey(), value);
                }
            }
            redisTemplate.opsForValue().multiSet(redisMap);
            // 设置统一的过期时间
            long ttl = properties.getRedis().getDefaultTtl().toMillis();
            for (String key : map.keySet()) {
                redisTemplate.expire(key, ttl, TimeUnit.MILLISECONDS);
            }

            putCount.addAndGet(map.size());
        } catch (Exception e) {
            log.error("Redis multiSet operation failed", e);
            throw new RuntimeException("Redis multiSet operation failed", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> multiGet(Set<String> keys, Class<T> type) {
        try {
            List<String> keyList = new ArrayList<>(keys);
            List<Object> values = redisTemplate.opsForValue().multiGet(keyList);

            Map<String, T> result = new HashMap<>();
            for (int i = 0; i < keyList.size(); i++) {
                String key = keyList.get(i);
                Object value = values != null && i < values.size() ? values.get(i) : null;

                if (value != null) {
                    hitCount.incrementAndGet();
                    if (NULL_VALUE.equals(value)) {
                        result.put(key, null);
                    } else {
                        result.put(key, (T) value);
                    }
                } else {
                    missCount.incrementAndGet();
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Redis multiGet operation failed", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public <T> T getWithCachePenetrationProtection(String key, Class<T> type, Supplier<T> loader, long ttl, TimeUnit unit) {
        // 1. 先尝试从缓存获取
        T value = get(key, type);
        if (value != null) {
            return value;
        }

        // 2. 检查是否是缓存的空值
        Object rawValue = redisTemplate.opsForValue().get(key);
        if (NULL_VALUE.equals(rawValue)) {
            return null;
        }

        // 3. 从loader加载数据
        value = loader.get();

        // 4. 写入缓存
        if (value != null) {
            set(key, value, ttl, unit);
        } else if (properties.getRedis().isCacheNullValues()) {
            // 缓存空值防止穿透
            set(key, NULL_VALUE, Math.min(ttl, properties.getRedis().getNullValueTtl().toMillis()), unit);
        }
        return value;
    }

    @Override
    public <T> T getWithCacheBreakdownProtection(String key, Class<T> type, Supplier<T> loader, long ttl, TimeUnit unit, long lockTimeout, TimeUnit lockUnit) {
        // 1. 先尝试从缓存获取
        T value = get(key, type);
        if (value != null) {
            return value;
        }

        // 2. 使用分布式锁防止缓存击穿
        String lockKey = key + ":mutex";
        boolean locked = false;
        try {
            // 尝试获取锁
            locked = tryLock(lockKey, lockTimeout, lockUnit);

            if (locked) {
                // 获取到锁，再次检查缓存（双重检查）
                value = get(key, type);
                if (value != null) {
                    return value;
                }

                // 加载数据
                value = loader.get();
                if (value != null) {
                    set(key, value, ttl, unit);
                } else if (properties.getRedis().isCacheNullValues()) {
                    set(key, NULL_VALUE, Math.min(ttl, properties.getRedis().getNullValueTtl().toMillis()), unit);
                }
                return value;
            } else {
                // 未获取到锁，等待并重试
                Thread.sleep(50);
                return get(key, type);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for cache lock", e);
            return null;
        } finally {
            if (locked) {
                releaseLock(lockKey);
            }
        }
    }

    @Override
    public CacheStats getStats() {
        return CacheStats.builder()
                .hitCount(hitCount.get())
                .missCount(missCount.get())
                .putCount(putCount.get())
                .evictionCount(0)   // Redis不统计淘汰次数
                .totalLoadTime(0)   // Redis不统计加载时间
                .hotKeyCount(0)     // 需要额外实现热点key检测
                .build();
    }

    @Override
    public void clearStats() {
        hitCount.set(0);
        missCount.set(0);
        putCount.set(0);
    }

    // 分布式锁方法
    private boolean tryLock(String lockKey, long timeout, TimeUnit unit) {
        try {
            List<String> keys = Collections.singletonList(lockKey);
            String value = "LOCKED:" + System.currentTimeMillis();
            long ttlSeconds = unit.toSeconds(timeout);

            Boolean result = redisTemplate.execute(setNxScript, keys, value, String.valueOf(ttlSeconds));
            return result != null & result;
        } catch (Exception e) {
            log.error("Failed to acquire lock: {}", lockKey, e);
            return false;
        }
    }

    private void releaseLock(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.error("ailed to release lock: {}", lockKey, e);
        }
    }

    // 批量删除
    public Long deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                return redisTemplate.delete(keys);
            }
            return 0L;
        } catch (Exception e) {
            log.error("Failed to delete keys by pattern: {}", pattern, e);
            return 0L;
        }
    }

    // 获取剩余过期时间
    public Long getExpire(String key, TimeUnit unit) {
        try {
            return redisTemplate.getExpire(key, unit);
        } catch (Exception e) {
            log.error("Failed to get expire time for key: {}", key, e);
            return null;
        }
    }

}
