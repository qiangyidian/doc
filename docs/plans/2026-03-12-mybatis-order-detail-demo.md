# mybatis-order-detail-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot + MyBatis 项目，完成“订单主表和订单项子表的一对多详情查询”练习模块，让初学者理解 MyBatis 的 `resultMap` 和 `collection` 用法。

**Architecture:** 这个项目的核心链路是 `Controller -> Service -> Mapper -> MySQL`。通过一条联表 SQL 查询订单主表和订单项表，再利用 MyBatis 的 `resultMap` 把平铺结果映射成“一个订单对象 + 多个订单项”的结构。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Web, MyBatis Spring Boot Starter 3.0.4, MySQL 8.0, Docker Compose, Maven, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 MyBatis

订单系统中最常见的一类查询就是：

- 查订单基本信息
- 同时查订单明细项

如果只用最基础的单表查询，就要写很多额外组装代码。  
而 MyBatis 的优势之一，就是它可以通过 `resultMap` 把 SQL 查询结果映射成更复杂的对象结构。

这在生产环境里非常常见，比如：

- 订单详情
- 发票详情
- 采购单详情
- 出库单详情

---

## 二、最终目录结构

```text
mybatis-order-detail-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/orderdetail
│   │   │   ├── OrderDetailApplication.java
│   │   │   ├── common
│   │   │   │   └── Result.java
│   │   │   ├── controller
│   │   │   │   └── OrderController.java
│   │   │   ├── entity
│   │   │   │   ├── OrderInfo.java
│   │   │   │   └── OrderItem.java
│   │   │   ├── mapper
│   │   │   │   └── OrderInfoMapper.java
│   │   │   ├── service
│   │   │   │   ├── OrderService.java
│   │   │   │   └── impl
│   │   │   │       └── OrderServiceImpl.java
│   │   │   └── vo
│   │   │       ├── OrderDetailVO.java
│   │   │       └── OrderItemVO.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── data.sql
│   │       ├── mapper
│   │       │   └── OrderInfoMapper.xml
│   │       └── schema.sql
│   └── test
│       └── java/com/example/orderdetail/OrderDetailApplicationTests.java
```

---

## 三、端口规划

- 应用端口：`8102`
- MySQL：`3314`

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/orderdetail/OrderDetailApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/orderdetail/OrderDetailApplicationTests.java`

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
    <artifactId>mybatis-order-detail-demo</artifactId>
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
    container_name: mybatis-order-detail-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: order_detail_db
    ports:
      - "3314:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - mybatis-order-detail-mysql-data:/var/lib/mysql

volumes:
  mybatis-order-detail-mysql-data:
```

**Step 3: 创建启动类 `OrderDetailApplication.java`**

```java
package com.example.orderdetail;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.orderdetail.mapper")
public class OrderDetailApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderDetailApplication.class, args);
    }
}
```

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8102

spring:
  application:
    name: mybatis-order-detail-demo

  datasource:
    url: jdbc:mysql://localhost:3314/order_detail_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
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
DROP TABLE IF EXISTS t_order_item;
DROP TABLE IF EXISTS t_order_info;

CREATE TABLE t_order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
    status VARCHAR(32) NOT NULL COMMENT '订单状态',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
);

CREATE TABLE t_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    price DECIMAL(10, 2) NOT NULL COMMENT '单价',
    quantity INT NOT NULL COMMENT '数量',
    create_time DATETIME NOT NULL COMMENT '创建时间'
);
```

**Step 6: 创建 `data.sql`**

```sql
INSERT INTO t_order_info (order_no, user_id, total_amount, status, create_time, update_time)
VALUES ('ORD202603120001', 1001, 398.00, 'PAID', NOW(), NOW());

INSERT INTO t_order_item (order_no, product_name, price, quantity, create_time)
VALUES ('ORD202603120001', 'MyBatis实战课', 199.00, 1, NOW()),
       ('ORD202603120001', 'SpringBoot实战课', 199.00, 1, NOW());
```

**Step 7: 创建测试类 `OrderDetailApplicationTests.java`**

```java
package com.example.orderdetail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderDetailApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

---

### Task 2: 创建通用类、实体类、VO、Mapper 接口和 XML

**Files:**
- Create: `src/main/java/com/example/orderdetail/common/Result.java`
- Create: `src/main/java/com/example/orderdetail/entity/OrderInfo.java`
- Create: `src/main/java/com/example/orderdetail/entity/OrderItem.java`
- Create: `src/main/java/com/example/orderdetail/vo/OrderItemVO.java`
- Create: `src/main/java/com/example/orderdetail/vo/OrderDetailVO.java`
- Create: `src/main/java/com/example/orderdetail/mapper/OrderInfoMapper.java`
- Create: `src/main/resources/mapper/OrderInfoMapper.xml`

**Step 1: 创建 `Result.java`**

```java
package com.example.orderdetail.common;

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

**Step 2: 创建实体类**

`OrderInfo.java`

```java
package com.example.orderdetail.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderInfo {

    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

`OrderItem.java`

```java
package com.example.orderdetail.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderItem {

    private Long id;

    private String orderNo;

    private String productName;

    private BigDecimal price;

    private Integer quantity;

    private LocalDateTime createTime;
}
```

**Step 3: 创建 VO**

`OrderItemVO.java`

```java
package com.example.orderdetail.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVO {

    private Long itemId;

    private String productName;

    private BigDecimal price;

    private Integer quantity;
}
```

`OrderDetailVO.java`

```java
package com.example.orderdetail.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情视图对象。
 *
 * 这里不是直接返回数据库实体，而是返回更适合接口展示的结构。
 */
@Data
public class OrderDetailVO {

    private Long orderId;

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    private String status;

    private LocalDateTime createTime;

    private List<OrderItemVO> itemList;
}
```

**Step 4: 创建 Mapper 接口 `OrderInfoMapper.java`**

```java
package com.example.orderdetail.mapper;

import com.example.orderdetail.vo.OrderDetailVO;
import org.apache.ibatis.annotations.Param;

public interface OrderInfoMapper {

    OrderDetailVO selectOrderDetailByOrderNo(@Param("orderNo") String orderNo);
}
```

**Step 5: 创建 XML `OrderInfoMapper.xml`**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.orderdetail.mapper.OrderInfoMapper">

    <!--
        一对多映射的关键：
        1. 主对象用 resultMap 映射
        2. 子对象列表用 collection 映射
        3. 联表查询结果虽然是“多行”，但 MyBatis 可以帮我们组装成“一个订单 + 多个订单项”
    -->
    <resultMap id="orderDetailResultMap" type="com.example.orderdetail.vo.OrderDetailVO">
        <id property="orderId" column="order_id"/>
        <result property="orderNo" column="order_no"/>
        <result property="userId" column="user_id"/>
        <result property="totalAmount" column="total_amount"/>
        <result property="status" column="status"/>
        <result property="createTime" column="order_create_time"/>

        <collection property="itemList" ofType="com.example.orderdetail.vo.OrderItemVO">
            <id property="itemId" column="item_id"/>
            <result property="productName" column="product_name"/>
            <result property="price" column="price"/>
            <result property="quantity" column="quantity"/>
        </collection>
    </resultMap>

    <select id="selectOrderDetailByOrderNo" resultMap="orderDetailResultMap">
        SELECT
            o.id AS order_id,
            o.order_no,
            o.user_id,
            o.total_amount,
            o.status,
            o.create_time AS order_create_time,
            i.id AS item_id,
            i.product_name,
            i.price,
            i.quantity
        FROM t_order_info o
        LEFT JOIN t_order_item i ON o.order_no = i.order_no
        WHERE o.order_no = #{orderNo}
    </select>

</mapper>
```

---

### Task 3: 创建 Service 和 Controller

**Files:**
- Create: `src/main/java/com/example/orderdetail/service/OrderService.java`
- Create: `src/main/java/com/example/orderdetail/service/impl/OrderServiceImpl.java`
- Create: `src/main/java/com/example/orderdetail/controller/OrderController.java`

**Step 1: 创建 `OrderService.java`**

```java
package com.example.orderdetail.service;

import com.example.orderdetail.vo.OrderDetailVO;

public interface OrderService {

    OrderDetailVO getOrderDetail(String orderNo);
}
```

**Step 2: 创建 `OrderServiceImpl.java`**

```java
package com.example.orderdetail.service.impl;

import com.example.orderdetail.mapper.OrderInfoMapper;
import com.example.orderdetail.service.OrderService;
import com.example.orderdetail.vo.OrderDetailVO;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderInfoMapper orderInfoMapper;

    public OrderServiceImpl(OrderInfoMapper orderInfoMapper) {
        this.orderInfoMapper = orderInfoMapper;
    }

    @Override
    public OrderDetailVO getOrderDetail(String orderNo) {
        return orderInfoMapper.selectOrderDetailByOrderNo(orderNo);
    }
}
```

**Step 3: 创建控制器 `OrderController.java`**

```java
package com.example.orderdetail.controller;

import com.example.orderdetail.common.Result;
import com.example.orderdetail.service.OrderService;
import com.example.orderdetail.vo.OrderDetailVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderNo}/detail")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable String orderNo) {
        return Result.success(orderService.getOrderDetail(orderNo));
    }
}
```

---

### Task 4: 启动项目并验证一对多查询链路

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

**Step 3: 查询订单详情**

Run:

```bash
curl "http://localhost:8102/orders/ORD202603120001/detail"
```

Expected:

- 返回一个订单对象
- 其中 `itemList` 里有 2 条订单项

---

### Task 5: 常见错误排查

**问题 1：查出来只有订单，没有订单项**

排查：

- `collection` 的 `property` 是否写成了 `itemList`
- SQL 中子表字段是否起了别名
- `item_id` 是否正确映射到 `OrderItemVO.itemId`

**问题 2：返回了重复的订单主信息**

说明：

- 联表查询天然会返回多行
- MyBatis 正是通过 `resultMap + collection` 来帮你把这些行聚合回一个订单对象

**问题 3：为什么这里用了 VO，而不是直接返回实体类**

原因：

- 实体类更偏数据库结构
- VO 更偏接口展示结构
- 真实项目里这样更清晰

---

## 你做完这个项目后应该掌握什么

1. 什么是一对多查询
2. `resultMap` 和 `collection` 分别是干什么的
3. 为什么联表查询结果能被组装成“一个对象 + 一个列表”
4. MyBatis 为什么适合做复杂查询映射
