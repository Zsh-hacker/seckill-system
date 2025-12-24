package com.zsh.util;

import com.zsh.dao.SeckillActivityDao;
import com.zsh.dao.SeckillOrderDao;
import com.zsh.entity.SeckillActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@Order(2)  // 在缓存预热之后执行
@RequiredArgsConstructor
public class TestDataGenerator implements CommandLineRunner {

    private final SeckillActivityDao activityDAO;
    private final SeckillOrderDao orderDAO;

    @Override
    public void run(String... args) throws Exception {
        log.info("Generating test data...");

        // 确保有一个进行中的活动用于测试
        createTestActivity();

        log.info("Test data generation completed");
    }

    private void createTestActivity() {
        // 检查是否已有进行中的活动
        long count = activityDAO.selectCount(null);
        if (count > 0) {
            log.info("Activities already exist, skipping test data creation");
            return;
        }

        // 创建一个测试用的秒杀活动
        SeckillActivity activity = new SeckillActivity();
        activity.setName("测试秒杀活动");
        activity.setProductId(1L);
        activity.setSeckillPrice(new BigDecimal("99.00"));
        activity.setOriginalPrice(new BigDecimal("199.00"));
        activity.setTotalStock(1000);
        activity.setAvailableStock(1000);
        activity.setStartTime(LocalDateTime.now().minusHours(1));
        activity.setEndTime(LocalDateTime.now().plusHours(2));
        activity.setStatus(1);  // 进行中
        activity.setLimitPerUser(2);

        activityDAO.insert(activity);
        log.info("Created test activity: {}", activity.getId());
    }
}