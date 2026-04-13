package com.example.lockdemo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DistributedOrderRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;
}