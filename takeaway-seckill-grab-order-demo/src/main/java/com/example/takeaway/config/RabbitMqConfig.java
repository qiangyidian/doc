package com.example.takeaway.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // 交换机名称。
    public static final String CACHE_INVALID_EXCHANGE = "takeaway.cache.invalid.exchange";

    // 队列名称。
    public static final String CACHE_INVALID_QUEUE = "takeaway.cache.invalid.queue";

    // 路由键。
    public static final String CACHE_INVALID_ROUTING_KEY = "takeaway.cache.invalid.meal";

    @Bean
    public DirectExchange cacheInvalidExchange() {
        // DirectExchange 表示精确路由。
        return new DirectExchange(CACHE_INVALID_EXCHANGE, true, false);
    }

    @Bean
    public Queue cacheInvalidQueue() {
        // durable=true 表示队列持久化。
        return new Queue(CACHE_INVALID_QUEUE, true);
    }

    @Bean
    public MessageConverter messageConverter() {
        // 把 Java 对象自动转换成 JSON。
        // 如果不配这个转换器，RabbitTemplate 直接发送普通 Java 对象时会更容易报错。
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Binding cacheInvalidBinding(Queue cacheInvalidQueue,
                                       DirectExchange cacheInvalidExchange) {
        // 把队列绑定到交换机上，并指定路由键。
        return BindingBuilder.bind(cacheInvalidQueue)
                .to(cacheInvalidExchange)
                .with(CACHE_INVALID_ROUTING_KEY);
    }
}