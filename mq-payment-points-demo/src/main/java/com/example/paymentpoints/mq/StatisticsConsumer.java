package com.example.paymentpoints.mq;

import com.example.paymentpoints.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Fanout 广播模式 —— 数据统计消费者
 *
 * 监听 statistics.queue 队列，处理数据统计逻辑
 *
 * 三种消费者的对比理解：
 * - SmsNotifyConsumer  → sms.notify.queue   → 发短信
 * - EmailNotifyConsumer → email.notify.queue → 发邮件
 * - StatisticsConsumer  → statistics.queue   → 记录统计
 *
 * 它们监听不同的队列，但通过 Fanout Exchange 的广播机制，收到的是同一条消息
 * 各自独立消费，互不干扰，任何一个消费失败都不影响其他消费者
 */
@Component
public class StatisticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(StatisticsConsumer.class);

    @RabbitListener(queues = Constants.STATISTICS_QUEUE)
    public void receive(PaymentNotifyMessage message) {
        log.info("[统计服务] 收到支付成功通知 -> 用户ID: {}, 订单号: {}, 金额: {}",
                message.getUserId(), message.getOrderNo(), message.getPayAmount());
        // 实际项目中这里会将支付数据写入统计报表或数仓
        // 例如：statisticsService.recordPayment(message);
    }
}
