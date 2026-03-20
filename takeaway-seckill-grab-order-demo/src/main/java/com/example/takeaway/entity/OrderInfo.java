package com.example.takeaway.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderInfo {

    private Long id;

    // 订单号。
    private String orderNo;

    // 下单用户 ID。
    private Long userId;

    // 菜品 ID。
    private Long mealId;

    // 订单金额。
    private BigDecimal orderAmount;

    // 订单状态，例如 CREATED。
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}