package com.example.paymentpoints.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data

@TableName("t_payment_info")
public class PaymentInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String paymentNo;

    private Long userId;

    private String orderNo;

    private BigDecimal payAmount;

    private String payStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}