package com.example.paymentpoints.mq;

import com.example.paymentpoints.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Topic 主题模式 —— 普通订单消费者
 *
 * 监听 normal.order.queue 队列，binding key = "order.normal.*"
 *
 * 匹配规则：所有普通等级的订单事件都会被路由到这里
 * - order.normal.created   ✓ 匹配
 * - order.normal.paid      ✓ 匹配
 * - order.normal.cancelled ✓ 匹配
 * - order.vip.created      ✗ 不匹配（level 不是 normal）
 *
 * 与 VipOrderConsumer 的对比：
 * - 两者监听不同的队列，binding key 也不同
 * - 一条消息只会匹配其中之一（要么 VIP，要么普通）
 * - 但如果 routing key 是 order.vip.created，它还会同时匹配 order.*.created
 */
@Component
public class NormalOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(NormalOrderConsumer.class);

    @RabbitListener(queues = Constants.NORMAL_ORDER_QUEUE)
    public void receive(OrderEventMessage message) {
        log.info("[普通订单处理] 等级: {}, 事件: {}, 订单号: {}, 金额: {}",
                message.getLevel(), message.getEvent(), message.getOrderNo(), message.getAmount());
        // 普通订单的标准处理流程
    }
}
