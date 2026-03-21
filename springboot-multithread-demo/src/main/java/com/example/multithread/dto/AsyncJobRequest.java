package com.example.multithread.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AsyncJobRequest {

    // 模拟订单号。
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    // 模拟通知手机号。
    @NotBlank(message = "手机号不能为空")
    private String phone;
}