package com.example.takeaway.controller;

import com.example.takeaway.common.Result;
import com.example.takeaway.dto.SeckillOrderRequest;
import com.example.takeaway.entity.OrderInfo;
import com.example.takeaway.service.OrderService;
import com.example.takeaway.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;
    private final OrderService orderService;

    @PostMapping("/seckill/orders")
    public Result<String> createOrder(@RequestBody SeckillOrderRequest request) {
        String orderNo = seckillService.createOrder(request);
        return Result.success(orderNo);
    }

    @GetMapping("/orders/{orderNo}")
    public Result<OrderInfo> queryOrder(@PathVariable String orderNo) {
        return Result.success(orderService.queryByOrderNo(orderNo));
    }
}