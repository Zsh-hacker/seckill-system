package com.zsh.dao;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zsh.entity.BaseEntity;
import com.zsh.entity.SeckillOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SeckillOrderDao extends BaseMapper<SeckillOrder> {
    /**
     * 根据订单号查询
     */
    @Select("SELECT * FROM seckill_order WHERE order_no = #{orderNo}")
    SeckillOrder selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据用户ID查询订单
     */
    @Select("SELECT * FROM seckill_order WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<SeckillOrder> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据活动ID查询订单
     */
    @Select("SELECT * FROM seckill_order WHERE activity_id = #{activityId}")
    List<SeckillOrder> selectByActivityId(@Param("activityId") Long activityId);

    /**
     * 更新订单状态
     */
    int updateOrderStatus(@Param("orderNo") String orderNo,
                          @Param("oldStatus") Integer oldStatus,
                          @Param("newStatus") Integer newStatus);

}
