package com.example.paymentpoints;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.paymentpoints.mapper")
public class PaymentPointsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentPointsApplication.class, args);
    }
}