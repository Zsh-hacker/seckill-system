package com.zsh.endpoint;

import com.zsh.cache.CacheManager;
import com.zsh.cache.CacheStats;
import com.zsh.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Endpoint(id = "cache")
@RequiredArgsConstructor
public class CacheMonitorEndpoint {

    private final CacheManager cacheManager;
    private final CacheUtil cacheUtil;

    @ReadOperation
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        CacheStats localStats = cacheManager.getLocalCacheStats();
        CacheStats redisStats = cacheManager.getRedisCacheStats();
        CacheStats combinedStats = cacheManager.getStats();

        stats.put("localCache", buildStatsMap(localStats));
        stats.put("redisCache", buildStatsMap(redisStats));
        stats.put("combined", buildStatsMap(combinedStats));
        stats.put("timestamp", System.currentTimeMillis());

        return stats;
    }

    @WriteOperation
    public Map<String, Object> clearCache(String type) {
        Map<String, Object> result = new HashMap<>();

        switch (type.toLowerCase()) {
            case "local":
                cacheManager.clearLocalCache();
                result.put("message", "Local cache cleared");
                break;
            case "redis":
                Long count = cacheManager.clearRedisCache("*");
                result.put("message", "Redis cache cleared");
                result.put("keysCleared", count);
                break;
            case "all":
                cacheManager.clearLocalCache();
                Long redisCount = cacheManager.clearRedisCache("*");
                result.put("message", "All cache cleared");
                result.put("redisKeysCleared", redisCount);
                break;
            default:
                result.put("error", "Invalid cache type: " + type);
        }

        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    private Map<String, Object> buildStatsMap(CacheStats stats) {
        Map<String, Object> map = new HashMap<>();
        map.put("hitCount", stats.getHitCount());
        map.put("missCount", stats.getMissCount());
        map.put("putCount", stats.getPutCount());
        map.put("evictionCount", stats.getEvictionCount());
        map.put("hitRate", String.format("%.2f%%", stats.getHitRate() * 100));
        map.put("missRate", String.format("%.2f%%", stats.getMissRate() * 100));
        map.put("averageLoadPenalty", stats.getAverageLoadPenalty());
        map.put("hotKeyCount", stats.getHotKeyCount());
        return map;
    }
}
