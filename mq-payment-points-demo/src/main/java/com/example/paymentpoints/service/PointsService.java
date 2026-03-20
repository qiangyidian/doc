package com.example.paymentpoints.service;

import com.example.paymentpoints.mq.PaymentSuccessMessage;

public interface PointsService {

    void grantPoints(PaymentSuccessMessage message);
}