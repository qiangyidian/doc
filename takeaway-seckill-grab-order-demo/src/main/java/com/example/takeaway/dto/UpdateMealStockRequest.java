package com.example.takeaway.dto;

import lombok.Data;

@Data
public class UpdateMealStockRequest {

    // 商家改完后的最新库存。
    private Integer stock;
}