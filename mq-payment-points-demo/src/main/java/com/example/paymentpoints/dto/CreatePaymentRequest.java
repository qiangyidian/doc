package com.example.paymentpoints.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePaymentRequest {

    private Long userId;

    private String orderNo;

    private BigDecimal payAmount;
}