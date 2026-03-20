package com.example.takeaway.service;

import com.example.takeaway.dto.SeckillOrderRequest;

public interface SeckillService {

    String createOrder(SeckillOrderRequest request);
}