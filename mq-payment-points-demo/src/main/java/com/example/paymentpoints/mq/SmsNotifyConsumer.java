package com.example.paymentpoints.mq;

import com.example.paymentpoints.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Fanout 广播模式 —— 短信通知消费者
 *
 * 监听 sms.notify.queue 队列，处理短信发送逻辑
 *
 * Fanout 模式的关键理解：
 * - 当 Producer 发送一条消息到 Fanout Exchange 时，
 *   sms.notify.queue、email.notify.queue、statistics.queue 三个队列都会收到同一条消息
 * - 每个消费者独立处理，互不影响
 * - 这就像广播电台：一个信号源，所有收音机都能收到
 */
@Component
public class SmsNotifyConsumer {

    private static final Logger log = LoggerFactory.getLogger(SmsNotifyConsumer.class);

    /**
     * 监听短信通知队列
     * 当 Fanout Exchange 收到消息后，会自动将消息推送到所有绑定队列，包括此队列
     */
    @RabbitListener(queues = Constants.SMS_NOTIFY_QUEUE)
    public void receive(PaymentNotifyMessage message) {
        log.info("[短信服务] 收到支付成功通知 -> 手机号: {}, 订单号: {}, 金额: {}",
                message.getPhone(), message.getOrderNo(), message.getPayAmount());
        // 实际项目中这里会调用短信 SDK 发送短信
        // 例如：smsClient.send(message.getPhone(), "您的订单 " + message.getOrderNo() + " 已支付成功");
    }
}
