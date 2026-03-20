package com.example.ordernotify.mq;

import com.example.ordernotify.common.Constants;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * MQ 生产者。
 */
@Component
public class OrderNotifyProducer {

    private final RabbitTemplate rabbitTemplate;

    public OrderNotifyProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(OrderNotifyMessage message) {
        rabbitTemplate.convertAndSend(
                Constants.ORDER_NOTIFY_EXCHANGE,
                Constants.ORDER_NOTIFY_ROUTING_KEY,
                message
        );
    }
}