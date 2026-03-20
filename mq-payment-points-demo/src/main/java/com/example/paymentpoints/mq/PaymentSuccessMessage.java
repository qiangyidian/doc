package com.example.paymentpoints.mq;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentSuccessMessage {

    private Long paymentId;

    private String paymentNo;

    private Long userId;

    private String orderNo;

    private BigDecimal payAmount;

    private Integer points;

    private LocalDateTime messageTime;
}