package com.example.multithread.dto;

import lombok.Data;

@Data
public class OrderSummaryResponse {

    // 订单 ID。
    private Long orderId;

    // 用户名称。
    private String userName;

    // 商品价格描述。
    private String priceInfo;

    // 库存描述。
    private String stockInfo;

    // 这次接口总共耗时多少毫秒。
    private long costMillis;
}