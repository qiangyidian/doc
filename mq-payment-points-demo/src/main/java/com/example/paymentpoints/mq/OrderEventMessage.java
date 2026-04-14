package com.example.paymentpoints.mq;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Topic 主题模式的消息体
 *
 * 场景：订单事件按等级（VIP/普通）和事件类型（created/paid/cancelled）路由
 *
 * Topic 模式的核心 —— routing key 的设计：
 * - routing key 格式：order.<level>.<event>
 * - level 可以是 vip 或 normal
 * - event 可以是 created、paid、cancelled 等
 * - Broker 端通过通配符 binding key 来匹配
 *
 * 通配符说明：
 *   * (星号) —— 精确匹配一个单词
 *     例如 order.*.created 匹配 order.vip.created，但不匹配 order.vip.extra.created
 *   # (井号) —— 匹配零个或多个单词
 *     例如 order.# 匹配 order.vip、order.vip.created、order.normal.paid.extra 等
 */
@Data
public class OrderEventMessage {

    /** 订单号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 订单等级：vip / normal */
    private String level;

    /** 事件类型：created / paid / cancelled */
    private String event;

    /** 订单金额 */
    private BigDecimal amount;

    /** 订单描述 */
    private String description;
}
