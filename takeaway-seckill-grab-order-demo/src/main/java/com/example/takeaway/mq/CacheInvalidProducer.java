package com.example.takeaway.mq;

import com.example.takeaway.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CacheInvalidProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(Long mealId, Long merchantId, String reason) {
        CacheInvalidMessage message = new CacheInvalidMessage();
        // 记录是哪一个菜品要删缓存。
        message.setMealId(mealId);
        // 记录商家 ID，方便删除热门菜单缓存。
        message.setMerchantId(merchantId);
        // 记录本次删除缓存的原因。
        message.setReason(reason);
        // 记录消息发送时间，方便排查问题。
        message.setEventTime(LocalDateTime.now());

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.CACHE_INVALID_EXCHANGE,
                RabbitMqConfig.CACHE_INVALID_ROUTING_KEY,
                message
        );
    }
}