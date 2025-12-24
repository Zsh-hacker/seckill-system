package com.zsh.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "seckill.thread-pool")
@Data
public class SeckillThreadPoolConfig {

    // 核心线程数
    private int corePoolSize = 20;

    // 最大线程数
    private int maxPoolSize = 100;

    // 队列容量
    private int queueCapacity = 1000;

    // 线程空闲时间（秒）
    private int keepAliveSeconds = 60;

    // 线程名前缀
    private String threadNamePrefix = "seckill-executor-";

    // 是否等待任务完成后再关闭
    private boolean waitForTasksToCompleteOnShutdown = true;

    // 关闭时等待任务完成的最大时间（秒）
    private int awaitTerminationSeconds = 60;

    @Bean("seckillThreadPool")
    public Executor seckillThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 配置核心参数
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix(threadNamePrefix);

        // 配置拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 配置关闭行为
        executor.setWaitForTasksToCompleteOnShutdown(waitForTasksToCompleteOnShutdown);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);

        // 添加线程池监控
        executor.setTaskDecorator(runnable -> {
            long startTime = System.currentTimeMillis();
            return () -> {
                try {
                    runnable.run();
                } finally {
                    long executionTime = System.currentTimeMillis() - startTime;
                    log.debug("Task executed in {} ms", executionTime);
                }
            };
        });

        executor.initialize();
        log.info("Seckill thread pool initialized: core={}, max={}, queue={}",
                corePoolSize, maxPoolSize, queueCapacity);

        return executor;
    }

    /**
     * 异步处理线程池（用于库存同步等后台任务
     */
    @Bean("asyncThreadPool")
    public Executor asyncThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-executor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }
}
