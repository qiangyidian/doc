package com.example.paymentpoints.controller;

import com.example.paymentpoints.common.Result;
import com.example.paymentpoints.mq.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * MQ 模式测试控制器
 *
 * 提供独立的接口，方便手动测试四种 Exchange 模式的消息发送
 * 可以通过 Postman 或 curl 调用这些接口，观察不同模式下的路由效果
 *
 * 四种模式对比：
 * ┌───────────┬────────────────────────────────┬──────────────────────────────────┐
 * │ Exchange  │ 路由依据                        │ 特点                              │
 * ├───────────┼────────────────────────────────┼──────────────────────────────────┤
 * │ Direct    │ routing key 精确匹配            │ 一对一，最简单                      │
 * │ Fanout    │ 无条件广播                       │ 一对多，最暴力                      │
 * │ Topic     │ routing key 通配符匹配          │ 一对多，最灵活                      │
 * │ Headers   │ 消息头键值对匹配                 │ 一对多，不看 routing key            │
 * └───────────┴────────────────────────────────┴──────────────────────────────────┘
 */
@RestController
@RequestMapping("/mq-test")
public class MqTestController {

    private final PaymentSuccessProducer directProducer;
    private final PaymentNotifyProducer fanoutProducer;
    private final OrderEventProducer topicProducer;
    private final PaymentActionProducer headersProducer;

    public MqTestController(PaymentSuccessProducer directProducer,
                            PaymentNotifyProducer fanoutProducer,
                            OrderEventProducer topicProducer,
                            PaymentActionProducer headersProducer) {
        this.directProducer = directProducer;
        this.fanoutProducer = fanoutProducer;
        this.topicProducer = topicProducer;
        this.headersProducer = headersProducer;
    }

    // ==================== Direct 模式测试 ====================

    /**
     * 测试 Direct Exchange —— 精确匹配 routing key
     *
     * 调用后只有 payment.success.queue（积分队列）会收到消息
     *
     * 测试命令：POST /mq-test/direct
     */
    @PostMapping("/direct")
    public Result<String> testDirect() {
        PaymentSuccessMessage message = new PaymentSuccessMessage();
        message.setPaymentId(1L);
        message.setPaymentNo("PAY20260413001");
        message.setUserId(100L);
        message.setOrderNo("ORD20260413001");
        message.setPayAmount(new BigDecimal("99.00"));
        message.setPoints(99);

        directProducer.send(message);
        return Result.success("Direct 模式消息已发送 → payment.success.queue（积分发放）");
    }

    // ==================== Fanout 模式测试 ====================

    /**
     * 测试 Fanout Exchange —— 广播到所有绑定队列
     *
     * 调用后三个队列都会收到消息：
     * - sms.notify.queue（短信）
     * - email.notify.queue（邮件）
     * - statistics.queue（统计）
     *
     * 测试命令：POST /mq-test/fanout
     */
    @PostMapping("/fanout")
    public Result<String> testFanout() {
        PaymentNotifyMessage message = new PaymentNotifyMessage();
        message.setPaymentId(1L);
        message.setPaymentNo("PAY20260413001");
        message.setUserId(100L);
        message.setOrderNo("ORD20260413001");
        message.setPayAmount(new BigDecimal("199.00"));
        message.setPhone("138****8888");
        message.setEmail("user@example.com");

        fanoutProducer.send(message);
        return Result.success("Fanout 模式消息已发送 → sms + email + statistics 三个队列同时收到");
    }

    // ==================== Topic 模式测试 ====================

    /**
     * 测试 Topic Exchange —— 通配符匹配 routing key
     *
     * 通过 level 和 event 参数动态构建 routing key
     * 不同的 routing key 会匹配不同的 Binding，路由到不同的队列
     *
     * 测试命令示例：
     *   POST /mq-test/topic?vip=vip&event=created   → routing key = order.vip.created
     *   POST /mq-test/topic?level=normal&event=paid  → routing key = order.normal.paid
     *
     * 路由结果：
     *   order.vip.created    → VIP队列 ✓ + 所有创建事件队列 ✓（同时匹配两个 binding）
     *   order.vip.paid       → VIP队列 ✓
     *   order.normal.created → 普通队列 ✓ + 所有创建事件队列 ✓
     *   order.normal.paid    → 普通队列 ✓
     */
    @PostMapping("/topic")
    public Result<String> testTopic(
            @RequestParam(defaultValue = "vip") String level,
            @RequestParam(defaultValue = "created") String event) {

        OrderEventMessage message = new OrderEventMessage();
        message.setOrderNo("ORD20260413001");
        message.setUserId(100L);
        message.setLevel(level);
        message.setEvent(event);
        message.setAmount(new BigDecimal("299.00"));
        message.setDescription("Topic模式测试");

        topicProducer.send(message);

        String routingKey = "order." + level + "." + event;
        return Result.success("Topic 模式消息已发送，routing key = " + routingKey +
                "。请查看日志确认哪些消费者收到了消息");
    }

    // ==================== Headers 模式测试 ====================

    /**
     * 测试 Headers Exchange —— 根据消息头匹配
     *
     * 通过 priority 和 type 参数设置消息头
     * 不同的 headers 组合会匹配不同的 Binding，路由到不同的队列
     *
     * 测试命令示例：
     *   POST /mq-test/headers?priority=high&type=payment   → 高优先级队列 ✓
     *   POST /mq-test/headers?priority=normal&type=refund  → 退款队列 ✓
     *   POST /mq-test/headers?priority=high&type=refund    → 高优先级队列 ✓ + 退款队列 ✓（同时匹配）
     *
     * 路由结果：
     *   {priority=high, type=payment}   → 高优先级队列 ✓（whereAll 匹配 priority=high）
     *   {priority=normal, type=refund}  → 退款队列 ✓（whereAny 匹配 type=refund）
     *   {priority=high, type=refund}    → 两个队列都 ✓（同时匹配两个 binding）
     *   {priority=normal, type=payment} → 无队列匹配 ✗
     */
    @PostMapping("/headers")
    public Result<String> testHeaders(
            @RequestParam(defaultValue = "high") String priority,
            @RequestParam(defaultValue = "payment") String type) {

        PaymentActionMessage message = new PaymentActionMessage();
        message.setPaymentNo("PAY20260413001");
        message.setUserId(100L);
        message.setOrderNo("ORD20260413001");
        message.setAmount(new BigDecimal("399.00"));
        message.setDescription("Headers模式测试");

        headersProducer.send(message, priority, type);

        return Result.success("Headers 模式消息已发送，headers = {priority=" + priority +
                ", type=" + type + "}。请查看日志确认哪些消费者收到了消息");
    }
}
