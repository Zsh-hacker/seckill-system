package com.zsh.service;

import com.zsh.dto.SeckillRequestDTO;
import com.zsh.vo.OrderVO;
import com.zsh.vo.Result;

public interface SeckillService {

    /**
     * 执行秒杀（核心方法）
     */
    Result<OrderVO> executeSeckill(SeckillRequestDTO request);

    /**
     * 秒杀预检查
     */
    Result<Boolean> preCheck(SeckillRequestDTO request);

    /**
     * 秒杀排队
     */
    Result<String> queueSeckill(SeckillRequestDTO request);

    /**
     * 查询秒杀结果
     */
    Result<OrderVO> querySeckillResult(String requestId);

    /**
     * 初始化活动库存到缓存
     */
    boolean initActivityStock(Long activityId);

    /**
     * 获取秒杀令牌
     */
    Result<String> getSeckillToken(Long activityId, Long userId);

    /**
     * 验证秒杀令牌
     */
    boolean verifySeckillToken(Long activityId, Long userId, String token);

}
