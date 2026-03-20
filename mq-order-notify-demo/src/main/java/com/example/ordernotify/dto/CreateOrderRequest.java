package com.example.ordernotify.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单接口的请求对象。
 */
@Data
public class CreateOrderRequest {

    private Long userId;

    private String productName;

    private BigDecimal amount;
}