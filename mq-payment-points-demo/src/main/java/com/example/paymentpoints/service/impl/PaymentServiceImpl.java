package com.example.paymentpoints.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.paymentpoints.dto.CreatePaymentRequest;
import com.example.paymentpoints.dto.PaymentResponse;
import com.example.paymentpoints.entity.PaymentInfo;
import com.example.paymentpoints.entity.UserPointsLog;
import com.example.paymentpoints.mapper.PaymentInfoMapper;
import com.example.paymentpoints.mapper.UserPointsLogMapper;
import com.example.paymentpoints.mq.*;
import com.example.paymentpoints.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentInfoMapper paymentInfoMapper;
    private final UserPointsLogMapper userPointsLogMapper;

    // ===== 四种 Exchange 模式的 Producer =====
    private final PaymentSuccessProducer paymentSuccessProducer;   // Direct 模式：积分发放
    private final PaymentNotifyProducer paymentNotifyProducer;     // Fanout 模式：广播通知
    private final OrderEventProducer orderEventProducer;           // Topic 模式：订单事件路由
    private final PaymentActionProducer paymentActionProducer;     // Headers 模式：头部匹配路由

    public PaymentServiceImpl(PaymentInfoMapper paymentInfoMapper,
                              UserPointsLogMapper userPointsLogMapper,
                              PaymentSuccessProducer paymentSuccessProducer,
                              PaymentNotifyProducer paymentNotifyProducer,
                              OrderEventProducer orderEventProducer,
                              PaymentActionProducer paymentActionProducer) {
        this.paymentInfoMapper = paymentInfoMapper;
        this.userPointsLogMapper = userPointsLogMapper;
        this.paymentSuccessProducer = paymentSuccessProducer;
        this.paymentNotifyProducer = paymentNotifyProducer;
        this.orderEventProducer = orderEventProducer;
        this.paymentActionProducer = paymentActionProducer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentNo(generatePaymentNo());
        paymentInfo.setUserId(request.getUserId());
        paymentInfo.setOrderNo(request.getOrderNo());
        paymentInfo.setPayAmount(request.getPayAmount());
        paymentInfo.setPayStatus("WAIT_PAY");
        paymentInfo.setCreateTime(LocalDateTime.now());
        paymentInfo.setUpdateTime(LocalDateTime.now());
        paymentInfoMapper.insert(paymentInfo);
        return new PaymentResponse(paymentInfo.getId(), paymentInfo.getPaymentNo(), paymentInfo.getPayStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse markPaid(Long paymentId) {
        PaymentInfo paymentInfo = paymentInfoMapper.selectById(paymentId);
        if (paymentInfo == null) {
            throw new IllegalArgumentException("支付单不存在");
        }

        paymentInfo.setPayStatus("PAID");
        paymentInfo.setUpdateTime(LocalDateTime.now());
        paymentInfoMapper.updateById(paymentInfo);

        PaymentSuccessMessage message = new PaymentSuccessMessage();
        message.setPaymentId(paymentInfo.getId());
        message.setPaymentNo(paymentInfo.getPaymentNo());
        message.setUserId(paymentInfo.getUserId());
        message.setOrderNo(paymentInfo.getOrderNo());
        message.setPayAmount(paymentInfo.getPayAmount());
        message.setPoints(paymentInfo.getPayAmount().intValue());
        message.setMessageTime(LocalDateTime.now());

        // ==================== Direct 模式：积分发放 ====================
        // 精确匹配 routing key，消息只会路由到 payment.success.queue（积分服务）
        paymentSuccessProducer.send(message);

        // ==================== Fanout 模式：广播通知 ====================
        // 支付成功后同时通知短信、邮件、统计三个服务
        // 所有绑定到 Fanout Exchange 的队列都会收到消息副本
        PaymentNotifyMessage notifyMessage = new PaymentNotifyMessage();
        notifyMessage.setPaymentId(paymentInfo.getId());
        notifyMessage.setPaymentNo(paymentInfo.getPaymentNo());
        notifyMessage.setUserId(paymentInfo.getUserId());
        notifyMessage.setOrderNo(paymentInfo.getOrderNo());
        notifyMessage.setPayAmount(paymentInfo.getPayAmount());
        notifyMessage.setPhone("138****8888");   // 模拟手机号
        notifyMessage.setEmail("user@example.com"); // 模拟邮箱
        paymentNotifyProducer.send(notifyMessage);

        // ==================== Topic 模式：订单事件路由 ====================
        // 支付成功后发送 order.<level>.paid 事件
        // 假设所有支付都是 VIP 等级，routing key = "order.vip.paid"
        // 该消息会匹配 order.vip.* 绑定，路由到 VIP 订单队列
        OrderEventMessage orderEvent = new OrderEventMessage();
        orderEvent.setOrderNo(paymentInfo.getOrderNo());
        orderEvent.setUserId(paymentInfo.getUserId());
        orderEvent.setLevel("vip");
        orderEvent.setEvent("paid");
        orderEvent.setAmount(paymentInfo.getPayAmount());
        orderEvent.setDescription("订单支付成功");
        orderEventProducer.send(orderEvent);

        // ==================== Headers 模式：头部匹配路由 ====================
        // 支付成功后发送操作消息，通过消息头指定优先级和业务类型
        // priority=high, type=payment → 同时匹配高优先级队列（whereAll）
        PaymentActionMessage actionMessage = new PaymentActionMessage();
        actionMessage.setPaymentNo(paymentInfo.getPaymentNo());
        actionMessage.setUserId(paymentInfo.getUserId());
        actionMessage.setOrderNo(paymentInfo.getOrderNo());
        actionMessage.setAmount(paymentInfo.getPayAmount());
        actionMessage.setDescription("支付成功操作");
        paymentActionProducer.send(actionMessage, "high", "payment");

        return new PaymentResponse(paymentInfo.getId(), paymentInfo.getPaymentNo(), paymentInfo.getPayStatus());
    }

    @Override
    public PaymentInfo getPaymentById(Long paymentId) {
        return paymentInfoMapper.selectById(paymentId);
    }

    @Override
    public List<UserPointsLog> listPointsLogs() {
        return userPointsLogMapper.selectList(
                new LambdaQueryWrapper<UserPointsLog>().orderByDesc(UserPointsLog::getId)
        );
    }

    private String generatePaymentNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "PAY" + timePart + randomPart;
    }
}