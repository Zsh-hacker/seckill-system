package com.zsh.config;

import io.lettuce.core.support.caching.RedisCache;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    // 本地缓存配置
    private LocalCache local = new LocalCache();

    // Redis缓存配置
    private RedisCache redis = new RedisCache();

    // 多级缓存配置
    private MultiLevelCache multiLevel = new MultiLevelCache();

    @Data
    public static class LocalCache {
        private int initialCapacity = 100;
        private int maximumSize = 10000;
        private Duration expireAfterWrite = Duration.ofMinutes(10);
        private Duration expireAfterAccess = Duration.ofMinutes(5);
        private boolean recordStats = true;
    }

    @Data
    public static class RedisCache {
        private Duration defaultTtl = Duration.ofMinutes(30);
        private Duration nullValueTtl = Duration.ofMinutes(5); // 空值缓存时间
        private boolean cacheNullValues = true; // 是否缓存空值
    }

    @Data
    public static class MultiLevelCache {
        private boolean enabled = true;
        private int localCacheSize = 1000;
        private Duration syncDelay = Duration.ofSeconds(1); // 同步延迟
    }
}
