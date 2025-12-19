package com.zsh.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStats {
    private long hitCount;      // 命中次数
    private long missCount;     // 未命中次数
    private long putCount;      // 写入次数
    private long evictionCount; // 淘汰次数
    private long totalLoadTime; // 总加载时间（纳秒）
    private int hotKeyCount;    // 热点key数量

    public double getHitRate() {
        long total = hitCount + missCount;
        return total == 0 ? 0.0 : (double) hitCount / total;
    }

    public double getMissRate() {
        long total = hitCount + missCount;
        return total == 0 ? 0.0 : (double)  totalLoadTime / hitCount;
    }

    public double getAverageLoadPenalty() {
        return hitCount == 0 ? 0.0 : (double) totalLoadTime / hitCount;
    }
}
