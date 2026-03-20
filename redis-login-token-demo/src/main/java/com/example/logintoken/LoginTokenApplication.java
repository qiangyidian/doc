package com.example.logintoken;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.logintoken.mapper")
public class LoginTokenApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoginTokenApplication.class, args);
    }
}