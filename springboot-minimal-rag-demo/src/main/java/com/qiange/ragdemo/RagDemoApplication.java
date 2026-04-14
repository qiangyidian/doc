package com.qiange.ragdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目启动入口。
 *
 * 为什么最小版项目也要保留一个独立启动类？
 * 1. 这是 Spring Boot 应用的标准入口
 * 2. 后面我们所有的 Controller、Service、Mapper 都会从这里开始扫描
 * 3. 用户在照着文档实操时，最先运行的就是这个类
 */
@SpringBootApplication
public class RagDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagDemoApplication.class, args);
    }
}