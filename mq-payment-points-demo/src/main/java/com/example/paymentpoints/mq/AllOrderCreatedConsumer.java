package com.example.paymentpoints.mq;

import com.example.paymentpoints.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Topic 主题模式 —— 所有订单创建事件消费者
 *
 * 监听 all.order.created.queue 队列，binding key = "order.*.created"
 *
 * 匹配规则：不管 VIP 还是普通，只要事件是 created 就会匹配
 * - order.vip.created    ✓ 匹配（* 匹配 vip）
 * - order.normal.created ✓ 匹配（* 匹配 normal）
 * - order.vip.paid       ✗ 不匹配（event 不是 created）
 * - order.normal.cancelled ✗ 不匹配（event 不是 created）
 *
 * ★★★ Topic 模式最重要的理解点 ★★★
 * 当发送 routing key = "order.vip.created" 时：
 * - 它同时匹配了 order.vip.* 和 order.*.created 两个 binding
 * - 因此 VIP 队列和本队列都会收到这条消息！
 * - 这不是重复消费，而是 Topic 的多队列匹配特性
 * - 每个队列收到的是消息的独立副本，互不影响
 */
@Component
public class AllOrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(AllOrderCreatedConsumer.class);

    @RabbitListener(queues = Constants.ALL_ORDER_CREATED_QUEUE)
    public void receive(OrderEventMessage message) {
        log.info("[所有订单创建事件] 等级: {}, 订单号: {}, 金额: {}, 描述: {}",
                message.getLevel(), message.getOrderNo(), message.getAmount(), message.getDescription());
        // 实际项目中可能用于全局订单创建通知
        // 例如：发送运营通知、记录全局日志、触发风控检查等
    }
}
