package com.example.paymentpoints.mq;

import com.example.paymentpoints.common.Constants;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentSuccessProducer {

    private final RabbitTemplate rabbitTemplate;

    public PaymentSuccessProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(PaymentSuccessMessage message) {
        rabbitTemplate.convertAndSend(
                Constants.PAYMENT_SUCCESS_EXCHANGE,
                Constants.PAYMENT_SUCCESS_ROUTING_KEY,
                message
        );
    }
}