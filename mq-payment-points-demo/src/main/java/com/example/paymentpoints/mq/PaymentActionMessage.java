package com.example.paymentpoints.mq;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Headers 头部匹配模式的消息体
 *
 * 场景：根据消息头中的 priority（优先级）和 type（业务类型）属性进行路由
 *
 * Headers Exchange 与其他三种 Exchange 的根本区别：
 * - Direct/Topic：依赖 routing key 字符串匹配
 * - Fanout：忽略 routing key，无条件广播
 * - Headers：完全忽略 routing key，根据消息头（MessageProperties.headers）中的键值对匹配
 *
 * 消息头（headers）是在发送消息时附带的元数据，不是消息体的一部分
 * 发送时通过 MessagePostProcessor 设置 headers：
 *   rabbitTemplate.convertAndSend(exchange, "", message, msg -> {
 *       msg.getMessageProperties().setHeader("priority", "high");
 *       msg.getMessageProperties().setHeader("type", "payment");
 *       return msg;
 *   });
 *
 * Broker 端的匹配方式：
 * - whereAll()：消息头必须包含所有指定的键值对才匹配（AND 逻辑）
 * - whereAny()：消息头包含任意一个指定的键值对即匹配（OR 逻辑）
 */
@Data
public class PaymentActionMessage {

    /** 支付单号 */
    private String paymentNo;

    /** 用户ID */
    private Long userId;

    /** 订单号 */
    private String orderNo;

    /** 操作金额 */
    private BigDecimal amount;

    /** 操作描述 */
    private String description;
}
