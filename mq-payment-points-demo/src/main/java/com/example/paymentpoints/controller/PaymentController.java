package com.example.paymentpoints.controller;

import com.example.paymentpoints.common.Result;
import com.example.paymentpoints.dto.CreatePaymentRequest;
import com.example.paymentpoints.dto.PaymentResponse;
import com.example.paymentpoints.entity.PaymentInfo;
import com.example.paymentpoints.entity.UserPointsLog;
import com.example.paymentpoints.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public Result<PaymentResponse> createPayment(@RequestBody CreatePaymentRequest request) {
        return Result.success(paymentService.createPayment(request));
    }

    @PostMapping("/success/{paymentId}")
    public Result<PaymentResponse> markPaid(@PathVariable Long paymentId) {
        return Result.success(paymentService.markPaid(paymentId));
    }

    @GetMapping("/{paymentId}")
    public Result<PaymentInfo> getPayment(@PathVariable Long paymentId) {
        return Result.success(paymentService.getPaymentById(paymentId));
    }

    @GetMapping("/points/logs")
    public Result<List<UserPointsLog>> listPointsLogs() {
        return Result.success(paymentService.listPointsLogs());
    }
}