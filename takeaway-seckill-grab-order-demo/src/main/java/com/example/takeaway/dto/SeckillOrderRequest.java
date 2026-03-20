package com.example.takeaway.dto;

import lombok.Data;

@Data
public class SeckillOrderRequest {

    // 下单用户 ID。
    private Long userId;

    // 秒杀菜品 ID。
    private Long mealId;
}