package com.zsh.lock.segment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class SegmentLockManager {

    // 分段数量，建议为2的幂次方
    private static final int SEGMENT_COUNT = 16;
    private static final int SEGMENT_MASK = SEGMENT_COUNT - 1;

    // 分段锁数组
    private final ReentrantLock[] segments;

    // 锁统计信息
    private final long[] lockWaitTime;
    private final long[] lockHoldTime;
    private final int[] lockCount;

    public SegmentLockManager() {
        this.segments = new ReentrantLock[SEGMENT_COUNT];
        this.lockWaitTime = new long[SEGMENT_COUNT];
        this.lockHoldTime = new long[SEGMENT_COUNT];
        this.lockCount = new int[SEGMENT_COUNT];

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            segments[i] = new ReentrantLock(true);
        }

        log.info("SegmentLockManager initialized with {} segments", SEGMENT_COUNT);
    }

    /**
     * 根据商品ID获取分段锁
     */
    public ReentrantLock getSegmentLock(Long productId) {
        int segmentIndex = calculateSegmentIndex(productId);
        return segments[segmentIndex];
    }

    /**
     * 获取锁并记录统计
     */
    public void lock(Long productId) {
        int segmentIndex = calculateSegmentIndex(productId);
        ReentrantLock lock = segments[segmentIndex];

        long startWait = System.nanoTime();
        lock.lock();
        long endWait = System.nanoTime();

        // 记录持有时间
        synchronized (this) {
            lockWaitTime[segmentIndex] += (endWait - startWait);
            lockCount[segmentIndex]++;
        }

        // 记录持有时间
        final int finalSegmentIndex = segmentIndex;
        final  long lockStartTime = System.nanoTime();

        // 添加清理钩子
        Thread lockThread = Thread.currentThread();
        lockThread.setContextClassLoader(new ClassLoader() {
            @Override
            protected void finalize() throws Throwable {
                long holdTime = System.nanoTime() - lockStartTime;
                synchronized (SegmentLockManager.this) {
                    lockHoldTime[finalSegmentIndex] += holdTime;
                }
                super.finalize();
            }
        });
    }

    /**
     * 释放锁
     */
    public void unlock(Long productId) {
        int segmentIndex = calculateSegmentIndex(productId);
        segments[segmentIndex].unlock();
    }

    /**
     * 尝试获取锁
     */
    public boolean tryLock(Long productId) {
        int segmentIndex = calculateSegmentIndex(productId);
        return segments[segmentIndex].tryLock();
    }

    /**
     * 计算分段索引
     */
    private int calculateSegmentIndex(Long productId) {
        if (productId == null) {
            return 0;
        }
        // 使用哈希码并获取锁，确保均匀分布
        int hashCode = productId.hashCode();
        return (hashCode ^ (hashCode >>> 16)) & SEGMENT_MASK;
    }

    /**
     * 获取锁统计信息
     */
    public SegmentLockStats getStats() {
        synchronized (this) {
            SegmentLockStats stats = new SegmentLockStats();

            long totalWaitTime = 0;
            long totalHoldTime = 0;
            int totalCount = 0;
            int maxQueueLength = 0;

            for (int i = 0; i < SEGMENT_COUNT; i++) {
                totalWaitTime += lockWaitTime[i];
                totalHoldTime += lockHoldTime[i];
                totalCount += lockCount[i];
                maxQueueLength = Math.max(maxQueueLength, segments[i].getQueueLength());

                stats.addSegmentStat(i,
                        lockCount[i],
                        lockWaitTime[i],
                        lockHoldTime[i],
                        segments[i].getQueueLength(),
                        segments[i].isLocked()
                );
            }

            stats.setTotalWaitTime(totalWaitTime);
            stats.setTotalHoldTime(totalHoldTime);
            stats.setTotalCount(totalCount);
            stats.setMaxQueueLength(maxQueueLength);

            if (totalCount > 0) {
                stats.setAverageWaitTime((double) totalWaitTime / totalCount / 1_000_000); // 转换为毫秒
                stats.setAverageHoldTime((double) totalHoldTime / totalCount / 1_000_000);
            }

            return stats;
        }
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        synchronized (this) {
            for (int i = 0; i < SEGMENT_COUNT; i++) {
                lockWaitTime[i] = 0;
                lockHoldTime[i] = 0;
                lockCount[i] = 0;
            }
        }
    }

    @Data
    public static class SegmentLockStats {
        private List<SegmentStat> segmentStats = new ArrayList<>();
        private long totalWaitTime;
        private long totalHoldTime;
        private int totalCount;
        private int maxQueueLength;
        private double averageWaitTime;
        private double averageHoldTime;

        public void addSegmentStat(int index, int count, long waitTime, long holdTime,
                                   int queueLength, boolean isLocked) {
            segmentStats.add(new SegmentStat(index, count, waitTime, holdTime,
                    queueLength, isLocked));
        }

        @Data
        @AllArgsConstructor
        public static class SegmentStat {
            private int segmentIndex;
            private int lockCount;
            private long waitTime;     // 纳秒
            private long holdTime;     // 纳秒
            private int queueLength;
            private boolean isLocked;
        }
    }
}
