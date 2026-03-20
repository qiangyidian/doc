package com.example.ordernotify.controller;

import com.example.ordernotify.common.Result;
import com.example.ordernotify.dto.CreateOrderRequest;
import com.example.ordernotify.dto.OrderResponse;
import com.example.ordernotify.entity.OrderInfo;
import com.example.ordernotify.entity.OrderNotifyLog;
import com.example.ordernotify.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        return Result.success(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    public Result<OrderInfo> getOrder(@PathVariable Long id) {
        return Result.success(orderService.getOrderById(id));
    }

    @GetMapping("/notify/logs")
    public Result<List<OrderNotifyLog>> listNotifyLogs() {
        return Result.success(orderService.listNotifyLogs());
    }
}