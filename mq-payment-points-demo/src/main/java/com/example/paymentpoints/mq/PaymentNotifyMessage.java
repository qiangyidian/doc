package com.example.paymentpoints.mq;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Fanout 广播模式的消息体
 *
 * 场景：支付成功后需要同时通知多个下游服务（短信、邮件、统计）
 * 所有绑定到 Fanout Exchange 的队列都会收到这条消息的副本
 *
 * 与 Direct 模式的 PaymentSuccessMessage 的区别：
 * - Direct 模式：一条消息只会路由到一个队列（精确匹配 routing key）
 * - Fanout 模式：一条消息会广播到所有绑定队列（忽略 routing key）
 */
@Data
public class PaymentNotifyMessage {

    /** 支付单ID */
    private Long paymentId;

    /** 支付单号 */
    private String paymentNo;

    /** 用户ID */
    private Long userId;

    /** 订单号 */
    private String orderNo;

    /** 支付金额 */
    private BigDecimal payAmount;

    /** 用户手机号（短信服务需要） */
    private String phone;

    /** 用户邮箱（邮件服务需要） */
    private String email;
}
