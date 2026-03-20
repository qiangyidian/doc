package com.example.paymentpoints.mq;

import com.example.paymentpoints.common.Constants;
import com.example.paymentpoints.service.PointsService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PaymentSuccessConsumer {

    private final PointsService pointsService;

    public PaymentSuccessConsumer(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @RabbitListener(queues = Constants.PAYMENT_SUCCESS_QUEUE)
    public void receive(PaymentSuccessMessage message, Message amqpMessage, Channel channel) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();

        try {
            pointsService.grantPoints(message);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            channel.basicNack(deliveryTag, false, true);
            throw ex;
        }
    }
}