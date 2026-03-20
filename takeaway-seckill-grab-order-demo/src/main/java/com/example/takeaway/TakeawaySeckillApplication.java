package com.example.takeaway;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @SpringBootApplication 是 Spring Boot 项目的总开关。
// 它会帮你做自动配置、组件扫描、配置类识别。
@SpringBootApplication
// @MapperScan 用来扫描 MyBatis Mapper 接口。
@MapperScan("com.example.takeaway.mapper")
// 开启定时任务，这样凌晨预热菜单的任务才能运行。
@EnableScheduling
public class TakeawaySeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(TakeawaySeckillApplication.class, args);
    }
}