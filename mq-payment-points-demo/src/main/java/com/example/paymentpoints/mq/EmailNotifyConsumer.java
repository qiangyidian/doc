package com.example.paymentpoints.mq;

import com.example.paymentpoints.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Fanout 广播模式 —— 邮件通知消费者
 *
 * 监听 email.notify.queue 队列，处理邮件发送逻辑
 *
 * 注意：与 SmsNotifyConsumer 监听的是不同的队列，但它们收到的是同一条消息的副本
 * 这就是 Fanout 广播模式的特性——一条消息，多个消费者各自收到
 */
@Component
public class EmailNotifyConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailNotifyConsumer.class);

    @RabbitListener(queues = Constants.EMAIL_NOTIFY_QUEUE)
    public void receive(PaymentNotifyMessage message) {
        log.info("[邮件服务] 收到支付成功通知 -> 邮箱: {}, 订单号: {}, 金额: {}",
                message.getEmail(), message.getOrderNo(), message.getPayAmount());
        // 实际项目中这里会调用邮件 SDK 发送邮件
        // 例如：emailClient.send(message.getEmail(), "支付成功", "...");
    }
}
