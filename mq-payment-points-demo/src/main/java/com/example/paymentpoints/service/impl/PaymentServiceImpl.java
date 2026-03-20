package com.example.paymentpoints.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.paymentpoints.dto.CreatePaymentRequest;
import com.example.paymentpoints.dto.PaymentResponse;
import com.example.paymentpoints.entity.PaymentInfo;
import com.example.paymentpoints.entity.UserPointsLog;
import com.example.paymentpoints.mapper.PaymentInfoMapper;
import com.example.paymentpoints.mapper.UserPointsLogMapper;
import com.example.paymentpoints.mq.PaymentSuccessMessage;
import com.example.paymentpoints.mq.PaymentSuccessProducer;
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
    private final PaymentSuccessProducer paymentSuccessProducer;

    public PaymentServiceImpl(PaymentInfoMapper paymentInfoMapper,
                              UserPointsLogMapper userPointsLogMapper,
                              PaymentSuccessProducer paymentSuccessProducer) {
        this.paymentInfoMapper = paymentInfoMapper;
        this.userPointsLogMapper = userPointsLogMapper;
        this.paymentSuccessProducer = paymentSuccessProducer;
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

        // 支付成功后只负责发消息，不等待积分逻辑执行完成。
        paymentSuccessProducer.send(message);

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