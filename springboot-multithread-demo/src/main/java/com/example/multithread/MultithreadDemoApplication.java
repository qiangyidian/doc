package com.example.multithread;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// @SpringBootApplication 是 Spring Boot 的启动入口注解。
@SpringBootApplication
// 开启 @Async 异步能力。
@EnableAsync
// 开启 @Scheduled 定时任务能力。
@EnableScheduling
public class MultithreadDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultithreadDemoApplication.class, args);
    }
}