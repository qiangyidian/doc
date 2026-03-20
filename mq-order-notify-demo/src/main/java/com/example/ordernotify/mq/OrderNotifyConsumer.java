package com.example.ordernotify.mq;

import com.example.ordernotify.common.Constants;
import com.example.ordernotify.service.NotifyService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * MQ 消费者。
 *
 * 消费成功后手动 ACK。
 * 消费失败时 NACK 并重新入队。
 */
@Component
public class OrderNotifyConsumer {

    private final NotifyService notifyService;

    public OrderNotifyConsumer(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @RabbitListener(queues = Constants.ORDER_NOTIFY_QUEUE)
    public void receive(OrderNotifyMessage message, Message amqpMessage, Channel channel) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();

        try {
            notifyService.handleOrderNotify(message);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            channel.basicNack(deliveryTag, false, true);
            throw ex;
        }
    }
}