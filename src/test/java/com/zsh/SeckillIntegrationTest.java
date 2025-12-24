package com.zsh;

import com.zsh.dto.SeckillRequestDTO;
import com.zsh.service.SeckillService;
import com.zsh.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@SpringBootTest
class SeckillIntegrationTest {

    @Autowired
    private SeckillService seckillService;

    @Test
    void testSingleSeckill() {
        SeckillRequestDTO request = new SeckillRequestDTO();
        request.setUserId(1001L);
        request.setActivityId(1L);
        request.setQuantity(1);

        Result<?> result = seckillService.executeSeckill(request);
        log.info("Seckill result: {}", result);
    }

    @Test
    void testConcurrentSeckill() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int userId = 2000 + i;
            executorService.submit(() -> {
                try {
                    SeckillRequestDTO request = new SeckillRequestDTO();
                    request.setUserId((long) userId);
                    request.setActivityId(1L);
                    request.setQuantity(1);

                    Result<?> result = seckillService.executeSeckill(request);
                    log.info("User {} seckill result: {}", userId, result.getCode());
                } catch (Exception e) {
                    log.error("Seckill error for user {}", userId, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("Concurrent seckill test completed in {} ms", elapsedTime);
    }
}