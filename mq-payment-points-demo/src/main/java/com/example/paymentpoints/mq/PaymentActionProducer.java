package com.example.paymentpoints.mq;

import com.example.paymentpoints.common.Constants;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Headers 头部匹配模式的消息生产者
 *
 * 核心特点：
 * - 发送时不指定 routing key（传空串），而是通过消息头（headers）来决定路由
 * - 消息头是附加在 AMQP 消息属性中的键值对，不是消息体的一部分
 * - Broker 根据 Binding 中声明的 header 匹配规则来路由消息
 *
 * 四种 Exchange 的路由依据对比：
 * ┌───────────┬──────────────────────────────────────┐
 * │ Exchange  │ 路由依据                              │
 * ├───────────┼──────────────────────────────────────┤
 * │ Direct    │ routing key 精确匹配                  │
 * │ Fanout    │ 无条件广播（不看任何东西）              │
 * │ Topic     │ routing key 通配符匹配                │
 * │ Headers   │ 消息头键值对匹配（whereAll/whereAny）  │
 * └───────────┴──────────────────────────────────────┘
 *
 * Headers 模式的适用场景：
 * - 需要多维度条件组合路由（如 priority + type）
 * - routing key 不够灵活时（例如需要同时匹配多个独立属性）
 * - 不想设计复杂的 routing key 层级结构时
 */
@Component
public class PaymentActionProducer {

    private final RabbitTemplate rabbitTemplate;

    public PaymentActionProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送支付操作消息（Headers 模式）
     *
     * @param message  消息体
     * @param priority 优先级（"high" / "normal"），写入消息头
     * @param type     业务类型（"payment" / "refund"），写入消息头
     *
     * 关键点：priority 和 type 不是消息体的字段，而是消息头（headers）中的属性
     * Broker 根据 headers 中的键值对匹配 Binding 中声明的条件来路由
     *
     * 路由结果示例：
     * ┌──────────────────────────────────────┬────────────────┬────────────┐
     * │ 消息头 headers                       │ 高优先级队列    │ 退款队列    │
     * │                                      │ whereAll       │ whereAny   │
     * │                                      │ priority=high  │ type=refund│
     * ├──────────────────────────────────────┼────────────────┼────────────┤
     * │ {priority=high, type=payment}        │      ✓         │     ✗      │
     * │ {priority=high, type=refund}         │      ✓         │     ✓      │
     * │ {priority=normal, type=refund}       │      ✗         │     ✓      │
     * │ {priority=normal, type=payment}      │      ✗         │     ✗      │
     * └──────────────────────────────────────┴────────────────┴────────────┘
     */
    public void send(PaymentActionMessage message, String priority, String type) {
        rabbitTemplate.convertAndSend(
                Constants.PAYMENT_HEADERS_EXCHANGE,
                "",     // Headers 模式忽略 routing key
                message,
                msg -> {
                    // 设置消息头 —— 这是 Headers 模式的核心
                    // 消息头是 AMQP 协议的元数据，和消息体（body）是分开的
                    msg.getMessageProperties().setHeader("priority", priority);
                    msg.getMessageProperties().setHeader("type", type);
                    return msg;
                }
        );
    }
}
