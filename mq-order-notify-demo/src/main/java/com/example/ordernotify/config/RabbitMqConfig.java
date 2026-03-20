package com.example.ordernotify.config;

import com.example.ordernotify.common.Constants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类。
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange orderNotifyExchange() {
        return new DirectExchange(Constants.ORDER_NOTIFY_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderNotifyQueue() {
        return new Queue(Constants.ORDER_NOTIFY_QUEUE, true);
    }

    @Bean
    public Binding orderNotifyBinding(Queue orderNotifyQueue, DirectExchange orderNotifyExchange) {
        return BindingBuilder.bind(orderNotifyQueue)
                .to(orderNotifyExchange)
                .with(Constants.ORDER_NOTIFY_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}