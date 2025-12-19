package com.zsh.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zsh.entity.UserSeckillRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserSeckiillRecordDao extends BaseMapper<UserSeckillRecord> {
    /**
     * 检查用户是否已参与活动
     */
    @Select("SELECT COUNT(1) FROM user_seckill_record WHERE user_id = #{userId} AND activity_id = #{activityId}")
    int checkUserParticipated(@Param("userId") Long userId,
                              @Param("activityId") Long activityId);

    /**
     * 查询用户参与记录
     */
    @Select("SELECT * FROM user_seckill_record WHERE user_id = #{userId} AND activity_id = #{activityId}")
    UserSeckillRecord selectByUserAndActivity(@Param("userId") Long userId,
                                              @Param("activityId") Long activityId);

}
