# mybatis-product-search-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot + MyBatis 项目，完成“商品后台动态条件搜索和分页查询”的练习模块，让初学者理解 MyBatis 动态 SQL 的典型使用方式。

**Architecture:** 这个项目的核心链路是 `Controller -> Service -> Mapper -> MySQL`。前端传入一组可选查询条件，Service 负责把页码转换成偏移量，Mapper XML 使用 `if + where + limit` 动态拼接 SQL，实现后台管理列表最常见的搜索页面。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Web, MyBatis Spring Boot Starter 3.0.4, MySQL 8.0, Docker Compose, Maven, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 MyBatis

后台系统最常见的页面之一就是列表搜索页，比如：

- 商品列表
- 用户列表
- 订单列表
- 活动列表

这些页面通常会有多个查询条件：

- 关键字
- 状态
- 分类
- 时间区间
- 价格区间

而且不是每次都会传全部条件。  
这正是 MyBatis 动态 SQL 的经典场景。

---

## 二、最终目录结构

```text
mybatis-product-search-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/productsearch
│   │   │   ├── ProductSearchApplication.java
│   │   │   ├── common
│   │   │   │   ├── PageResponse.java
│   │   │   │   └── Result.java
│   │   │   ├── controller
│   │   │   │   └── ProductController.java
│   │   │   ├── dto
│   │   │   │   └── ProductSearchRequest.java
│   │   │   ├── entity
│   │   │   │   └── ProductInfo.java
│   │   │   ├── mapper
│   │   │   │   └── ProductInfoMapper.java
│   │   │   └── service
│   │   │       ├── ProductService.java
│   │   │       └── impl
│   │   │           └── ProductServiceImpl.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── data.sql
│   │       ├── mapper
│   │       │   └── ProductInfoMapper.xml
│   │       └── schema.sql
│   └── test
│       └── java/com/example/productsearch/ProductSearchApplicationTests.java
```

---

## 三、端口规划

- 应用端口：`8103`
- MySQL：`3315`

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/productsearch/ProductSearchApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/productsearch/ProductSearchApplicationTests.java`

**Step 1: 创建 `pom.xml`**

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>mybatis-product-search-demo</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
        <mybatis.version>3.0.4</mybatis.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>${mybatis.version}</version>
        </dependency>

        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**Step 2: 创建 `docker-compose.yml`**

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: mybatis-product-search-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: product_search_db
    ports:
      - "3315:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - mybatis-product-search-mysql-data:/var/lib/mysql

volumes:
  mybatis-product-search-mysql-data:
```

**Step 3: 创建启动类 `ProductSearchApplication.java`**

```java
package com.example.productsearch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.productsearch.mapper")
public class ProductSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductSearchApplication.class, args);
    }
}
```

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8103

spring:
  application:
    name: mybatis-product-search-demo

  datasource:
    url: jdbc:mysql://localhost:3315/product_search_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

mybatis:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

**Step 5: 创建 `schema.sql`**

```sql
DROP TABLE IF EXISTS t_product_info;

CREATE TABLE t_product_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    category_name VARCHAR(64) NOT NULL COMMENT '分类名称',
    price DECIMAL(10, 2) NOT NULL COMMENT '价格',
    status VARCHAR(32) NOT NULL COMMENT '状态',
    stock_count INT NOT NULL COMMENT '库存',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
);
```

**Step 6: 创建 `data.sql`**

```sql
INSERT INTO t_product_info (product_name, category_name, price, status, stock_count, create_time, update_time)
VALUES ('MyBatis入门课', '后端课程', 99.00, 'ENABLE', 100, NOW(), NOW()),
       ('SpringBoot高级课', '后端课程', 199.00, 'ENABLE', 88, NOW(), NOW()),
       ('Redis实战课', '中间件课程', 159.00, 'DISABLE', 66, NOW(), NOW()),
       ('Docker部署课', '运维课程', 129.00, 'ENABLE', 50, NOW(), NOW());
```

**Step 7: 创建测试类 `ProductSearchApplicationTests.java`**

```java
package com.example.productsearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductSearchApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

---

### Task 2: 创建通用类、实体类、查询 DTO、Mapper 接口和 XML

**Files:**
- Create: `src/main/java/com/example/productsearch/common/Result.java`
- Create: `src/main/java/com/example/productsearch/common/PageResponse.java`
- Create: `src/main/java/com/example/productsearch/entity/ProductInfo.java`
- Create: `src/main/java/com/example/productsearch/dto/ProductSearchRequest.java`
- Create: `src/main/java/com/example/productsearch/mapper/ProductInfoMapper.java`
- Create: `src/main/resources/mapper/ProductInfoMapper.xml`

**Step 1: 创建 `Result.java`**

```java
package com.example.productsearch.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }
}
```

**Step 2: 创建分页响应对象 `PageResponse.java`**

```java
package com.example.productsearch.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResponse<T> {

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    private List<T> records;
}
```

**Step 3: 创建实体类 `ProductInfo.java`**

```java
package com.example.productsearch.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductInfo {

    private Long id;

    private String productName;

    private String categoryName;

    private BigDecimal price;

    private String status;

    private Integer stockCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

**Step 4: 创建查询对象 `ProductSearchRequest.java`**

```java
package com.example.productsearch.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSearchRequest {

    private String keyword;

    private String categoryName;

    private String status;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
```

**Step 5: 创建 Mapper 接口 `ProductInfoMapper.java`**

```java
package com.example.productsearch.mapper;

import com.example.productsearch.dto.ProductSearchRequest;
import com.example.productsearch.entity.ProductInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductInfoMapper {

    Long countByCondition(@Param("request") ProductSearchRequest request);

    List<ProductInfo> selectByCondition(@Param("request") ProductSearchRequest request,
                                        @Param("offset") Integer offset,
                                        @Param("pageSize") Integer pageSize);
}
```

**Step 6: 创建 XML `ProductInfoMapper.xml`**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.productsearch.mapper.ProductInfoMapper">

    <!--
        动态 SQL 的关键在于：
        1. 只在有条件时才拼接 where 子句
        2. 避免写很多 if else 的 Java 代码
        3. 把搜索逻辑留在 SQL 层更直观
    -->
    <sql id="baseWhere">
        <where>
            <if test="request.keyword != null and request.keyword != ''">
                AND product_name LIKE CONCAT('%', #{request.keyword}, '%')
            </if>

            <if test="request.categoryName != null and request.categoryName != ''">
                AND category_name = #{request.categoryName}
            </if>

            <if test="request.status != null and request.status != ''">
                AND status = #{request.status}
            </if>

            <if test="request.minPrice != null">
                AND price &gt;= #{request.minPrice}
            </if>

            <if test="request.maxPrice != null">
                AND price &lt;= #{request.maxPrice}
            </if>
        </where>
    </sql>

    <select id="countByCondition" resultType="java.lang.Long">
        SELECT COUNT(1)
        FROM t_product_info
        <include refid="baseWhere"/>
    </select>

    <select id="selectByCondition" resultType="com.example.productsearch.entity.ProductInfo">
        SELECT
            id,
            product_name,
            category_name,
            price,
            status,
            stock_count,
            create_time,
            update_time
        FROM t_product_info
        <include refid="baseWhere"/>
        ORDER BY id DESC
        LIMIT #{offset}, #{pageSize}
    </select>

</mapper>
```

---

### Task 3: 创建 Service 和 Controller

**Files:**
- Create: `src/main/java/com/example/productsearch/service/ProductService.java`
- Create: `src/main/java/com/example/productsearch/service/impl/ProductServiceImpl.java`
- Create: `src/main/java/com/example/productsearch/controller/ProductController.java`

**Step 1: 创建 `ProductService.java`**

```java
package com.example.productsearch.service;

import com.example.productsearch.common.PageResponse;
import com.example.productsearch.dto.ProductSearchRequest;
import com.example.productsearch.entity.ProductInfo;

public interface ProductService {

    PageResponse<ProductInfo> search(ProductSearchRequest request);
}
```

**Step 2: 创建 `ProductServiceImpl.java`**

```java
package com.example.productsearch.service.impl;

import com.example.productsearch.common.PageResponse;
import com.example.productsearch.dto.ProductSearchRequest;
import com.example.productsearch.entity.ProductInfo;
import com.example.productsearch.mapper.ProductInfoMapper;
import com.example.productsearch.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductInfoMapper productInfoMapper;

    public ProductServiceImpl(ProductInfoMapper productInfoMapper) {
        this.productInfoMapper = productInfoMapper;
    }

    @Override
    public PageResponse<ProductInfo> search(ProductSearchRequest request) {
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        Long total = productInfoMapper.countByCondition(request);
        List<ProductInfo> records = productInfoMapper.selectByCondition(request, offset, pageSize);

        return new PageResponse<>(total, pageNum, pageSize, records);
    }
}
```

**Step 3: 创建控制器 `ProductController.java`**

```java
package com.example.productsearch.controller;

import com.example.productsearch.common.PageResponse;
import com.example.productsearch.common.Result;
import com.example.productsearch.dto.ProductSearchRequest;
import com.example.productsearch.entity.ProductInfo;
import com.example.productsearch.service.ProductService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/search")
    public Result<PageResponse<ProductInfo>> search(@RequestBody ProductSearchRequest request) {
        return Result.success(productService.search(request));
    }
}
```

---

### Task 4: 启动项目并验证动态 SQL 搜索链路

**Step 1: 启动 MySQL**

Run:

```bash
docker compose up -d
```

**Step 2: 启动应用**

Run:

```bash
mvn spring-boot:run
```

**Step 3: 查询全部商品**

Run:

```bash
curl -X POST "http://localhost:8103/products/search" ^
  -H "Content-Type: application/json" ^
  -d "{\"pageNum\":1,\"pageSize\":10}"
```

**Step 4: 按关键字查询**

Run:

```bash
curl -X POST "http://localhost:8103/products/search" ^
  -H "Content-Type: application/json" ^
  -d "{\"keyword\":\"MyBatis\",\"pageNum\":1,\"pageSize\":10}"
```

**Step 5: 按分类和状态查询**

Run:

```bash
curl -X POST "http://localhost:8103/products/search" ^
  -H "Content-Type: application/json" ^
  -d "{\"categoryName\":\"后端课程\",\"status\":\"ENABLE\",\"pageNum\":1,\"pageSize\":10}"
```

**Step 6: 按价格区间查询**

Run:

```bash
curl -X POST "http://localhost:8103/products/search" ^
  -H "Content-Type: application/json" ^
  -d "{\"minPrice\":100,\"maxPrice\":200,\"pageNum\":1,\"pageSize\":10}"
```

---

### Task 5: 常见错误排查

**问题 1：SQL 拼出来有多余的 `AND`**

原因：

- 你没有用 `<where>`
- `<where>` 会自动帮你处理开头多余的 `AND`

**问题 2：分页参数不生效**

排查：

- `offset` 是否按 `(pageNum - 1) * pageSize` 计算
- XML 中是否写了 `LIMIT #{offset}, #{pageSize}`

**问题 3：为什么要先查 total，再查 list**

原因：

- 前端分页通常既要当前页数据，也要总条数
- 所以后台常常会分成两条 SQL：一条 `count`，一条 `list`

---

## 你做完这个项目后应该掌握什么

1. MyBatis 动态 SQL 适合解决什么问题
2. `<if>` 和 `<where>` 各自负责什么
3. 后台分页为什么通常是“count + list”两条 SQL
4. 为什么 MyBatis 特别适合做管理后台搜索页面
