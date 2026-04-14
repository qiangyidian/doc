package com.example.paymentpoints.config;

import com.example.paymentpoints.common.Constants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置类
 *
 * 本配置类演示了 RabbitMQ 四种核心交换机类型的声明与绑定规则：
 * 1. Direct Exchange  —— 精确匹配 routing key，点对点路由
 * 2. Fanout Exchange  —— 广播模式，忽略 routing key，分发到所有绑定队列
 * 3. Topic Exchange   —— 通配符匹配 routing key，支持 * 和 # 模式
 * 4. Headers Exchange —— 根据消息头键值对匹配，不依赖 routing key
 *
 * 每种交换机类型对应不同的业务场景，理解它们的区别是掌握 MQ 路由机制的关键。
 */
@Configuration
public class RabbitMqConfig {

    // ========================================================================
    // 1. Direct Exchange（直连交换机）—— 精确匹配 routing key
    // ========================================================================
    // 特点：消息的 routing key 必须与 Binding 的 routing key 完全一致才会被路由
    // 适用场景：点对点通信，如支付成功通知积分服务
    // 路由规则：routing key == binding key → 精确匹配
    // ========================================================================

    /**
     * 声明 Direct 交换机
     * 参数说明：
     *   name   = "payment.success.exchange"  交换机名称
     *   durable = true   持久化，Broker 重启后交换机不丢失
     *   autoDelete = false  不自动删除（没有队列绑定时也不删除）
     */
    @Bean
    public DirectExchange paymentSuccessExchange() {
        return new DirectExchange(Constants.PAYMENT_SUCCESS_EXCHANGE, true, false);
    }

    /**
     * 声明队列 —— 积分发放队列
     * 参数说明：
     *   name    = "payment.success.queue"  队列名称
     *   durable = true  持久化，Broker 重启后队列不丢失
     */
    @Bean
    public Queue paymentSuccessQueue() {
        return new Queue(Constants.PAYMENT_SUCCESS_QUEUE, true);
    }

    /**
     * 将队列绑定到 Direct 交换机，指定 routing key
     * 效果：发到 payment.success.exchange 且 routing key = "payment.success.routing" 的消息
     *       会被路由到 payment.success.queue
     */
    @Bean
    public Binding paymentSuccessBinding(Queue paymentSuccessQueue, DirectExchange paymentSuccessExchange) {
        return BindingBuilder.bind(paymentSuccessQueue)
                .to(paymentSuccessExchange)
                .with(Constants.PAYMENT_SUCCESS_ROUTING_KEY);
    }

    // ========================================================================
    // 2. Fanout Exchange（扇出/广播交换机）—— 忽略 routing key，广播到所有绑定队列
    // ========================================================================
    // 特点：不看 routing key，消息会发到该交换机绑定的所有队列
    // 适用场景：一对多通知，如支付成功后同时通知短信、邮件、统计等多个下游服务
    // 路由规则：无条件广播 → 所有绑定队列都能收到
    // ========================================================================

    /**
     * 声明 Fanout 交换机
     * 注意：Fanout 交换机不需要 routing key，绑定也不需要指定 binding key
     */
    @Bean
    public FanoutExchange paymentFanoutExchange() {
        return new FanoutExchange(Constants.PAYMENT_FANOUT_EXCHANGE, true, false);
    }

    /** 短信通知队列 */
    @Bean
    public Queue smsNotifyQueue() {
        return new Queue(Constants.SMS_NOTIFY_QUEUE, true);
    }

    /** 邮件通知队列 */
    @Bean
    public Queue emailNotifyQueue() {
        return new Queue(Constants.EMAIL_NOTIFY_QUEUE, true);
    }

    /** 数据统计队列 */
    @Bean
    public Queue statisticsQueue() {
        return new Queue(Constants.STATISTICS_QUEUE, true);
    }

    /**
     * Fanout 绑定：短信队列 → Fanout 交换机
     * 注意：Fanout 模式的绑定不需要 .with(routingKey)，因为广播模式忽略 routing key
     */
    @Bean
    public Binding smsNotifyBinding(Queue smsNotifyQueue, FanoutExchange paymentFanoutExchange) {
        return BindingBuilder.bind(smsNotifyQueue).to(paymentFanoutExchange);
    }

    /** Fanout 绑定：邮件队列 → Fanout 交换机 */
    @Bean
    public Binding emailNotifyBinding(Queue emailNotifyQueue, FanoutExchange paymentFanoutExchange) {
        return BindingBuilder.bind(emailNotifyQueue).to(paymentFanoutExchange);
    }

    /** Fanout 绑定：统计队列 → Fanout 交换机 */
    @Bean
    public Binding statisticsBinding(Queue statisticsQueue, FanoutExchange paymentFanoutExchange) {
        return BindingBuilder.bind(statisticsQueue).to(paymentFanoutExchange);
    }

    // ========================================================================
    // 3. Topic Exchange（主题交换机）—— 通配符匹配 routing key
    // ========================================================================
    // 特点：routing key 支持通配符
    //   * (星号) —— 匹配恰好一个单词，例如 order.*.created 匹配 order.vip.created
    //   # (井号) —— 匹配零个或多个单词，例如 order.# 匹配 order.vip.created.paid
    // 适用场景：按分类和事件类型路由，如不同等级（VIP/普通）的不同事件（创建/支付/取消）
    // 路由规则：routing key 模式匹配 binding key 中的通配符
    //
    // 示例 routing key：
    //   order.vip.created    → 匹配 order.vip.* 和 order.*.created
    //   order.normal.paid    → 匹配 order.normal.* 但不匹配 order.*.created
    //   order.vip.cancelled  → 匹配 order.vip.* 和 order.*.cancelled (如果有这个绑定)
    // ========================================================================

    /**
     * 声明 Topic 交换机
     */
    @Bean
    public TopicExchange orderTopicExchange() {
        return new TopicExchange(Constants.ORDER_TOPIC_EXCHANGE, true, false);
    }

    /** VIP 订单队列 —— 只接收 VIP 等级的订单事件 */
    @Bean
    public Queue vipOrderQueue() {
        return new Queue(Constants.VIP_ORDER_QUEUE, true);
    }

    /** 普通订单队列 —— 只接收普通等级的订单事件 */
    @Bean
    public Queue normalOrderQueue() {
        return new Queue(Constants.NORMAL_ORDER_QUEUE, true);
    }

    /** 所有订单创建事件队列 —— 不管 VIP 还是普通，只要事件是 created 就接收 */
    @Bean
    public Queue allOrderCreatedQueue() {
        return new Queue(Constants.ALL_ORDER_CREATED_QUEUE, true);
    }

    /**
     * Topic 绑定：VIP 订单队列
     * binding key = "order.vip.*"
     * 匹配示例：order.vip.created ✓  order.vip.paid ✓  order.vip.cancelled ✓
     * 不匹配：order.normal.created ✗  order.vip.created.extra ✗ (因为 * 只匹配一个词)
     */
    @Bean
    public Binding vipOrderBinding(Queue vipOrderQueue, TopicExchange orderTopicExchange) {
        return BindingBuilder.bind(vipOrderQueue)
                .to(orderTopicExchange)
                .with(Constants.VIP_ORDER_ROUTING_KEY);
    }

    /**
     * Topic 绑定：普通订单队列
     * binding key = "order.normal.*"
     * 匹配示例：order.normal.created ✓  order.normal.paid ✓  order.normal.cancelled ✓
     */
    @Bean
    public Binding normalOrderBinding(Queue normalOrderQueue, TopicExchange orderTopicExchange) {
        return BindingBuilder.bind(normalOrderQueue)
                .to(orderTopicExchange)
                .with(Constants.NORMAL_ORDER_ROUTING_KEY);
    }

    /**
     * Topic 绑定：所有订单创建事件队列
     * binding key = "order.*.created"
     * 匹配示例：order.vip.created ✓  order.normal.created ✓
     * 不匹配：order.vip.paid ✗  order.normal.cancelled ✗
     *
     * 重点：当发送 routing key = "order.vip.created" 时，这条消息会同时匹配
     *       order.vip.* 和 order.*.created 两个绑定，因此 VIP 队列和创建事件队列都会收到！
     *       这就是 Topic Exchange 的多队列匹配特性。
     */
    @Bean
    public Binding allOrderCreatedBinding(Queue allOrderCreatedQueue, TopicExchange orderTopicExchange) {
        return BindingBuilder.bind(allOrderCreatedQueue)
                .to(orderTopicExchange)
                .with(Constants.ALL_ORDER_CREATED_ROUTING_KEY);
    }

    // ========================================================================
    // 4. Headers Exchange（头部交换机）—— 根据消息头属性匹配
    // ========================================================================
    // 特点：完全忽略 routing key，根据消息的 headers（键值对）进行匹配
    //   whereAll() —— headers 中必须包含所有指定的键值对才匹配（AND 逻辑）
    //   whereAny() —— headers 中包含任意一个指定的键值对即匹配（OR 逻辑）
    // 适用场景：需要多维度条件路由，如按优先级+业务类型组合判断
    // 路由规则：消息头键值对匹配 binding 中声明的条件
    //
    // 示例：
    //   消息头 {priority=high, type=payment} → 匹配 whereAll("priority", "high")
    //   消息头 {type=refund}                → 匹配 whereAny("type", "refund")
    // ========================================================================

    /**
     * 声明 Headers 交换机
     */
    @Bean
    public HeadersExchange paymentHeadersExchange() {
        return new HeadersExchange(Constants.PAYMENT_HEADERS_EXCHANGE, true, false);
    }

    /** 高优先级处理队列 */
    @Bean
    public Queue highPriorityQueue() {
        return new Queue(Constants.HIGH_PRIORITY_QUEUE, true);
    }

    /** 退款处理队列 */
    @Bean
    public Queue refundQueue() {
        return new Queue(Constants.REFUND_QUEUE, true);
    }

    /**
     * Headers 绑定：高优先级队列
     * 使用 whereAll() —— 必须 headers 中 priority=high 才匹配
     * 即：消息头必须包含 {"priority": "high"} 才会被路由到该队列
     */
    @Bean
     public Binding highPriorityBinding(Queue highPriorityQueue, HeadersExchange paymentHeadersExchange) {
         return BindingBuilder.bind(highPriorityQueue)
                 .to(paymentHeadersExchange)
                .whereAll(highPriorityHeaders()).match();
     }

    /**
     * 高优先级匹配条件：priority = high
     */
    private Map<String, Object> highPriorityHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("priority", "high");
        return headers;
    }

    /**
     * Headers 绑定：退款队列
     * 使用 whereAny() —— headers 中只要包含 type=refund 即匹配（OR 逻辑）
     * 即：消息头只要包含 {"type": "refund"} 就会被路由到该队列
     */
    @Bean
     public Binding refundBinding(Queue refundQueue, HeadersExchange paymentHeadersExchange) {
         return BindingBuilder.bind(refundQueue)
                 .to(paymentHeadersExchange)
                .whereAny(refundHeaders()).match();
     }

    /**
     * 退款匹配条件：type = refund
     */
    private Map<String, Object> refundHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("type", "refund");
        return headers;
    }

    // ========================================================================
    // 公共配置：JSON 消息转换器
    // ========================================================================

    /**
     * Jackson JSON 消息转换器
     * 作用：将 Java 对象序列化为 JSON 格式发送，消费时自动反序列化
     * 替代默认的 Java 序列化（不可读、不安全、跨语言不兼容）
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
