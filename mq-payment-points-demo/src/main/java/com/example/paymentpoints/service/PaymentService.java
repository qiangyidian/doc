package com.example.paymentpoints.service;

import com.example.paymentpoints.dto.CreatePaymentRequest;
import com.example.paymentpoints.dto.PaymentResponse;
import com.example.paymentpoints.entity.PaymentInfo;
import com.example.paymentpoints.entity.UserPointsLog;

import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request);

    PaymentResponse markPaid(Long paymentId);

    PaymentInfo getPaymentById(Long paymentId);

    List<UserPointsLog> listPointsLogs();
}