package com.example.ordernotify.service.impl;

import com.example.ordernotify.entity.OrderNotifyLog;
import com.example.ordernotify.mapper.OrderNotifyLogMapper;
import com.example.ordernotify.mq.OrderNotifyMessage;
import com.example.ordernotify.service.NotifyService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 模拟通知发送服务。
 *
 * 这里不接真实短信平台，只往日志表插一条记录。
 * 这样可以非常直观地看到消费者到底有没有执行成功。
 */
@Service
public class NotifyServiceImpl implements NotifyService {

    private final OrderNotifyLogMapper orderNotifyLogMapper;

    public NotifyServiceImpl(OrderNotifyLogMapper orderNotifyLogMapper) {
        this.orderNotifyLogMapper = orderNotifyLogMapper;
    }

    @Override
    public void handleOrderNotify(OrderNotifyMessage message) {
        OrderNotifyLog log = new OrderNotifyLog();
        log.setOrderNo(message.getOrderNo());
        log.setNotifyType("SMS");
        log.setNotifyStatus("SUCCESS");
        log.setRemark("模拟发送通知成功，商品：" + message.getProductName());
        log.setCreateTime(LocalDateTime.now());
        orderNotifyLogMapper.insert(log);
    }
}