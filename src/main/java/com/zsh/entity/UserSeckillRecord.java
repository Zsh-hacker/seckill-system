package com.zsh.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_seckill_record")
public class UserSeckillRecord extends BaseEntity{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long activityId;
    private Long orderId;
}
