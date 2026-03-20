# mq-order-notify-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot 项目，完成“下单成功后，通过 RabbitMQ 异步发送通知”的练习模块，让初学者可以照着文档一步一步创建文件、粘贴代码并跑通流程。

**Architecture:** 这个项目只有一条核心业务链路：`Controller -> Service -> MySQL -> Producer -> RabbitMQ -> Consumer -> NotifyService -> MySQL`。订单写库是主流程，通知发送是异步从流程，消费者采用手动确认模式，帮助初学者理解消息确认的基本思想。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Maven, Spring Web, Spring AMQP, RabbitMQ, MyBatis-Plus 3.5.15, MySQL 8.0, Docker Compose, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 MQ

在生产环境里，“创建订单”往往是主流程，“发送短信、邮件、站内信”往往是后续动作。

如果把通知逻辑直接写在下单接口里，会有两个明显问题：

1. 通知接口慢，会拖慢下单接口响应时间
2. 通知接口失败，容易影响主业务结果

所以常见做法是：

1. 先把订单保存成功
2. 再发一条 MQ 消息
3. 由消费者异步处理通知

---

## 二、最终目录结构

```text
mq-order-notify-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com/example/ordernotify
│   │   │       ├── OrderNotifyApplication.java
│   │   │       ├── common
│   │   │       │   ├── Constants.java
│   │   │       │   └── Result.java
│   │   │       ├── config
│   │   │       │   └── RabbitMqConfig.java
│   │   │       ├── controller
│   │   │       │   └── OrderController.java
│   │   │       ├── dto
│   │   │       │   ├── CreateOrderRequest.java
│   │   │       │   └── OrderResponse.java
│   │   │       ├── entity
│   │   │       │   ├── OrderInfo.java
│   │   │       │   └── OrderNotifyLog.java
│   │   │       ├── mapper
│   │   │       │   ├── OrderInfoMapper.java
│   │   │       │   └── OrderNotifyLogMapper.java
│   │   │       ├── mq
│   │   │       │   ├── OrderNotifyConsumer.java
│   │   │       │   ├── OrderNotifyMessage.java
│   │   │       │   └── OrderNotifyProducer.java
│   │   │       └── service
│   │   │           ├── NotifyService.java
│   │   │           ├── OrderService.java
│   │   │           └── impl
│   │   │               ├── NotifyServiceImpl.java
│   │   │               └── OrderServiceImpl.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── data.sql
│   │       └── schema.sql
│   └── test
│       └── java/com/example/ordernotify/OrderNotifyApplicationTests.java
```

---

## 三、端口规划

- 应用端口：`8081`
- MySQL：`3307`
- RabbitMQ AMQP：`5673`
- RabbitMQ 控制台：`15673`

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/ordernotify/OrderNotifyApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/ordernotify/OrderNotifyApplicationTests.java`

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
    <artifactId>mq-order-notify-demo</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.15</mybatis-plus.version>
    </properties>

    <dependencies>
        <!-- 提供 REST 接口能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 提供 RabbitMQ 生产者和消费者能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <!-- 简化数据库访问 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL 驱动 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- 减少样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖 -->
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
    container_name: order-notify-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: order_notify_db
    ports:
      - "3307:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - order-notify-mysql-data:/var/lib/mysql

  rabbitmq:
    image: rabbitmq:3-management
    container_name: order-notify-rabbitmq
    restart: always
    ports:
      - "5673:5672"
      - "15673:15672"
    volumes:
      - order-notify-rabbitmq-data:/var/lib/rabbitmq

volumes:
  order-notify-mysql-data:
  order-notify-rabbitmq-data:
```

**Step 3: 创建启动类 `OrderNotifyApplication.java`**

```java
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
```

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8081

spring:
  application:
    name: mq-order-notify-demo

  datasource:
    url: jdbc:mysql://localhost:3307/order_notify_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
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
    port: 5673
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
DROP TABLE IF EXISTS t_order_notify_log;
DROP TABLE IF EXISTS t_order_info;

CREATE TABLE t_order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    amount DECIMAL(10, 2) NOT NULL COMMENT '订单金额',
    status VARCHAR(32) NOT NULL COMMENT '订单状态',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
);

CREATE TABLE t_order_notify_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    notify_type VARCHAR(32) NOT NULL COMMENT '通知类型',
    notify_status VARCHAR(32) NOT NULL COMMENT '通知状态',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL COMMENT '创建时间'
);
```

**Step 6: 创建 `data.sql`**

```sql
INSERT INTO t_order_info (order_no, user_id, product_name, amount, status, create_time, update_time)
VALUES ('ORD_INIT_001', 1001, '初始化订单', 99.00, 'CREATED', NOW(), NOW());
```

**Step 7: 创建测试类 `OrderNotifyApplicationTests.java`**

```java
package com.example.ordernotify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderNotifyApplicationTests {

    @Test
    void contextLoads() {
        // 只验证 Spring 容器是否能正常启动。
    }
}
```

**Step 8: 启动中间件**

Run:

```bash
docker compose up -d
```

Expected:

- `http://localhost:15673` 可以打开 RabbitMQ 控制台
- `mvn test` 可以跑通 `contextLoads`

---

### Task 2: 创建通用类、实体类、Mapper 和 DTO

**Files:**
- Create: `src/main/java/com/example/ordernotify/common/Constants.java`
- Create: `src/main/java/com/example/ordernotify/common/Result.java`
- Create: `src/main/java/com/example/ordernotify/entity/OrderInfo.java`
- Create: `src/main/java/com/example/ordernotify/entity/OrderNotifyLog.java`
- Create: `src/main/java/com/example/ordernotify/mapper/OrderInfoMapper.java`
- Create: `src/main/java/com/example/ordernotify/mapper/OrderNotifyLogMapper.java`
- Create: `src/main/java/com/example/ordernotify/dto/CreateOrderRequest.java`
- Create: `src/main/java/com/example/ordernotify/dto/OrderResponse.java`

**Step 1: 创建常量类 `Constants.java`**

```java
package com.example.ordernotify.common;

/**
 * 常量类。
 *
 * 这类固定值单独抽出来，可以避免在多个文件里重复写字符串。
 */
public final class Constants {

    private Constants() {
    }

    public static final String ORDER_NOTIFY_EXCHANGE = "order.notify.exchange";
    public static final String ORDER_NOTIFY_QUEUE = "order.notify.queue";
    public static final String ORDER_NOTIFY_ROUTING_KEY = "order.notify.routing";
}
```

**Step 2: 创建统一返回类 `Result.java`**

```java
package com.example.ordernotify.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回对象。
 */
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

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
}
```

**Step 3: 创建订单实体 `OrderInfo.java`**

```java
package com.example.ordernotify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类，对应 t_order_info 表。
 */
@Data
@TableName("t_order_info")
public class OrderInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private String productName;

    private BigDecimal amount;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

**Step 4: 创建通知日志实体 `OrderNotifyLog.java`**

```java
package com.example.ordernotify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知日志实体类，对应 t_order_notify_log 表。
 */
@Data
@TableName("t_order_notify_log")
public class OrderNotifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private String notifyType;

    private String notifyStatus;

    private String remark;

    private LocalDateTime createTime;
}
```

**Step 5: 创建 Mapper 接口**

`OrderInfoMapper.java`

```java
package com.example.ordernotify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ordernotify.entity.OrderInfo;

/**
 * 订单表 Mapper。
 */
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
}
```

`OrderNotifyLogMapper.java`

```java
package com.example.ordernotify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ordernotify.entity.OrderNotifyLog;

/**
 * 通知日志表 Mapper。
 */
public interface OrderNotifyLogMapper extends BaseMapper<OrderNotifyLog> {
}
```

**Step 6: 创建 DTO**

`CreateOrderRequest.java`

```java
package com.example.ordernotify.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单接口的请求对象。
 */
@Data
public class CreateOrderRequest {

    private Long userId;

    private String productName;

    private BigDecimal amount;
}
```

`OrderResponse.java`

```java
package com.example.ordernotify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 创建订单成功后的响应对象。
 */
@Data
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;

    private String orderNo;

    private String status;
}
```

---

### Task 3: 创建 Service 和 Controller

**Files:**
- Create: `src/main/java/com/example/ordernotify/service/OrderService.java`
- Create: `src/main/java/com/example/ordernotify/service/NotifyService.java`
- Create: `src/main/java/com/example/ordernotify/service/impl/OrderServiceImpl.java`
- Create: `src/main/java/com/example/ordernotify/service/impl/NotifyServiceImpl.java`
- Create: `src/main/java/com/example/ordernotify/controller/OrderController.java`

**Step 1: 创建 `OrderService.java`**

```java
package com.example.ordernotify.service;

import com.example.ordernotify.dto.CreateOrderRequest;
import com.example.ordernotify.dto.OrderResponse;
import com.example.ordernotify.entity.OrderInfo;
import com.example.ordernotify.entity.OrderNotifyLog;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderInfo getOrderById(Long id);

    List<OrderNotifyLog> listNotifyLogs();
}
```

**Step 2: 创建 `NotifyService.java`**

```java
package com.example.ordernotify.service;

import com.example.ordernotify.mq.OrderNotifyMessage;

public interface NotifyService {

    void handleOrderNotify(OrderNotifyMessage message);
}
```

**Step 3: 创建 `OrderServiceImpl.java`**

```java
package com.example.ordernotify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ordernotify.dto.CreateOrderRequest;
import com.example.ordernotify.dto.OrderResponse;
import com.example.ordernotify.entity.OrderInfo;
import com.example.ordernotify.entity.OrderNotifyLog;
import com.example.ordernotify.mapper.OrderInfoMapper;
import com.example.ordernotify.mapper.OrderNotifyLogMapper;
import com.example.ordernotify.mq.OrderNotifyMessage;
import com.example.ordernotify.mq.OrderNotifyProducer;
import com.example.ordernotify.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单业务实现类。
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderInfoMapper orderInfoMapper;
    private final OrderNotifyLogMapper orderNotifyLogMapper;
    private final OrderNotifyProducer orderNotifyProducer;

    public OrderServiceImpl(OrderInfoMapper orderInfoMapper,
                            OrderNotifyLogMapper orderNotifyLogMapper,
                            OrderNotifyProducer orderNotifyProducer) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderNotifyLogMapper = orderNotifyLogMapper;
        this.orderNotifyProducer = orderNotifyProducer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (request.getProductName() == null || request.getProductName().isBlank()) {
            throw new IllegalArgumentException("productName 不能为空");
        }
        if (request.getAmount() == null) {
            throw new IllegalArgumentException("amount 不能为空");
        }

        LocalDateTime now = LocalDateTime.now();

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo(generateOrderNo());
        orderInfo.setUserId(request.getUserId());
        orderInfo.setProductName(request.getProductName());
        orderInfo.setAmount(request.getAmount());
        orderInfo.setStatus("CREATED");
        orderInfo.setCreateTime(now);
        orderInfo.setUpdateTime(now);
        orderInfoMapper.insert(orderInfo);

        // 主业务写库成功后，构建 MQ 消息。
        // 这一步代表“把通知动作交给异步系统处理”。
        OrderNotifyMessage message = new OrderNotifyMessage();
        message.setOrderId(orderInfo.getId());
        message.setOrderNo(orderInfo.getOrderNo());
        message.setUserId(orderInfo.getUserId());
        message.setProductName(orderInfo.getProductName());
        message.setAmount(orderInfo.getAmount());
        message.setMessageTime(now);

        orderNotifyProducer.send(message);

        return new OrderResponse(orderInfo.getId(), orderInfo.getOrderNo(), orderInfo.getStatus());
    }

    @Override
    public OrderInfo getOrderById(Long id) {
        return orderInfoMapper.selectById(id);
    }

    @Override
    public List<OrderNotifyLog> listNotifyLogs() {
        return orderNotifyLogMapper.selectList(
                new LambdaQueryWrapper<OrderNotifyLog>().orderByDesc(OrderNotifyLog::getId)
        );
    }

    private String generateOrderNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ORD" + timePart + randomPart;
    }
}
```

**Step 4: 创建 `NotifyServiceImpl.java`**

```java
package com.example.ordernotify.service.impl;

import com.example.ordernotify.entity.OrderNotifyLog;
import com.example.ordernotify.mapper.OrderNotifyLogMapper;
import com.example.ordernotify.mq.OrderNotifyMessage;
import com.example.ordernotify.service.NotifyService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 模拟通知发送服务。
 *
 * 这里不接真实短信平台，只往日志表插一条记录。
 * 这样可以非常直观地看到消费者到底有没有执行成功。
 */
@Service
public class NotifyServiceImpl implements NotifyService {

    private final OrderNotifyLogMapper orderNotifyLogMapper;

    public NotifyServiceImpl(OrderNotifyLogMapper orderNotifyLogMapper) {
        this.orderNotifyLogMapper = orderNotifyLogMapper;
    }

    @Override
    public void handleOrderNotify(OrderNotifyMessage message) {
        OrderNotifyLog log = new OrderNotifyLog();
        log.setOrderNo(message.getOrderNo());
        log.setNotifyType("SMS");
        log.setNotifyStatus("SUCCESS");
        log.setRemark("模拟发送通知成功，商品：" + message.getProductName());
        log.setCreateTime(LocalDateTime.now());
        orderNotifyLogMapper.insert(log);
    }
}
```

**Step 5: 创建控制器 `OrderController.java`**

```java
package com.example.ordernotify.controller;

import com.example.ordernotify.common.Result;
import com.example.ordernotify.dto.CreateOrderRequest;
import com.example.ordernotify.dto.OrderResponse;
import com.example.ordernotify.entity.OrderInfo;
import com.example.ordernotify.entity.OrderNotifyLog;
import com.example.ordernotify.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        return Result.success(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    public Result<OrderInfo> getOrder(@PathVariable Long id) {
        return Result.success(orderService.getOrderById(id));
    }

    @GetMapping("/notify/logs")
    public Result<List<OrderNotifyLog>> listNotifyLogs() {
        return Result.success(orderService.listNotifyLogs());
    }
}
```

---

### Task 4: 创建 MQ 配置、消息体、生产者和消费者

**Files:**
- Create: `src/main/java/com/example/ordernotify/config/RabbitMqConfig.java`
- Create: `src/main/java/com/example/ordernotify/mq/OrderNotifyMessage.java`
- Create: `src/main/java/com/example/ordernotify/mq/OrderNotifyProducer.java`
- Create: `src/main/java/com/example/ordernotify/mq/OrderNotifyConsumer.java`

**Step 1: 创建 `RabbitMqConfig.java`**

```java
package com.example.ordernotify.config;

import com.example.ordernotify.common.Constants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类。
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange orderNotifyExchange() {
        return new DirectExchange(Constants.ORDER_NOTIFY_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderNotifyQueue() {
        return new Queue(Constants.ORDER_NOTIFY_QUEUE, true);
    }

    @Bean
    public Binding orderNotifyBinding(Queue orderNotifyQueue, DirectExchange orderNotifyExchange) {
        return BindingBuilder.bind(orderNotifyQueue)
                .to(orderNotifyExchange)
                .with(Constants.ORDER_NOTIFY_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

**Step 2: 创建消息对象 `OrderNotifyMessage.java`**

```java
package com.example.ordernotify.mq;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单通知消息体。
 */
@Data
public class OrderNotifyMessage {

    private Long orderId;

    private String orderNo;

    private Long userId;

    private String productName;

    private BigDecimal amount;

    private LocalDateTime messageTime;
}
```

**Step 3: 创建生产者 `OrderNotifyProducer.java`**

```java
package com.example.ordernotify.mq;

import com.example.ordernotify.common.Constants;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * MQ 生产者。
 */
@Component
public class OrderNotifyProducer {

    private final RabbitTemplate rabbitTemplate;

    public OrderNotifyProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(OrderNotifyMessage message) {
        rabbitTemplate.convertAndSend(
                Constants.ORDER_NOTIFY_EXCHANGE,
                Constants.ORDER_NOTIFY_ROUTING_KEY,
                message
        );
    }
}
```

**Step 4: 创建消费者 `OrderNotifyConsumer.java`**

```java
package com.example.ordernotify.mq;

import com.example.ordernotify.common.Constants;
import com.example.ordernotify.service.NotifyService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * MQ 消费者。
 *
 * 消费成功后手动 ACK。
 * 消费失败时 NACK 并重新入队。
 */
@Component
public class OrderNotifyConsumer {

    private final NotifyService notifyService;

    public OrderNotifyConsumer(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @RabbitListener(queues = Constants.ORDER_NOTIFY_QUEUE)
    public void receive(OrderNotifyMessage message, Message amqpMessage, Channel channel) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();

        try {
            notifyService.handleOrderNotify(message);
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

**Step 1: 启动应用**

Run:

```bash
mvn spring-boot:run
```

Expected:

- 应用启动成功
- 控制台能看到 SQL 初始化日志
- 控制台能看到 RabbitMQ 连接成功日志

**Step 2: 调用创建订单接口**

Run:

```bash
curl -X POST "http://localhost:8081/orders" ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\":1001,\"productName\":\"Java训练营课程\",\"amount\":199.00}"
```

Expected:

- 返回 `code=200`
- 得到新的 `orderId` 和 `orderNo`

**Step 3: 查询订单**

Run:

```bash
curl "http://localhost:8081/orders/1"
```

**Step 4: 查询通知日志**

Run:

```bash
curl "http://localhost:8081/orders/notify/logs"
```

Expected:

- 返回列表中能看到一条 `notifyStatus=SUCCESS`
- 说明消费者已经收到消息并完成了通知动作

**Step 5: 打开 RabbitMQ 控制台**

访问：

```text
http://localhost:15673
```

账号密码：

```text
guest / guest
```

重点观察：

- `Exchanges` 是否有 `order.notify.exchange`
- `Queues` 是否有 `order.notify.queue`
- 队列中的 `Ready` 和 `Unacked` 是否变化过

---

### Task 6: 常见错误排查

**问题 1：MySQL 连接失败**

排查：

- 是否先执行了 `docker compose up -d`
- `3307` 是否被占用
- 容器是否完全启动

命令：

```bash
docker compose ps
```

**问题 2：消息发出去了，但消费者没有执行**

排查：

- `exchange`、`queue`、`routingKey` 是否写成同一组名称
- `@RabbitListener` 是否监听了 `order.notify.queue`
- `application.yml` 中 RabbitMQ 端口是否为 `5673`

**问题 3：消息被反复消费**

原因：

- 你的消费者使用了手动确认
- 业务抛异常时，代码会 `basicNack(..., true)`，消息会重新入队

这正是 MQ 的一个重要知识点：消息不是“收到了就结束”，而是“处理成功并确认了才结束”。

---

## 你做完这个项目后应该掌握什么

1. 为什么“下单”和“发送通知”适合拆开
2. 生产者为什么不应该自己处理通知逻辑
3. RabbitMQ 的交换机、队列、路由键分别负责什么
4. 手动 ACK 的作用是什么
5. 为什么异步化可以减少主流程耗时
