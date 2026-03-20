package com.example.paymentpoints.config;

import com.example.paymentpoints.common.Constants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange paymentSuccessExchange() {
        return new DirectExchange(Constants.PAYMENT_SUCCESS_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return new Queue(Constants.PAYMENT_SUCCESS_QUEUE, true);
    }

    @Bean
    public Binding paymentSuccessBinding(Queue paymentSuccessQueue, DirectExchange paymentSuccessExchange) {
        return BindingBuilder.bind(paymentSuccessQueue)
                .to(paymentSuccessExchange)
                .with(Constants.PAYMENT_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}