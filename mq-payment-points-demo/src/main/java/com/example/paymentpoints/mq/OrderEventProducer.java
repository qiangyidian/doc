package com.example.paymentpoints.mq;

import com.example.paymentpoints.common.Constants;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Topic 主题模式的消息生产者
 *
 * 核心特点：
 * - 发送时指定具体的 routing key（不含通配符），如 "order.vip.created"
 * - Broker 端的 binding key 包含通配符（如 order.vip.*），负责匹配和路由
 * - 一条消息可能同时匹配多个 binding，从而被路由到多个队列
 *
 * 对比三种 Exchange 的 routing key 使用：
 * - Direct：routing key 必须精确匹配 binding key（一对一）
 * - Fanout：routing key 被忽略（广播到所有队列）
 * - Topic：routing key 被 binding key 中的通配符匹配（灵活的一对多）
 *
 * routing key 示例与路由结果：
 * ┌──────────────────────┬──────────┬──────────┬──────────────────┐
 * │ routing key          │ VIP队列  │ 普通队列  │ 所有创建事件队列   │
 * │                      │order.vip.*│order.normal.*│order.*.created │
 * ├──────────────────────┼──────────┼──────────┼──────────────────┤
 * │ order.vip.created    │    ✓     │    ✗     │       ✓          │
 * │ order.vip.paid       │    ✓     │    ✗     │       ✗          │
 * │ order.normal.created │    ✗     │    ✓     │       ✓          │
 * │ order.normal.paid    │    ✗     │    ✓     │       ✗          │
 * └──────────────────────┴──────────┴──────────┴──────────────────┘
 * 注意：order.vip.created 同时匹配 order.vip.* 和 order.*.created，所以会被路由到两个队列！
 */
@Component
public class OrderEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public OrderEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送订单事件消息（Topic 模式）
     *
     * @param message 消息体，包含 level 和 event 字段
     *
     * routing key 由 message.getLevel() 和 message.getEvent() 拼接而成
     * 例如：level=vip, event=created → routing key = "order.vip.created"
     *
     * Broker 端会根据 binding key 中的通配符规则进行匹配路由
     */
    public void send(OrderEventMessage message) {
        // 拼接 routing key：order.<level>.<event>
        // 这是 Topic 模式的关键 —— routing key 的层级设计决定了路由的灵活性
        String routingKey = "order." + message.getLevel() + "." + message.getEvent();

        rabbitTemplate.convertAndSend(
                Constants.ORDER_TOPIC_EXCHANGE,
                routingKey,  // Topic 模式必须指定 routing key，由 Broker 端通配符匹配
                message
        );
    }
}
