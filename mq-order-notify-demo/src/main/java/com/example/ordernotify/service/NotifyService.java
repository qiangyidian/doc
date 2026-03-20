package com.example.ordernotify.service;

import com.example.ordernotify.mq.OrderNotifyMessage;

public interface NotifyService {

    void handleOrderNotify(OrderNotifyMessage message);
}