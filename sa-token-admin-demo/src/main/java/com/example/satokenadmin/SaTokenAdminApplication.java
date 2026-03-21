package com.example.satokenadmin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication 是 Spring Boot 项目的总开关。
@SpringBootApplication
// 扫描 MyBatis 的 Mapper 接口。
@MapperScan("com.example.satokenadmin.mapper")
public class SaTokenAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaTokenAdminApplication.class, args);
    }
}