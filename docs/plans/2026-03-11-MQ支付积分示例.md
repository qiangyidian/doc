# mq-payment-points-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot 项目，完成“支付成功后，通过 RabbitMQ 异步发放积分”的练习模块，让初学者理解“主业务成功后，把附加业务异步化”的常见做法。

**Architecture:** 这个项目的核心链路是 `Controller -> PaymentService -> MySQL -> Producer -> RabbitMQ -> Consumer -> PointsService -> MySQL`。支付状态更新是主流程，积分发放是异步从流程，积分日志表承担“是否已经发过积分”的入门级幂等判断。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Maven, Spring Web, Spring AMQP, RabbitMQ, MyBatis-Plus 3.5.15, MySQL 8.0, Docker Compose, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 MQ

支付成功后，经常还会触发很多后续动作：

- 给用户发积分
- 推送支付成功通知
- 记录营销数据
- 通知其他业务系统

这些动作如果都同步写在支付接口里，会导致支付接口越来越慢，也更容易被次要流程拖垮。  
所以常见做法是：

1. 先把支付状态更新成功
2. 再发 MQ 消息
3. 让消费者异步执行“发积分”逻辑

---

## 二、最终目录结构

```text
mq-payment-points-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/paymentpoints
│   │   │   ├── PaymentPointsApplication.java
│   │   │   ├── common
│   │   │   │   ├── Constants.java
│   │   │   │   └── Result.java
│   │   │   ├── config
│   │   │   │   └── RabbitMqConfig.java
│   │   │   ├── controller
│   │   │   │   └── PaymentController.java
│   │   │   ├── dto
│   │   │   │   ├── CreatePaymentRequest.java
│   │   │   │   └── PaymentResponse.java
│   │   │   ├── entity
│   │   │   │   ├── PaymentInfo.java
│   │   │   │   └── UserPointsLog.java
│   │   │   ├── mapper
│   │   │   │   ├── PaymentInfoMapper.java
│   │   │   │   └── UserPointsLogMapper.java
│   │   │   ├── mq
│   │   │   │   ├── PaymentSuccessConsumer.java
│   │   │   │   ├── PaymentSuccessMessage.java
│   │   │   │   └── PaymentSuccessProducer.java
│   │   │   └── service
│   │   │       ├── PaymentService.java
│   │   │       ├── PointsService.java
│   │   │       └── impl
│   │   │           ├── PaymentServiceImpl.java
│   │   │           └── PointsServiceImpl.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── data.sql
│   │       └── schema.sql
│   └── test
│       └── java/com/example/paymentpoints/PaymentPointsApplicationTests.java
```

---

## 三、端口规划

- 应用端口：`8082`
- MySQL：`3308`
- RabbitMQ AMQP：`5674`
- RabbitMQ 控制台：`15674`

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/paymentpoints/PaymentPointsApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/paymentpoints/PaymentPointsApplicationTests.java`

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
    <artifactId>mq-payment-points-demo</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.15</mybatis-plus.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
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
    container_name: payment-points-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: payment_points_db
    ports:
      - "3308:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - payment-points-mysql-data:/var/lib/mysql

  rabbitmq:
    image: rabbitmq:3-management
    container_name: payment-points-rabbitmq
    restart: always
    ports:
      - "5674:5672"
      - "15674:15672"
    volumes:
      - payment-points-rabbitmq-data:/var/lib/rabbitmq

volumes:
  payment-points-mysql-data:
  payment-points-rabbitmq-data:
```

**Step 3: 创建启动类 `PaymentPointsApplication.java`**

```java
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
```

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8082

spring:
  application:
    name: mq-payment-points-demo

  datasource:
    url: jdbc:mysql://localhost:3308/payment_points_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

  rabbitmq:
    host: localhost
    port: 5674
    username: guest
    password: guest
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 1

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

**Step 5: 创建 `schema.sql`**

```sql
DROP TABLE IF EXISTS t_user_points_log;
DROP TABLE IF EXISTS t_payment_info;

CREATE TABLE t_payment_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    pay_amount DECIMAL(10, 2) NOT NULL COMMENT '支付金额',
    pay_status VARCHAR(32) NOT NULL COMMENT '支付状态',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
);

CREATE TABLE t_user_points_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    points INT NOT NULL COMMENT '积分数量',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL COMMENT '创建时间'
);
```

**Step 6: 创建 `data.sql`**

```sql
INSERT INTO t_payment_info (payment_no, user_id, order_no, pay_amount, pay_status, create_time, update_time)
VALUES ('PAY_INIT_001', 2001, 'ORDER_INIT_001', 299.00, 'WAIT_PAY', NOW(), NOW());
```

**Step 7: 创建测试类 `PaymentPointsApplicationTests.java`**

```java
package com.example.paymentpoints;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaymentPointsApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

---

### Task 2: 创建通用类、实体类、Mapper 和 DTO

**Files:**
- Create: `src/main/java/com/example/paymentpoints/common/Constants.java`
- Create: `src/main/java/com/example/paymentpoints/common/Result.java`
- Create: `src/main/java/com/example/paymentpoints/entity/PaymentInfo.java`
- Create: `src/main/java/com/example/paymentpoints/entity/UserPointsLog.java`
- Create: `src/main/java/com/example/paymentpoints/mapper/PaymentInfoMapper.java`
- Create: `src/main/java/com/example/paymentpoints/mapper/UserPointsLogMapper.java`
- Create: `src/main/java/com/example/paymentpoints/dto/CreatePaymentRequest.java`
- Create: `src/main/java/com/example/paymentpoints/dto/PaymentResponse.java`

**Step 1: 创建 `Constants.java`**

```java
package com.example.paymentpoints.common;

public final class Constants {

    private Constants() {
    }

    public static final String PAYMENT_SUCCESS_EXCHANGE = "payment.success.exchange";
    public static final String PAYMENT_SUCCESS_QUEUE = "payment.success.queue";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success.routing";
}
```

**Step 2: 创建 `Result.java`**

```java
package com.example.paymentpoints.common;

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

**Step 3: 创建实体类**

`PaymentInfo.java`

```java
package com.example.paymentpoints.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_payment_info")
public class PaymentInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String paymentNo;

    private Long userId;

    private String orderNo;

    private BigDecimal payAmount;

    private String payStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

`UserPointsLog.java`

```java
package com.example.paymentpoints.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_points_log")
public class UserPointsLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String paymentNo;

    private Integer points;

    private String bizType;

    private String remark;

    private LocalDateTime createTime;
}
```

**Step 4: 创建 Mapper**

`PaymentInfoMapper.java`

```java
package com.example.paymentpoints.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.paymentpoints.entity.PaymentInfo;

public interface PaymentInfoMapper extends BaseMapper<PaymentInfo> {
}
```

`UserPointsLogMapper.java`

```java
package com.example.paymentpoints.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.paymentpoints.entity.UserPointsLog;

public interface UserPointsLogMapper extends BaseMapper<UserPointsLog> {
}
```

**Step 5: 创建 DTO**

`CreatePaymentRequest.java`

```java
package com.example.paymentpoints.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePaymentRequest {

    private Long userId;

    private String orderNo;

    private BigDecimal payAmount;
}
```

`PaymentResponse.java`

```java
package com.example.paymentpoints.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentResponse {

    private Long paymentId;

    private String paymentNo;

    private String payStatus;
}
```

---

### Task 3: 创建 Service 和 Controller

**Files:**
- Create: `src/main/java/com/example/paymentpoints/service/PaymentService.java`
- Create: `src/main/java/com/example/paymentpoints/service/PointsService.java`
- Create: `src/main/java/com/example/paymentpoints/service/impl/PaymentServiceImpl.java`
- Create: `src/main/java/com/example/paymentpoints/service/impl/PointsServiceImpl.java`
- Create: `src/main/java/com/example/paymentpoints/controller/PaymentController.java`

**Step 1: 创建 `PaymentService.java`**

```java
package com.example.paymentpoints.service;

import com.example.paymentpoints.dto.CreatePaymentRequest;
import com.example.paymentpoints.dto.PaymentResponse;
import com.example.paymentpoints.entity.PaymentInfo;
import com.example.paymentpoints.entity.UserPointsLog;

import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request);

    PaymentResponse markPaid(Long paymentId);

    PaymentInfo getPaymentById(Long paymentId);

    List<UserPointsLog> listPointsLogs();
}
```

**Step 2: 创建 `PointsService.java`**

```java
package com.example.paymentpoints.service;

import com.example.paymentpoints.mq.PaymentSuccessMessage;

public interface PointsService {

    void grantPoints(PaymentSuccessMessage message);
}
```

**Step 3: 创建 `PaymentServiceImpl.java`**

```java
package com.example.paymentpoints.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.paymentpoints.dto.CreatePaymentRequest;
import com.example.paymentpoints.dto.PaymentResponse;
import com.example.paymentpoints.entity.PaymentInfo;
import com.example.paymentpoints.entity.UserPointsLog;
import com.example.paymentpoints.mapper.PaymentInfoMapper;
import com.example.paymentpoints.mapper.UserPointsLogMapper;
import com.example.paymentpoints.mq.PaymentSuccessMessage;
import com.example.paymentpoints.mq.PaymentSuccessProducer;
import com.example.paymentpoints.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentInfoMapper paymentInfoMapper;
    private final UserPointsLogMapper userPointsLogMapper;
    private final PaymentSuccessProducer paymentSuccessProducer;

    public PaymentServiceImpl(PaymentInfoMapper paymentInfoMapper,
                              UserPointsLogMapper userPointsLogMapper,
                              PaymentSuccessProducer paymentSuccessProducer) {
        this.paymentInfoMapper = paymentInfoMapper;
        this.userPointsLogMapper = userPointsLogMapper;
        this.paymentSuccessProducer = paymentSuccessProducer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentNo(generatePaymentNo());
        paymentInfo.setUserId(request.getUserId());
        paymentInfo.setOrderNo(request.getOrderNo());
        paymentInfo.setPayAmount(request.getPayAmount());
        paymentInfo.setPayStatus("WAIT_PAY");
        paymentInfo.setCreateTime(LocalDateTime.now());
        paymentInfo.setUpdateTime(LocalDateTime.now());
        paymentInfoMapper.insert(paymentInfo);
        return new PaymentResponse(paymentInfo.getId(), paymentInfo.getPaymentNo(), paymentInfo.getPayStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse markPaid(Long paymentId) {
        PaymentInfo paymentInfo = paymentInfoMapper.selectById(paymentId);
        if (paymentInfo == null) {
            throw new IllegalArgumentException("支付单不存在");
        }

        paymentInfo.setPayStatus("PAID");
        paymentInfo.setUpdateTime(LocalDateTime.now());
        paymentInfoMapper.updateById(paymentInfo);

        PaymentSuccessMessage message = new PaymentSuccessMessage();
        message.setPaymentId(paymentInfo.getId());
        message.setPaymentNo(paymentInfo.getPaymentNo());
        message.setUserId(paymentInfo.getUserId());
        message.setOrderNo(paymentInfo.getOrderNo());
        message.setPayAmount(paymentInfo.getPayAmount());
        message.setPoints(paymentInfo.getPayAmount().intValue());
        message.setMessageTime(LocalDateTime.now());

        // 支付成功后只负责发消息，不等待积分逻辑执行完成。
        paymentSuccessProducer.send(message);

        return new PaymentResponse(paymentInfo.getId(), paymentInfo.getPaymentNo(), paymentInfo.getPayStatus());
    }

    @Override
    public PaymentInfo getPaymentById(Long paymentId) {
        return paymentInfoMapper.selectById(paymentId);
    }

    @Override
    public List<UserPointsLog> listPointsLogs() {
        return userPointsLogMapper.selectList(
                new LambdaQueryWrapper<UserPointsLog>().orderByDesc(UserPointsLog::getId)
        );
    }

    private String generatePaymentNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "PAY" + timePart + randomPart;
    }
}
```

**Step 4: 创建 `PointsServiceImpl.java`**

```java
package com.example.paymentpoints.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.paymentpoints.entity.UserPointsLog;
import com.example.paymentpoints.mapper.UserPointsLogMapper;
import com.example.paymentpoints.mq.PaymentSuccessMessage;
import com.example.paymentpoints.service.PointsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PointsServiceImpl implements PointsService {

    private final UserPointsLogMapper userPointsLogMapper;

    public PointsServiceImpl(UserPointsLogMapper userPointsLogMapper) {
        this.userPointsLogMapper = userPointsLogMapper;
    }

    @Override
    public void grantPoints(PaymentSuccessMessage message) {
        Long count = userPointsLogMapper.selectCount(
                new LambdaQueryWrapper<UserPointsLog>()
                        .eq(UserPointsLog::getPaymentNo, message.getPaymentNo())
        );

        // 这是一个非常入门级的“重复消费保护”做法。
        // 如果这个 paymentNo 已经发过积分，就直接返回。
        if (count != null && count > 0) {
            return;
        }

        UserPointsLog log = new UserPointsLog();
        log.setUserId(message.getUserId());
        log.setPaymentNo(message.getPaymentNo());
        log.setPoints(message.getPoints());
        log.setBizType("PAY_REWARD");
        log.setRemark("支付成功后发放积分");
        log.setCreateTime(LocalDateTime.now());
        userPointsLogMapper.insert(log);
    }
}
```

**Step 5: 创建控制器 `PaymentController.java`**

```java
package com.example.paymentpoints.controller;

import com.example.paymentpoints.common.Result;
import com.example.paymentpoints.dto.CreatePaymentRequest;
import com.example.paymentpoints.dto.PaymentResponse;
import com.example.paymentpoints.entity.PaymentInfo;
import com.example.paymentpoints.entity.UserPointsLog;
import com.example.paymentpoints.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public Result<PaymentResponse> createPayment(@RequestBody CreatePaymentRequest request) {
        return Result.success(paymentService.createPayment(request));
    }

    @PostMapping("/success/{paymentId}")
    public Result<PaymentResponse> markPaid(@PathVariable Long paymentId) {
        return Result.success(paymentService.markPaid(paymentId));
    }

    @GetMapping("/{paymentId}")
    public Result<PaymentInfo> getPayment(@PathVariable Long paymentId) {
        return Result.success(paymentService.getPaymentById(paymentId));
    }

    @GetMapping("/points/logs")
    public Result<List<UserPointsLog>> listPointsLogs() {
        return Result.success(paymentService.listPointsLogs());
    }
}
```

---

### Task 4: 创建 MQ 配置、消息体、生产者和消费者

**Files:**
- Create: `src/main/java/com/example/paymentpoints/config/RabbitMqConfig.java`
- Create: `src/main/java/com/example/paymentpoints/mq/PaymentSuccessMessage.java`
- Create: `src/main/java/com/example/paymentpoints/mq/PaymentSuccessProducer.java`
- Create: `src/main/java/com/example/paymentpoints/mq/PaymentSuccessConsumer.java`

**Step 1: 创建 `RabbitMqConfig.java`**

```java
package com.example.paymentpoints.config;

import com.example.paymentpoints.common.Constants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange paymentSuccessExchange() {
        return new DirectExchange(Constants.PAYMENT_SUCCESS_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return new Queue(Constants.PAYMENT_SUCCESS_QUEUE, true);
    }

    @Bean
    public Binding paymentSuccessBinding(Queue paymentSuccessQueue, DirectExchange paymentSuccessExchange) {
        return BindingBuilder.bind(paymentSuccessQueue)
                .to(paymentSuccessExchange)
                .with(Constants.PAYMENT_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

**Step 2: 创建 `PaymentSuccessMessage.java`**

```java
package com.example.paymentpoints.mq;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentSuccessMessage {

    private Long paymentId;

    private String paymentNo;

    private Long userId;

    private String orderNo;

    private BigDecimal payAmount;

    private Integer points;

    private LocalDateTime messageTime;
}
```

**Step 3: 创建 `PaymentSuccessProducer.java`**

```java
package com.example.paymentpoints.mq;

import com.example.paymentpoints.common.Constants;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentSuccessProducer {

    private final RabbitTemplate rabbitTemplate;

    public PaymentSuccessProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(PaymentSuccessMessage message) {
        rabbitTemplate.convertAndSend(
                Constants.PAYMENT_SUCCESS_EXCHANGE,
                Constants.PAYMENT_SUCCESS_ROUTING_KEY,
                message
        );
    }
}
```

**Step 4: 创建 `PaymentSuccessConsumer.java`**

```java
package com.example.paymentpoints.mq;

import com.example.paymentpoints.common.Constants;
import com.example.paymentpoints.service.PointsService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PaymentSuccessConsumer {

    private final PointsService pointsService;

    public PaymentSuccessConsumer(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @RabbitListener(queues = Constants.PAYMENT_SUCCESS_QUEUE)
    public void receive(PaymentSuccessMessage message, Message amqpMessage, Channel channel) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();

        try {
            pointsService.grantPoints(message);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            channel.basicNack(deliveryTag, false, true);
            throw ex;
        }
    }
}
```

---

### Task 5: 启动项目并验证完整链路

**Step 1: 启动中间件**

Run:

```bash
docker compose up -d
```

**Step 2: 启动应用**

Run:

```bash
mvn spring-boot:run
```

**Step 3: 创建支付单**

Run:

```bash
curl -X POST "http://localhost:8082/payments" ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\":2001,\"orderNo\":\"ORDER202603110001\",\"payAmount\":299.00}"
```

**Step 4: 标记支付成功**

Run:

```bash
curl -X POST "http://localhost:8082/payments/success/1"
```

Expected:

- 返回 `payStatus=PAID`
- 不需要等待积分逻辑执行完成

**Step 5: 查询积分日志**

Run:

```bash
curl "http://localhost:8082/payments/points/logs"
```

Expected:

- 能看到一条积分日志
- `points` 默认按照支付金额整数部分演示计算

**Step 6: 打开 RabbitMQ 控制台**

地址：

```text
http://localhost:15674
```

---

### Task 6: 常见错误排查

**问题 1：支付状态更新成功了，但没有积分日志**

排查：

- `markPaid` 方法里是否真的调用了 `paymentSuccessProducer.send`
- 队列名是否为 `payment.success.queue`
- RabbitMQ 端口是否为 `5674`

**问题 2：重复调用支付成功接口后，积分重复增加**

说明：

- 这个练习项目已经做了最简单的防重复逻辑
- 逻辑位置在 `PointsServiceImpl#grantPoints`
- 它通过 `paymentNo` 查询积分日志表，存在则直接返回

注意：

- 这只是“入门级保护”
- 真正生产里通常还会配合唯一索引、幂等表、业务状态机等更严格方案

---

## 你做完这个项目后应该掌握什么

1. 为什么支付成功后的积分发放适合异步处理
2. 主业务和附加业务为什么不能强耦合
3. MQ 消费侧为什么要考虑重复消费
4. 手动 ACK 和重复消费保护为什么经常一起出现
