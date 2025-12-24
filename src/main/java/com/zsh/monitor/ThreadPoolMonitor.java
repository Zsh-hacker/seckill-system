package com.zsh.monitor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThreadPoolMonitor {

    private final ThreadPoolExecutor seckillThreadPool;

    /**
     * 每分钟监控一次线程状态
     */
    @Scheduled(fixedDelay = 60000)
    public void monitorThreadPool() {
        if (seckillThreadPool == null) {
            return;
        }

        int corePoolSize = seckillThreadPool.getCorePoolSize();
        int maxPoolSize = seckillThreadPool.getMaximumPoolSize();
        int activeCount = seckillThreadPool.getActiveCount();
        long completedTaskCount = seckillThreadPool.getCompletedTaskCount();
        long taskCount = seckillThreadPool.getTaskCount();
        int poolSize = seckillThreadPool.getPoolSize();
        int queueSize = seckillThreadPool.getQueue().size();
        int remainingCapacity = seckillThreadPool.getQueue().remainingCapacity();

        double usageRate = poolSize > 0 ? (double) activeCount / poolSize : 0;
        double completionRate = taskCount > 0 ? (double) completedTaskCount / taskCount : 0;

        // 记录监控日志
        log.info("ThreadPool Monitor - " +
                        "Core: {}, Max: {}, Active: {}, Pool: {}, " +
                        "Queue: {}/{}, Usage: {:.1%}, Completion: {:.1%}",
                corePoolSize, maxPoolSize, activeCount, poolSize,
                queueSize, queueSize + remainingCapacity,
                usageRate, completionRate);

        // 触发告警条件
        if (usageRate > 0.8) {
            log.warn("Thread pool usage rate is high: {:.1%}", usageRate);
        }

        if ((double) queueSize / (queueSize + remainingCapacity) > 0.9) {
            log.warn("Thread pool queue is almost full: {}/{}",
                    queueSize, queueSize + remainingCapacity);
        }
    }

    /**
     * 获取线程池状态
     */
    public ThreadPoolStatus getThreadPoolStatus() {
        return new ThreadPoolStatus(
                seckillThreadPool.getCorePoolSize(),
                seckillThreadPool.getMaximumPoolSize(),
                seckillThreadPool.getActiveCount(),
                seckillThreadPool.getPoolSize(),
                seckillThreadPool.getQueue().size(),
                seckillThreadPool.getQueue().remainingCapacity(),
                seckillThreadPool.getCompletedTaskCount(),
                seckillThreadPool.getTaskCount(),
                seckillThreadPool.getLargestPoolSize(),
                seckillThreadPool.getKeepAliveTime(TimeUnit.SECONDS)
        );
    }

    @Data
    @AllArgsConstructor
    public static class ThreadPoolStatus {
        private int corePoolSize;
        private int maxPoolSize;
        private int activeCount;
        private int poolSize;
        private int queueSize;
        private int queueRemainingCapacity;
        private long completedTaskCount;
        private long taskCount;
        private int largestPoolSize;
        private long keepAliveSeconds;

        public double getUsageRate() {
            return poolSize > 0 ? (double) activeCount / poolSize : 0;
        }

        public double getCompletionRate() {
            return taskCount > 0 ? (double) completedTaskCount / taskCount : 0;
        }

        public double getQueueUsageRate() {
            int totalCapacity = queueSize + queueRemainingCapacity;
            return totalCapacity > 0 ? (double) queueSize / totalCapacity : 0;
        }
    }
}
