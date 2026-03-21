package com.example.multithread.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class StockDeductRequest {

    // 扣减数量。
    @Min(value = 1, message = "扣减数量至少为 1")
    private Integer count;
}