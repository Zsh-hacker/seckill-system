package com.zsh.constant;

public class CacheKey {
    // 商品缓存
    public static final String PRODUCT_KEY = "product:%s";
    public static final String PRODUCT_STOCK_KEY = "product:stock:%s";

    // 活动缓存
    public static final String ACTIVITY_KEY = "activity:%s";
    public static final String ACTIVITY_STOCK_KEY = "activity:stock:%s";
    public static final String ACTIVITY_LIST_KEY = "activity:list";

    // 订单缓存
    public static final String ORDER_KEY = "order:%s";
    public static final String USER_ORDER_KEY = "user:order:%s:%s";

    // 限流相关
    public static final String RATE_LIMIT_KEY = "rate:limit:%s";
    public static final String USER_REQUEST_KEY = "user:request:%s:%s";

    // 分布式锁
    public static final String LOCK_ACTIVITY_KEY = "lock:activity:%s";
    public static final String LOCK_ORDER_KEY = "lock:order:%s";

    // 布隆过滤器
    public static final String BLOOM_FILTER_ACTIVITY = "bloom:activity";

    // 热key标记
    public static final String HOT_KEY_PREFIX = "hot:";

    public static String format(String pattern, Object... args) {
        return String.format(pattern, args);
    }
}
