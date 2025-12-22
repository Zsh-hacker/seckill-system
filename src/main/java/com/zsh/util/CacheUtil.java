package com.zsh.util;

import com.zsh.cache.CacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheUtil {

    private final CacheManager cacheManager;

    /**
     * 生成缓存key
     */
    public static String buildKey(String prefix, Object... params) {
        StringBuilder sb = new StringBuilder(prefix);
        for (Object param : params) {
            sb.append(":").append(params);
        }
        return sb.toString();
    }

    /**
     * 批量删除缓存（按模式）
     */
    public long deleteByPattern(String pattern) {
        return cacheManager.clearRedisCache(pattern);
    }

    /**
     * 获取缓存剩余时间
     */

    /**
     * 刷新缓存过期时间
     */
    public boolean refreshExpire(String key, long ttl, TimeUnit unit) {
        return cacheManager.expire(key, ttl, unit);
    }

    /**
     * 获取缓存统计信息
     */
    public void printCacheStats() {
        log.info("=== Cache Statistics ===");
        log.info("Local Cache Stats: {}", cacheManager.getLocalCacheStats());
        log.info("Redis Cache Stats: {}", cacheManager.getRedisCacheStats());
        log.info("Combined Stats: {}", cacheManager.getStats());
        log.info("Local Cache Hit Rate: {:.2f}%",
                cacheManager.getLocalCacheStats().getHitRate() * 100);
        log.info("Redis Cache Hit Rate: {:.2f}%",
                cacheManager.getRedisCacheStats().getHitRate() * 100);
        log.info("=========================");
    }

}
