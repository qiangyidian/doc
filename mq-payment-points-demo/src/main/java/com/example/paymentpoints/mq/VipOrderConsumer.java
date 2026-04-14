package com.example.paymentpoints.mq;

import com.example.paymentpoints.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Topic 主题模式 —— VIP 订单消费者
 *
 * 监听 vip.order.queue 队列，binding key = "order.vip.*"
 *
 * 匹配规则：所有 VIP 等级的订单事件都会被路由到这里
 * - order.vip.created   ✓ 匹配
 * - order.vip.paid      ✓ 匹配
 * - order.vip.cancelled ✓ 匹配
 * - order.normal.created ✗ 不匹配（level 不是 vip）
 *
 * 重点理解 * 通配符：
 * - order.vip.* 中的 * 只匹配一个单词
 * - 所以 order.vip.created 匹配，但 order.vip.extra.created 不匹配（多了一层）
 * - 如果想匹配多层，需要用 # 通配符，如 order.vip.#
 */
@Component
public class VipOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(VipOrderConsumer.class);

    @RabbitListener(queues = Constants.VIP_ORDER_QUEUE)
    public void receive(OrderEventMessage message) {
        log.info("[VIP订单处理] 等级: {}, 事件: {}, 订单号: {}, 金额: {}",
                message.getLevel(), message.getEvent(), message.getOrderNo(), message.getAmount());
        // 实际项目中 VIP 订单可能有专属处理逻辑
        // 例如：优先发货、专属客服通知、加倍积分等
    }
}
