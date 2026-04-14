package com.example.paymentpoints.common;

public final class Constants {

    private Constants() {
    }

    // ==================== Direct 模式（已有） ====================
    // Direct Exchange：精确匹配 routing key，一对一路由
    public static final String PAYMENT_SUCCESS_EXCHANGE = "payment.success.exchange";
    public static final String PAYMENT_SUCCESS_QUEUE = "payment.success.queue";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success.routing";

    // ==================== Fanout 模式（新增） ====================
    // Fanout Exchange：广播模式，忽略 routing key，消息分发到所有绑定的队列
    // 场景：支付成功后，同时通知短信服务、邮件服务、数据统计服务
    public static final String PAYMENT_FANOUT_EXCHANGE = "payment.fanout.exchange";
    // 三个队列分别对应三个下游服务
    public static final String SMS_NOTIFY_QUEUE = "sms.notify.queue";         // 短信通知队列
    public static final String EMAIL_NOTIFY_QUEUE = "email.notify.queue";     // 邮件通知队列
    public static final String STATISTICS_QUEUE = "statistics.queue";          // 数据统计队列

    // ==================== Topic 模式（新增） ====================
    // Topic Exchange：通配符匹配 routing key，支持 * (匹配一个词) 和 # (匹配零或多个词)
    // 场景：订单事件按类型和等级路由到不同消费者
    //   routing key 格式：order.<level>.<event>
    //   例如：order.vip.created, order.normal.paid, order.vip.cancelled
    public static final String ORDER_TOPIC_EXCHANGE = "order.topic.exchange";
    public static final String VIP_ORDER_QUEUE = "vip.order.queue";               // 匹配 order.vip.*
    public static final String NORMAL_ORDER_QUEUE = "normal.order.queue";         // 匹配 order.normal.*
    public static final String ALL_ORDER_CREATED_QUEUE = "all.order.created.queue"; // 匹配 order.*.created
    public static final String VIP_ORDER_ROUTING_KEY = "order.vip.*";
    public static final String NORMAL_ORDER_ROUTING_KEY = "order.normal.*";
    public static final String ALL_ORDER_CREATED_ROUTING_KEY = "order.*.created";

    // ==================== Headers 模式（新增） ====================
    // Headers Exchange：不依赖 routing key，而是根据消息头（headers）中的键值对匹配
    // 场景：根据消息的优先级和业务类型路由
    //   例如：priority=high 的消息路由到紧急处理队列，type=refund 路由到退款队列
    public static final String PAYMENT_HEADERS_EXCHANGE = "payment.headers.exchange";
    public static final String HIGH_PRIORITY_QUEUE = "high.priority.queue";   // 高优先级处理队列
    public static final String REFUND_QUEUE = "refund.queue";                // 退款处理队列
}