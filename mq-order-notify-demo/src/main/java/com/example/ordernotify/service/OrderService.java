package com.example.ordernotify.service;

import com.example.ordernotify.dto.CreateOrderRequest;
import com.example.ordernotify.dto.OrderResponse;
import com.example.ordernotify.entity.OrderInfo;
import com.example.ordernotify.entity.OrderNotifyLog;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderInfo getOrderById(Long id);

    List<OrderNotifyLog> listNotifyLogs();
}