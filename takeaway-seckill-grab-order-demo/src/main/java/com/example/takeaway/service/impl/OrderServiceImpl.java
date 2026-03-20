package com.example.takeaway.service.impl;

import com.example.takeaway.entity.OrderInfo;
import com.example.takeaway.mapper.OrderInfoMapper;
import com.example.takeaway.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderInfoMapper orderInfoMapper;

    @Override
    public OrderInfo createOrder(Long userId, Long mealId, String orderNo, BigDecimal amount) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo(orderNo);
        orderInfo.setUserId(userId);
        orderInfo.setMealId(mealId);
        orderInfo.setOrderAmount(amount);
        orderInfo.setStatus("CREATED");
        orderInfoMapper.insert(orderInfo);
        return orderInfo;
    }

    @Override
    public OrderInfo queryByOrderNo(String orderNo) {
        return orderInfoMapper.selectByOrderNo(orderNo);
    }
}