package com.example.paymentpoints.mq;

import com.example.paymentpoints.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Headers 头部匹配模式 —— 退款处理消费者
 *
 * 监听 refund.queue 队列
 * 绑定条件：whereAny({"type": "refund"}) —— 消息头中只要包含 type=refund 即匹配
 *
 * whereAny 的含义（OR 逻辑）：
 * - 消息头中只要包含 Binding 声明的任意一个键值对就匹配
 * - 本队列只声明了 {"type": "refund"}，所以只要 type=refund 就匹配
 * - 如果声明了 {"type": "refund", "priority": "high"}，则满足其一即匹配
 *
 * ★★★ whereAll vs whereAny 的关键区别 ★★★
 * 假设 Binding 声明了两个条件 {"priority": "high", "type": "refund"}：
 *
 * - whereAll：消息头必须同时包含 priority=high AND type=refund 才匹配
 * - whereAny：消息头只要包含 priority=high OR type=refund 就匹配
 *
 * 本项目中：
 * - 高优先级队列用 whereAll({"priority": "high"})  → 只看 priority
 * - 退款队列用 whereAny({"type": "refund"})         → 只看 type
 *
 * 所以当消息头 = {priority=high, type=refund} 时，两个队列都能匹配到！
 * 这与 Topic 模式中一条消息匹配多个 binding 的效果类似
 */
@Component
public class RefundConsumer {

    private static final Logger log = LoggerFactory.getLogger(RefundConsumer.class);

    @RabbitListener(queues = Constants.REFUND_QUEUE)
    public void receive(PaymentActionMessage message) {
        log.info("[退款处理] 支付单号: {}, 用户ID: {}, 金额: {}, 描述: {}",
                message.getPaymentNo(), message.getUserId(), message.getAmount(), message.getDescription());
        // 实际项目中退款流程可能包括：
        // - 调用支付渠道退款接口
        // - 恢复库存
        // - 发送退款通知
    }
}
