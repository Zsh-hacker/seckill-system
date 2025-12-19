package com.zsh.cache;

public interface CacheConstants {
    // 缓存名称
    String CACHE_PRODUCT = "product";
    String CACHE_ACTIVITY = "activity";
    String CACHE_ORDER = "order";

    // 过期时间
    long TTL_ONE_MINUTE = 60;
    long TTL_FIVE_MINUTES = 300;
    long TTL_THIRTY_MINUTES = 1800;
    long TTL_ONE_HOUR = 3600;
    long TTL_ONE_DAY = 86400;

    // 空值标记
    String NULL_VALUE = "NULL_VALUE";

    // 锁超时时间
    long LOCK_TIMEOUT_SECONDS = 3;
    long LOCK_WAIT_SECONDS = 1;
}
