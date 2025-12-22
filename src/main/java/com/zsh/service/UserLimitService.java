package com.zsh.service;

public interface UserLimitService {

    /**
     * 检查用户是否可以购买
     */
    boolean canUserPurchase(Long userId, Long activityId, Integer quantity);

    /**
     * 记录用户购买行为
     */
    void recordUserPurchase(Long userId, Long activityId, Long orderId);

    /**
     * 获取用户已购买数量
     */
    Integer getUserPurchasedQuantity(Long userId, Long activityId);

    /**
     * 清除用户限购记录（用于测试或重置）
     */
    void clearUserLimit(Long userId, Long activityId);

    /**
     * 检查用户是否重复购买
     */
    boolean hasUserParticipated(Long userId, Long activityId);
}
