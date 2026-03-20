package com.example.paymentpoints.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.paymentpoints.entity.UserPointsLog;
import com.example.paymentpoints.mapper.UserPointsLogMapper;
import com.example.paymentpoints.mq.PaymentSuccessMessage;
import com.example.paymentpoints.service.PointsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PointsServiceImpl implements PointsService {

    private final UserPointsLogMapper userPointsLogMapper;

    public PointsServiceImpl(UserPointsLogMapper userPointsLogMapper) {
        this.userPointsLogMapper = userPointsLogMapper;
    }

    @Override
    public void grantPoints(PaymentSuccessMessage message) {
        Long count = userPointsLogMapper.selectCount(
                new LambdaQueryWrapper<UserPointsLog>()
                        .eq(UserPointsLog::getPaymentNo, message.getPaymentNo())
        );

        // 这是一个非常入门级的“重复消费保护”做法。
        // 如果这个 paymentNo 已经发过积分，就直接返回。
        if (count != null && count > 0) {
            return;
        }

        UserPointsLog log = new UserPointsLog();
        log.setUserId(message.getUserId());
        log.setPaymentNo(message.getPaymentNo());
        log.setPoints(message.getPoints());
        log.setBizType("PAY_REWARD");
        log.setRemark("支付成功后发放积分");
        log.setCreateTime(LocalDateTime.now());
        userPointsLogMapper.insert(log);
    }
}