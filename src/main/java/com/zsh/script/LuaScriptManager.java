package com.zsh.script;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LuaScriptManager {
    private final RedisTemplate<String, Object> redisTemplate;

    private DefaultRedisScript<Long> deductStockScript;
    private DefaultRedisScript<Long> checkAndDeductScrtipt;

    @PostConstruct
    public void init() {
        // 加载扣减库存脚本
        deductStockScript = new DefaultRedisScript<>();
        deductStockScript.setLocation(new ClassPathResource("lua/deduct_stock.lua"));
        deductStockScript.setResultType(Long.class);

        // 加载检查并扣减脚本
        checkAndDeductScrtipt = new DefaultRedisScript<>();
        checkAndDeductScrtipt.setLocation(new ClassPathResource("lua/check_and_deduct.lua"));
        checkAndDeductScrtipt.setResultType(Long.class);

        log.info("Lua scripts loaded successfully");
    }

    /**
     * 扣减库存
     * @return -1：键不存在，-2：库存不足，其他：新库存值
     */
    public Long deductStock(String stockKey, Integer quantity) {
        try {
            List<String> keys = Arrays.asList(stockKey);
            return redisTemplate.execute(deductStockScript, keys, quantity.toString());
        } catch (Exception e) {
            log.error("Failed to execute deduct stock script", e);
            return null;
        }
    }

    /**
     * 检查并扣减库存（原子操作）
     * @return -1：超过限购，-2：库存键不存在，-3：库存不足，其他：新增库存
     */
    public Long checkAndDeduct(String stockKey, String limitKey,
                               Long userId, Long activityId,
                               Integer quantity, Integer limitPerUser) {
        try {
            List<String> keys = Arrays.asList(stockKey, limitKey);
            Object[] args = {userId.toString(), activityId.toString(),
                            quantity.toString(), limitPerUser.toString()};
            return redisTemplate.execute(checkAndDeductScrtipt, keys, args);
        } catch (Exception e) {
            log.error("Failed to execute check and deduct script", e);
            return null;
        }
    }
}
