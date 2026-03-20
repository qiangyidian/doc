package com.example.takeaway.service;

import com.example.takeaway.entity.OrderInfo;

import java.math.BigDecimal;

public interface OrderService {

    OrderInfo createOrder(Long userId, Long mealId, String orderNo, BigDecimal amount);

    OrderInfo queryByOrderNo(String orderNo);
}