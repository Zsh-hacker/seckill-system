package com.zsh.service.impl;

import com.zsh.cache.activity.ActivityCacheService;
import com.zsh.constant.ErrorCode;
import com.zsh.dao.SeckillActivityDao;
import com.zsh.dao.UserSeckiillRecordDao;
import com.zsh.dto.SeckillRequestDTO;
import com.zsh.entity.SeckillActivity;
import com.zsh.entity.SeckillOrder;
import com.zsh.entity.UserSeckillRecord;
import com.zsh.exception.BusinessException;
import com.zsh.factory.StockStrategyFactory;
import com.zsh.lock.segment.SegmentLockManager;
import com.zsh.service.OrderService;
import com.zsh.service.SeckillService;
import com.zsh.service.UserLimitService;
import com.zsh.strategy.StockDeductionStrategy;
import com.zsh.vo.OrderVO;
import com.zsh.vo.Result;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final ActivityCacheService activityCacheService;
    private final OrderService orderService;
    private final UserLimitService userLimitService;
    @Resource
    private StockStrategyFactory stockStrategyFactory;
    private final SeckillActivityDao activityDao;
    private final UserSeckiillRecordDao userSeckiillRecordDao;
    private final SegmentLockManager segmentLockManager;

    // 默认使用Redis扣减策略
    private static final String DEFAULT_STOCK_STRATEGY = "redis";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderVO> executeSeckill(SeckillRequestDTO request) {
        long startTime = System.currentTimeMillis();

        // 使用分段锁优化
        segmentLockManager.lock(request.getActivityId());

        try {
            // 1. 参数校验
            validateRequest(request);

            // 2. 预检查
            Result<Boolean> preCheckResult = preCheck(request);
            if (!preCheckResult.getCode().equals(ErrorCode.SUCCESS.getCode())) {
                return Result.error(preCheckResult.getCode(), preCheckResult.getMessage());
            }

            // 确保库存已缓存到Redis
            ensureStockCached(request.getActivityId());

            // 3. 用户限购检查
            if (!userLimitService.canUserPurchase(request.getUserId(),
                    request.getActivityId(),
                    request.getQuantity())) {
                throw new BusinessException(ErrorCode.USER_LIMIT_EXCEEDED);
            }
            // 4. 扣减库存（使用策略模式）
            StockDeductionStrategy stockStrategy = stockStrategyFactory.getStrategy(DEFAULT_STOCK_STRATEGY);
            boolean stockDeducted = stockStrategy.deductStock(request.getActivityId(), request.getQuantity());

            if (!stockDeducted) {
                throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
            }

            // 5. 创建订单
            SeckillOrder order = orderService.createOrder(
                    request.getUserId(),
                    request.getActivityId(),
                    request.getQuantity()
            );

            // 6. 记录用户购买行为
            UserSeckillRecord record = new UserSeckillRecord();
            record.setUserId(request.getUserId());
            record.setActivityId(request.getActivityId());
            record.setOrderId(order.getId());
            userSeckiillRecordDao.insert(record);

            // 7. 转换为VO
            OrderVO orderVO = convertToOrderVO(order);

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("Seckill success: userId={}, activityId={}, orderNo={}, time={}ms",
                    request.getUserId(), request.getActivityId(), order.getOrderNo(), elapsedTime);

            return Result.success(orderVO);
        } catch (BusinessException e) {
            log.warn("Seckill business exception: userId={}, activityId={}, error={}",
                    request.getUserId(), request.getActivityId(), e.getMessage());
            return Result.error(e.getErrorCodeValue(), e.getMessage());
        } catch (Exception e) {
            log.error("Seckill system error: userId={}, activityId={}",
                    request.getUserId(), request.getActivityId(), e);
            return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
        } finally {
            segmentLockManager.unlock(request.getActivityId());
        }
    }

    private void ensureStockCached(Long activityId) {
        try {
            // 检查Redis中是否有库存缓存
            Integer stock = activityCacheService.getActivityStock(activityId);
            if (stock == null) {
                // 从数据库加载并缓存
                SeckillActivity activity = activityDao.selectById(activityId);
                if (activity != null) {
                    activityCacheService.cacheActivityStock(activityId, activity.getAvailableStock());
                    log.info("Initialized stock cache for activity {}: {}", activityId, activity.getAvailableStock());
                }
            }
        } catch (Exception e) {
            log.error("Failed to ensure stock cache for activity: {}", activityId, e);
        }
    }

    @Override
    public Result<Boolean> preCheck(SeckillRequestDTO request) {
        try {
            // 1. 检查活动是否存在
            SeckillActivity activity = activityDao.selectById(request.getActivityId());

            if (activity == null) {
                return Result.error(ErrorCode.ACTIVITY_NOT_FOUND.getCode(), ErrorCode.ACTIVITY_NOT_FOUND.getMessage());
            }

            // 2. 检查活动状态
            if (activity.getStatus() == 0) {    // 未开始
                return Result.error(ErrorCode.ACTIVITY_NOT_STARTED.getCode(), ErrorCode.ACTIVITY_NOT_STARTED.getMessage());
            }
            if (activity.getStatus() == 2) {    // 已结束
                return Result.error(ErrorCode.ACTIVITY_ENDED.getCode(), ErrorCode.ACTIVITY_ENDED.getMessage());
            }
            if (activity.getStatus() == 3) {    // 已关闭
                return Result.error(ErrorCode.ACTIVITY_CLOSED.getCode(), ErrorCode.ACTIVITY_CLOSED.getMessage());
            }

            // 3. 检查活动时间
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(activity.getStartTime())) {
                return Result.error(ErrorCode.ACTIVITY_NOT_STARTED.getCode(), ErrorCode.ACTIVITY_NOT_STARTED.getMessage());
            }
            if (now.isAfter(activity.getEndTime())) {
                return Result.error(ErrorCode.ACTIVITY_ENDED.getCode(), ErrorCode.ACTIVITY_ENDED.getMessage());
            }

            // 4. 检查库存
            Integer availableStock = activityCacheService.getActivityStock(request.getActivityId());
            if (availableStock == null || availableStock < request.getQuantity()) {
                return Result.error(ErrorCode.STOCK_NOT_ENOUGH.getCode(), ErrorCode.STOCK_NOT_ENOUGH.getMessage());
            }

            // 5. 检查用户是否已参与
            if (userLimitService.hasUserParticipated(request.getUserId(), request.getActivityId())) {
                return Result.error(ErrorCode.USER_ALREADY_PARTICIPATED.getCode(), ErrorCode.USER_ALREADY_PARTICIPATED.getMessage());
            }

            // 6. 检查用户限购数量
            Integer purchased = userLimitService.getUserPurchasedQuantity(request.getUserId(), request.getActivityId());
            if (purchased != null && purchased + request.getQuantity() > activity.getLimitPerUser()) {
                return Result.error(ErrorCode.USER_LIMIT_EXCEEDED.getCode(), ErrorCode.USER_LIMIT_EXCEEDED.getMessage());
            }
            return Result.success(true);
        } catch (Exception e) {
            log.error("Pre-check error", e);
            return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "预检查失败");
        }
    }

    @Override
    public Result<String> queueSeckill(SeckillRequestDTO request) {
        // TODO: 实现排队逻辑
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "排队功能暂未实现");
    }

    @Override
    public Result<OrderVO> querySeckillResult(String requestId) {
        // TODO: 实现结果查询
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "结果查询暂未实现");
    }

    @Override
    public boolean initActivityStock(Long activityId) {
        try {
            SeckillActivity activity = activityDao.selectById(activityId);
            if (activity != null) {
                activityCacheService.cacheActivityStock(activityId, activity.getAvailableStock());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to init activity stock: {}", activityId, e);
            return false;
        }
    }

    @Override
    public Result<String> getSeckillToken(Long activityId, Long userId) {
        // TODO: 实现令牌获取
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "令牌功能暂未实现");
    }

    @Override
    public boolean verifySeckillToken(Long activityId, Long userId, String token) {
        // TODO: 实现令牌验证
        return false;
    }

    private void validateRequest(SeckillRequestDTO request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数不能为空");
        }
        if (request.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "购买数量必须大于0");
        }
    }

    private OrderVO convertToOrderVO(SeckillOrder order) {
        // 这里简化实现，实际应该从数据库查询完整信息
        OrderVO vo = new OrderVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setActivityId(order.getActivityId());
        vo.setProductId(order.getProductId());
        vo.setQuantity(order.getQuantity());
        vo.setSeckillPrice(order.getSeckillPrice());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        return vo;
    }

    /**
     * 批量秒杀（使用分段锁优化）
     */
    public Result<List<OrderVO>> batchSeckill(List<SeckillRequestDTO> request) {
        if (request == null || request.isEmpty()) {
            return Result.error(400, "请求列表不能为空");
        }

        // 按活动ID分组，相同活动ID的请求一起处理
        Map<Long, List<SeckillRequestDTO>> requestsByActivity = request.stream().collect(Collectors.groupingBy(SeckillRequestDTO::getActivityId));
        List<OrderVO> results = new ArrayList<>();

        // 对每个活动使用分段锁
        for (Map.Entry<Long, List<SeckillRequestDTO>> entry : requestsByActivity.entrySet()) {
            Long activityId = entry.getKey();
            List<SeckillRequestDTO> activityRequests = entry.getValue();

            segmentLockManager.lock(activityId);
            try {
                // 处理同一活动的多个请求
                for (SeckillRequestDTO request : activityRequests) {
                    Result<OrderVO> result = executeSeckill(request);
                    if (result.isSuccess()) {
                        result.add(result.getData());
                    }
                }
            } finally {
                segmentLockManager.unlock(activityId);
            }
            return Result.success(results);
        }
    }
}
