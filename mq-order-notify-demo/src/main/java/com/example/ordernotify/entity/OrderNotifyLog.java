package com.example.ordernotify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知日志实体类，对应 t_order_notify_log 表。
 */
@Data
@TableName("t_order_notify_log")
public class OrderNotifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private String notifyType;

    private String notifyStatus;

    private String remark;

    private LocalDateTime createTime;
}