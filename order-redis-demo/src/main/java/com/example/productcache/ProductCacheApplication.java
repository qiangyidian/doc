package com.example.productcache;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.productcache.mapper")
public class ProductCacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductCacheApplication.class, args);
    }
}