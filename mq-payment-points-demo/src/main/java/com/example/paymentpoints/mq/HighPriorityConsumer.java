package com.example.paymentpoints.mq;

import com.example.paymentpoints.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Headers 头部匹配模式 —— 高优先级处理消费者
 *
 * 监听 high.priority.queue 队列
 * 绑定条件：whereAll({"priority": "high"}) —— 必须消息头中 priority=high 才匹配
 *
 * whereAll 的含义（AND 逻辑）：
 * - 消息头必须包含 Binding 中声明的所有键值对
 * - 本队列只声明了 {"priority": "high"}，所以只要 priority=high 就匹配
 * - 如果声明了 {"priority": "high", "type": "refund"}，则消息头必须同时包含这两个才匹配
 *
 * 与 whereAny 的区别：
 * - whereAll：必须全部匹配（AND）—— 更严格
 * - whereAny：匹配任意一个即可（OR）—— 更宽松
 */
@Component
public class HighPriorityConsumer {

    private static final Logger log = LoggerFactory.getLogger(HighPriorityConsumer.class);

    @RabbitListener(queues = Constants.HIGH_PRIORITY_QUEUE)
    public void receive(PaymentActionMessage message) {
        log.info("[高优先级处理] 支付单号: {}, 用户ID: {}, 金额: {}, 描述: {}",
                message.getPaymentNo(), message.getUserId(), message.getAmount(), message.getDescription());
        // 实际项目中高优先级消息可能需要：
        // - 立即处理（跳过排队）
        // - 发送告警通知
        // - 记录审计日志
    }
}
