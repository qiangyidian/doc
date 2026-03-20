package com.example.paymentpoints.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_points_log")
public class UserPointsLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String paymentNo;

    private Integer points;

    private String bizType;

    private String remark;

    private LocalDateTime createTime;
}