package com.zsh.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zsh.entity.SeckillActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SeckillActivityDao extends BaseMapper<SeckillActivity> {
    /**
     * 查询正在进行的秒杀活动
     */
    @Select("SELECT * FROM seckill_activity WHERE status = 1 AND start_time <= NOW() AND end_time >= NOW()")
    List<SeckillActivity> selectActiveActivities();

    /**
     * 扣减活动库存（乐观锁）
     */
    @Update("UPDATE seckill_activity SET available_stock = available_stock - #{quantity}, " +
    "version = version + 1 WHERE id = #{activityId} AND available_stock >= #{quantity}")
    int deductStock(@Param("activityId") Long activityId,
                    @Param("quantity") Integer quantity);

    /**
     * 批量查询活动
     */
    List<SeckillActivity> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 更新活动状态
     */
    @Update("UPDATE seckill_activity SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
