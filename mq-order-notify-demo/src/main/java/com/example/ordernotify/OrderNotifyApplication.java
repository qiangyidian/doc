package com.example.ordernotify;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目启动类。
 *
 * @MapperScan 的作用是扫描 mapper 接口，
 * 这样 Spring 才知道要为这些接口生成代理对象。
 */
@SpringBootApplication
@MapperScan("com.example.ordernotify.mapper")
public class OrderNotifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderNotifyApplication.class, args);
    }
}