package com.zsh.service;

import com.zsh.entity.SeckillOrder;
import com.zsh.vo.OrderVO;

import java.util.List;

public interface OrderService {

    /**
     * 创建订单
     */
    SeckillOrder createOrder(Long userId, Long activityId, Integer quantity);

    /**
     * 生成订单号
     */
    String generateOrderNo();

    /**
     * 根据订单号查询订单
     */
    OrderVO getOrderByNo(String orderNo);

    /**
     * 根据用户ID查询订单列表
     */
    List<OrderVO> getOrdersByUserId(Long userId);

    /**
     * 更新订单状态
     */
    boolean updateOrderStatus(String orderNo, Integer oldStatus, Integer newStatus);

    /**
     * 支付订单
     */
    boolean payOrder(String orderNo);

    /**
     * 取消订单
     */
    boolean cacelOrder(String orderNo);

    /**
     * 检查订单是否重复
     */
    boolean checkDuplicateOrder(Long userId, Long activityId);
}
