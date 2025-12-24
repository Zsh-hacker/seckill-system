package com.zsh.controller;

import com.zsh.cache.activity.ActivityCacheService;
import com.zsh.dao.SeckillActivityDao;
import com.zsh.dto.SeckillRequestDTO;
import com.zsh.entity.SeckillActivity;
import com.zsh.service.SeckillService;
import com.zsh.vo.OrderVO;
import com.zsh.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;
    private final ActivityCacheService activityCacheService;
    private final SeckillActivityDao activityDao;

    /**
     * 执行秒杀
     */
    @PostMapping("/execute")
    public Result<OrderVO> executeSeckill(@Validated @RequestBody SeckillRequestDTO request) {
        log.info("Seckill request received: userId={}, activityId={}, quantity={}",
                request.getUserId(), request.getActivityId(), request.getQuantity());

        return seckillService.executeSeckill(request);
    }

    /**
     * 秒杀预检查
     */
    @PostMapping("/precheck")
    public Result<Boolean> preCheck(@Validated @RequestBody SeckillRequestDTO request) {
        return seckillService.preCheck(request);
    }

    /**
     * 初始化活动库存
     */
    @PostMapping("/init-stock{activityId}")
    public Result<Boolean> initActivityStock(@PathVariable Long activityId) {
        try {
            SeckillActivity activity = activityDao.selectById(activityId);
            if (activity == null) {
                return Result.error(404, "活动不存在");
            }

            // 初始化库存缓存
            activityCacheService.cacheActivityStock(activityId, activity.getAvailableStock());

            // 添加到布隆过滤器
            activityCacheService.addToBloomFilter(activityId);

            log.info("Initialized stock cache for activity {}: {}",
                    activityId, activity.getAvailableStock());

            return Result.success(true);
        } catch (Exception e) {
            log.error("Failed to init activity stock: {}", activityId, e);
            return Result.error(500, "初始化库存失败");
        }
    }

    /**
     * 初始化所有活动库存
     */
    @PostMapping("/init-all-stocks")
    public Result<Boolean> initAllStocks() {
        try {
            List<SeckillActivity> activities = activityDao.selectList(null);
            for (SeckillActivity activity : activities) {
                activityCacheService.cacheActivityStock(activity.getId(), activity.getAvailableStock());
                activityCacheService.addToBloomFilter(activity.getId());
            }

            log.info("Initialized stock cache for {} activities", activities.size());
            return Result.success(true);
        } catch (Exception e) {
            log.error("Failed to init all stocks", e);
            return Result.error(500, "初始化所有库存失败");
        }
    }

    /**
     * 获取秒杀令牌（防刷）
     */
    @GetMapping("/token")
    public Result<String> getSeckillToken(@RequestParam Long activityId,
                                          @RequestParam Long userId) {
        return seckillService.getSeckillToken(activityId, userId);
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/order/{orderNo}")
    public Result<OrderVO> getOrderDetail(@PathVariable String orderNo) {
        // TODO: 需要实现订单查询服务
        return Result.error(500, "功能暂未实现");
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("Seckill service is running");
    }
}
