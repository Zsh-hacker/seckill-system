package com.zsh.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface CacheService {

    // 基础操作
    <T> void set(String key, T value);
    <T> void set(String key, T value, long ttl, TimeUnit unit);
    <T> T get(String key, Class<T> type);
    Boolean delete(String key);
    Boolean hasKey(String key);
    Boolean expire(String key, long ttl, TimeUnit unit);

    // 原子操作
    Long increment(String key);
    Long increment(String key, long delta);
    Long decrement(String key);
    Long decrement(String key, long delta);

    // 批量操作
    <T> void multiSet(Map<String, T> map);
    <T> Map<String, T> multiGet(Set<String> keys, Class<T> type);

    // 防止缓存穿透
    <T> T getWithCachePenetrationProtection(String key, Class<T> type, Supplier<T> loader, long ttl, TimeUnit unit);

    // 防缓存击穿
    <T> T getWithCacheBreakdownProtection(String key, Class<T> type, Supplier<T> loader, long ttl, TimeUnit unit);

    // 统计信息
    CacheStats getStats();
    void clearStats();
}
