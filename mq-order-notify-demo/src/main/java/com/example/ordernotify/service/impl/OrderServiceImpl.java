package com.example.ordernotify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ordernotify.dto.CreateOrderRequest;
import com.example.ordernotify.dto.OrderResponse;
import com.example.ordernotify.entity.OrderInfo;
import com.example.ordernotify.entity.OrderNotifyLog;
import com.example.ordernotify.mapper.OrderInfoMapper;
import com.example.ordernotify.mapper.OrderNotifyLogMapper;
import com.example.ordernotify.mq.OrderNotifyMessage;
import com.example.ordernotify.mq.OrderNotifyProducer;
import com.example.ordernotify.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单业务实现类。
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderInfoMapper orderInfoMapper;
    private final OrderNotifyLogMapper orderNotifyLogMapper;
    private final OrderNotifyProducer orderNotifyProducer;

    public OrderServiceImpl(OrderInfoMapper orderInfoMapper,
                            OrderNotifyLogMapper orderNotifyLogMapper,
                            OrderNotifyProducer orderNotifyProducer) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderNotifyLogMapper = orderNotifyLogMapper;
        this.orderNotifyProducer = orderNotifyProducer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (request.getProductName() == null || request.getProductName().isBlank()) {
            throw new IllegalArgumentException("productName 不能为空");
        }
        if (request.getAmount() == null) {
            throw new IllegalArgumentException("amount 不能为空");
        }

        LocalDateTime now = LocalDateTime.now();

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo(generateOrderNo());
        orderInfo.setUserId(request.getUserId());
        orderInfo.setProductName(request.getProductName());
        orderInfo.setAmount(request.getAmount());
        orderInfo.setStatus("CREATED");
        orderInfo.setCreateTime(now);
        orderInfo.setUpdateTime(now);
        orderInfoMapper.insert(orderInfo);

        // 主业务写库成功后，构建 MQ 消息。
        // 这一步代表“把通知动作交给异步系统处理”。
        OrderNotifyMessage message = new OrderNotifyMessage();
        message.setOrderId(orderInfo.getId());
        message.setOrderNo(orderInfo.getOrderNo());
        message.setUserId(orderInfo.getUserId());
        message.setProductName(orderInfo.getProductName());
        message.setAmount(orderInfo.getAmount());
        message.setMessageTime(now);

        orderNotifyProducer.send(message);

        return new OrderResponse(orderInfo.getId(), orderInfo.getOrderNo(), orderInfo.getStatus());
    }

    @Override
    public OrderInfo getOrderById(Long id) {
        return orderInfoMapper.selectById(id);
    }

    @Override
    public List<OrderNotifyLog> listNotifyLogs() {
        return orderNotifyLogMapper.selectList(
                new LambdaQueryWrapper<OrderNotifyLog>().orderByDesc(OrderNotifyLog::getId)
        );
    }

    private String generateOrderNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ORD" + timePart + randomPart;
    }
}