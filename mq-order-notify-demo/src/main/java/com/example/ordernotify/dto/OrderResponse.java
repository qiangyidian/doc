package com.example.ordernotify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 创建订单成功后的响应对象。
 */
@Data
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;

    private String orderNo;

    private String status;
}