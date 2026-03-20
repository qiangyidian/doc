package com.example.paymentpoints.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentResponse {

    private Long paymentId;

    private String paymentNo;

    private String payStatus;
}