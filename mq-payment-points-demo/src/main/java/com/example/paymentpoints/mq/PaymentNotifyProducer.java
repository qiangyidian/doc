package com.example.paymentpoints.mq;

import com.example.paymentpoints.common.Constants;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Fanout 广播模式的消息生产者
 *
 * 核心特点：
 * - 发送到 Fanout Exchange 时，不需要指定 routing key
 * - 即使指定了 routing key 也会被忽略
 * - 消息会被广播到该交换机绑定的所有队列
 *
 * 对比 Direct 模式的 PaymentSuccessProducer：
 * - Direct：convertAndSend(exchange, routingKey, message)  → 需要 routing key
 * - Fanout：convertAndSend(exchange, "", message)           → routing key 传空串即可
 */
@Component
public class PaymentNotifyProducer {

    private final RabbitTemplate rabbitTemplate;

    public PaymentNotifyProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送支付通知（广播模式）
     *
     * 调用后，所有绑定到 payment.fanout.exchange 的队列都会收到这条消息：
     * - sms.notify.queue     → 短信服务消费
     * - email.notify.queue   → 邮件服务消费
     * - statistics.queue     → 统计服务消费
     *
     * 注意：第二个参数 routing key 传了空字符串 ""，因为 Fanout 模式会忽略它
     */
    public void send(PaymentNotifyMessage message) {
        rabbitTemplate.convertAndSend(
                Constants.PAYMENT_FANOUT_EXCHANGE,
                "",     // Fanout 模式忽略 routing key，传空串即可
                message
        );
    }
}
