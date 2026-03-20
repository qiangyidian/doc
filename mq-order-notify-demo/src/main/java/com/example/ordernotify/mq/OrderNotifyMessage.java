package com.example.ordernotify.mq;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单通知消息体。
 */
@Data
public class OrderNotifyMessage {

    private Long orderId;

    private String orderNo;

    private Long userId;

    private String productName;

    private BigDecimal amount;

    private LocalDateTime messageTime;
}