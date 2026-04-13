package com.example.lockdemo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeductStockRequest {

    @NotNull(message = "扣减数量不能为空")
    private Integer count;
}