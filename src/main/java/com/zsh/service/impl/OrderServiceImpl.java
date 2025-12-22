package com.zsh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zsh.cache.activity.ActivityCacheService;
import com.zsh.cache.product.ProductCacheService;
import com.zsh.constant.OrderStatus;
import com.zsh.dao.SeckillActivityDao;
import com.zsh.dao.SeckillOrderDao;
import com.zsh.entity.SeckillActivity;
import com.zsh.entity.SeckillOrder;
import com.zsh.service.OrderService;
import com.zsh.util.OrderNoGenerator;
import com.zsh.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final SeckillOrderDao orderDao;
    private final SeckillActivityDao activityDao;
    private final ActivityCacheService activityCacheService;
    private final ProductCacheService productCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrder createOrder(Long userId, Long activityId, Integer quantity) {
        // 1. 获取活动信息
        SeckillActivity activity = activityDao.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在：" + activityId);
        }

        // 2. 生成订单号
        String orderNo = generateOrderNo();

        // 3. 创建订单
        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setActivityId(activityId);
        order.setProductId(activity.getProductId());
        order.setQuantity(quantity);
        order.setSeckillPrice(activity.getSeckillPrice());

        // 计算总金额
        BigDecimal totalAmount = activity.getSeckillPrice().multiply(BigDecimal.valueOf(quantity));
        order.setTotalAmount(totalAmount);

        // 初始状态为待支付
        order.setStatus(OrderStatus.PENDING.getCode());

        // 4. 保存订单
        orderDao.insert(order);
        log.info("Order created: orderNo={}, userId={}, activityId={}, quantity={}",
                orderNo, userId, activityId, quantity);

        return order;
    }

    @Override
    public String generateOrderNo() {
        return OrderNoGenerator.generateSeckillOrderNo();
    }

    @Override
    public OrderVO getOrderByNo(String orderNo) {
        SeckillOrder order = orderDao.selectByOrderNo(orderNo);
        if (order == null) {
            return null;
        }
        return convertToVO(order);
    }

    @Override
    public List<OrderVO> getOrdersByUserId(Long userId) {
        List<SeckillOrder> orders = orderDao.selectByUserId(userId);
        return orders.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrderStatus(String orderNo, Integer oldStatus, Integer newStatus) {
        int rows = orderDao.updateOrderStatus(orderNo, oldStatus, newStatus);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrder(String orderNo) {
        // 1. 获取订单
        SeckillOrder order = orderDao.selectByOrderNo(orderNo);
        if (order == null) {
            log.error("Order not found: {}", orderNo);
            return false;
        }
        // 2. 检查订单状态
        if (!OrderStatus.PENDING.getCode().equals(order.getStatus())) {
            log.error("Invalid order status for payment: orderNo={}, status={}",
                    orderNo, order.getStatus());
            return false;
        }
        // 3. 更新订单状态为已支付
        boolean updated = updateOrderStatus(orderNo, OrderStatus.PENDING.getCode(), OrderStatus.PAID.getCode());
        if (updated) {
            order.setStatus(OrderStatus.PAID.getCode());
            order.setPayTime(LocalDateTime.now());
            orderDao.updateById(order);
            log.info("Order paid: {}", orderNo);
        }

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cacelOrder(String orderNo) {
        // 1. 获取订单
        SeckillOrder order = orderDao.selectByOrderNo(orderNo);
        if (order == null) {
            log.error("Order not found: {}", orderNo);
            return false;
        }

        // 2. 只有待支付订单可以取消
        if (!OrderStatus.PENDING.getCode().equals(order.getStatus())) {
            log.error("Order cannot be canceled: orderNo={}, status={}",
                    orderNo, order.getStatus());
            return false;
        }

        // 3. 更新订单状态为已取消
        boolean updated = updateOrderStatus(orderNo, OrderStatus.PENDING.getCode(), OrderStatus.CANCELED.getCode());

        if (updated) {
            // 4. 恢复库存
            SeckillActivity activity = activityDao.selectById(order.getActivityId());
            if (activity != null) {
                activity.setAvailableStock(activity.getAvailableStock() + order.getQuantity());
                activityDao.updateById(activity);

                // 更新缓存
                activityCacheService.increaseActivityStock(order.getActivityId(), order.getQuantity());
                productCacheService.increaseProductStock(activity.getProductId(), order.getQuantity());
                log.info("Order canceled: {}", orderNo);
            }
        }
        return updated;
    }

    @Override
    public boolean checkDuplicateOrder(Long userId, Long activityId) {
        QueryWrapper<SeckillOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("activity_id", activityId)
                .in("status",
                        OrderStatus.PENDING.getCode(),
                        OrderStatus.PAID.getCode());

        Long count = orderDao.selectCount(queryWrapper);
        return count != null && count > 0;
    }

    private OrderVO convertToVO(SeckillOrder order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);

        // 设置状态描述
        OrderStatus status = OrderStatus.getByCode(order.getStatus());
        if (status != null) {
            vo.setStatusDesc(status.getDesc());
        }
        // 设置商品名称（这里简化处理，实际应该查询商品信息）
        vo.setProductName("商品" + order.getProductId());

        return vo;
    }
}
