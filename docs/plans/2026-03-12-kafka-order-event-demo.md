# kafka-order-event-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot + Kafka 项目，完成“创建订单后发送 Kafka 事件，消费者异步写通知日志”的练习模块，让初学者掌握 Kafka 最基础的生产和消费流程。

**Architecture:** 这个项目的核心链路是 `Controller -> Service -> MySQL -> Kafka Producer -> Kafka Topic -> Kafka Consumer -> MySQL`。订单创建是主流程，通知日志写入是异步从流程，Kafka 在这里承担“事件总线”的角色。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Web, Spring for Apache Kafka, Spring Data JPA, MySQL 8.0, Apache Kafka Docker Image, Docker Compose, Maven, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 Kafka

订单创建成功后，往往还会触发很多后续动作：

- 发送通知
- 推送积分
- 写审计日志
- 通知下游系统

这些后续动作如果都同步写在下单接口里，会让主流程变慢。  
Kafka 很适合做这种“先完成主业务，再异步发送事件”的场景。

---

## 二、最终目录结构

```text
kafka-order-event-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/kafkaorderevent
│   │   │   ├── KafkaOrderEventApplication.java
│   │   │   ├── common
│   │   │   │   ├── Constants.java
│   │   │   │   └── Result.java
│   │   │   ├── controller
│   │   │   │   └── OrderController.java
│   │   │   ├── dto
│   │   │   │   ├── CreateOrderRequest.java
│   │   │   │   └── OrderResponse.java
│   │   │   ├── entity
│   │   │   │   ├── NotifyLog.java
│   │   │   │   └── OrderInfo.java
│   │   │   ├── kafka
│   │   │   │   ├── KafkaTopicConfig.java
│   │   │   │   ├── OrderCreatedConsumer.java
│   │   │   │   ├── OrderCreatedEvent.java
│   │   │   │   └── OrderCreatedProducer.java
│   │   │   ├── repository
│   │   │   │   ├── NotifyLogRepository.java
│   │   │   │   └── OrderInfoRepository.java
│   │   │   └── service
│   │   │       ├── OrderService.java
│   │   │       └── impl
│   │   │           └── OrderServiceImpl.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── data.sql
│   │       └── schema.sql
│   └── test
│       └── java/com/example/kafkaorderevent/KafkaOrderEventApplicationTests.java
```

这段目录结构的作用：

- 帮你提前看清项目里会有哪些文件
- 把“业务代码”和“Kafka 代码”分开放，方便理解
- 后面你照着这个结构逐步创建文件，不容易乱

---

## 三、端口规划

- 应用端口：`8121`
- MySQL：`3319`
- Kafka：`9093`

这段端口规划的作用：

- 避免和你前面已经生成的 MQ、Redis、MyBatis、Security 文档冲突
- 让 3 个 Kafka 练习项目可以并行存在于同一台机器

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/kafkaorderevent/KafkaOrderEventApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/kafkaorderevent/KafkaOrderEventApplicationTests.java`

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
    <artifactId>kafka-order-event-demo</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
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

这段代码的作用：

- `spring-kafka` 提供 Kafka 生产者和消费者能力
- `spring-boot-starter-data-jpa` 用来把订单和通知日志落到 MySQL
- `mysql-connector-j` 负责连接 MySQL

**Step 2: 创建 `docker-compose.yml`**

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: kafka-order-event-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: kafka_order_event_db
    ports:
      - "3319:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - kafka-order-event-mysql-data:/var/lib/mysql

  kafka:
    image: apache/kafka:latest
    container_name: kafka-order-event-broker
    restart: always
    ports:
      - "9093:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0

volumes:
  kafka-order-event-mysql-data:
```

这段代码的作用：

- 启动一个 MySQL 容器，给应用保存订单数据
- 启动一个 Kafka 单机 Broker，适合本地学习
- 使用 KRaft 模式，不再依赖 ZooKeeper，配置更简单

**Step 3: 创建启动类 `KafkaOrderEventApplication.java`**

```java
package com.example.kafkaorderevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KafkaOrderEventApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaOrderEventApplication.class, args);
    }
}
```

这段代码的作用：

- 告诉 Spring Boot 从这里启动整个项目
- 这是所有 Spring Boot 项目的标准入口类

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8121

spring:
  application:
    name: kafka-order-event-demo

  datasource:
    url: jdbc:mysql://localhost:3319/kafka_order_event_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true

  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

  kafka:
    bootstrap-servers: localhost:9093
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: order-notify-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
        spring.json.value.default.type: com.example.kafkaorderevent.kafka.OrderCreatedEvent
```

这段代码的作用：

- 配置应用端口和 MySQL 连接
- 配置 Kafka Broker 地址
- 指定生产者把 Java 对象序列化成 JSON
- 指定消费者把 JSON 反序列化成 `OrderCreatedEvent`

**Step 5: 创建 `schema.sql`**

```sql
DROP TABLE IF EXISTS t_notify_log;
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

CREATE TABLE t_notify_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    notify_status VARCHAR(32) NOT NULL COMMENT '通知状态',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL COMMENT '创建时间'
);
```

这段代码的作用：

- `t_order_info` 保存订单主数据
- `t_notify_log` 保存消费者处理后的通知日志
- 这样你可以同时看到“主业务是否成功”和“异步消费是否成功”

**Step 6: 创建 `data.sql`**

```sql
-- 当前项目不预置订单数据，建议直接走创建订单接口测试。
```

这段代码的作用：

- 告诉 Spring Boot 启动时不插入测试订单
- 让你把注意力集中在完整的“发消息 -> 消费消息”流程上

**Step 7: 创建测试类 `KafkaOrderEventApplicationTests.java`**

```java
package com.example.kafkaorderevent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class KafkaOrderEventApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

这段代码的作用：

- 只验证 Spring 容器是否能正常启动
- 这是所有新项目最基础的一层验证

---

### Task 2: 创建通用类、实体、Repository、DTO 和 Kafka 事件对象

**Files:**
- Create: `src/main/java/com/example/kafkaorderevent/common/Constants.java`
- Create: `src/main/java/com/example/kafkaorderevent/common/Result.java`
- Create: `src/main/java/com/example/kafkaorderevent/entity/OrderInfo.java`
- Create: `src/main/java/com/example/kafkaorderevent/entity/NotifyLog.java`
- Create: `src/main/java/com/example/kafkaorderevent/repository/OrderInfoRepository.java`
- Create: `src/main/java/com/example/kafkaorderevent/repository/NotifyLogRepository.java`
- Create: `src/main/java/com/example/kafkaorderevent/dto/CreateOrderRequest.java`
- Create: `src/main/java/com/example/kafkaorderevent/dto/OrderResponse.java`
- Create: `src/main/java/com/example/kafkaorderevent/kafka/OrderCreatedEvent.java`

**Step 1: 创建 `Constants.java`**

```java
package com.example.kafkaorderevent.common;

public final class Constants {

    private Constants() {
    }

    public static final String ORDER_CREATED_TOPIC = "order-created-topic";
}
```

这段代码的作用：

- 把 Topic 名称提成常量
- 避免在 Producer、Consumer、Config 里重复写字符串

**Step 2: 创建 `Result.java`**

```java
package com.example.kafkaorderevent.common;

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

这段代码的作用：

- 统一接口返回格式
- 让你测试接口时响应结构更清晰

**Step 3: 创建实体类**

`OrderInfo.java`

```java
package com.example.kafkaorderevent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_order_info")
public class OrderInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

这段代码的作用：

- 映射数据库中的订单表
- 订单主流程创建成功后，会先保存这张表

`NotifyLog.java`

```java
package com.example.kafkaorderevent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_notify_log")
public class NotifyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNo;

    private String notifyStatus;

    private String remark;

    private LocalDateTime createTime;
}
```

这段代码的作用：

- 映射数据库中的通知日志表
- Kafka 消费成功后，消费者会往这里插入一条记录

**Step 4: 创建 Repository**

`OrderInfoRepository.java`

```java
package com.example.kafkaorderevent.repository;

import com.example.kafkaorderevent.entity.OrderInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderInfoRepository extends JpaRepository<OrderInfo, Long> {
}
```

这段代码的作用：

- 提供订单表的基础持久化能力
- 让我们不用手写最基础的增删改查 SQL

`NotifyLogRepository.java`

```java
package com.example.kafkaorderevent.repository;

import com.example.kafkaorderevent.entity.NotifyLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotifyLogRepository extends JpaRepository<NotifyLog, Long> {

    List<NotifyLog> findAllByOrderByIdDesc();
}
```

这段代码的作用：

- 提供通知日志表的基础持久化能力
- 增加了一个按 ID 倒序查询的方法，方便接口里直接看最新日志

**Step 5: 创建 DTO 和事件对象**

`CreateOrderRequest.java`

```java
package com.example.kafkaorderevent.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {

    private Long userId;

    private String productName;

    private BigDecimal amount;
}
```

这段代码的作用：

- 表示创建订单接口的入参
- 让接口层和数据库实体层职责分开

`OrderResponse.java`

```java
package com.example.kafkaorderevent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;

    private String orderNo;

    private String status;
}
```

这段代码的作用：

- 表示创建订单接口的响应
- 返回对前端最有意义的几个字段

`OrderCreatedEvent.java`

```java
package com.example.kafkaorderevent.kafka;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderCreatedEvent {

    private String orderNo;

    private Long userId;

    private String productName;

    private BigDecimal amount;

    private LocalDateTime eventTime;
}
```

这段代码的作用：

- 这是发到 Kafka Topic 中的事件对象
- Producer 把它发出去，Consumer 再把它接收回来

---

### Task 3: 创建 Kafka 配置、Producer、Consumer、Service 和 Controller

**Files:**
- Create: `src/main/java/com/example/kafkaorderevent/kafka/KafkaTopicConfig.java`
- Create: `src/main/java/com/example/kafkaorderevent/kafka/OrderCreatedProducer.java`
- Create: `src/main/java/com/example/kafkaorderevent/kafka/OrderCreatedConsumer.java`
- Create: `src/main/java/com/example/kafkaorderevent/service/OrderService.java`
- Create: `src/main/java/com/example/kafkaorderevent/service/impl/OrderServiceImpl.java`
- Create: `src/main/java/com/example/kafkaorderevent/controller/OrderController.java`

**Step 1: 创建 `KafkaTopicConfig.java`**

```java
package com.example.kafkaorderevent.kafka;

import com.example.kafkaorderevent.common.Constants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderCreatedTopic() {
        return new NewTopic(Constants.ORDER_CREATED_TOPIC, 1, (short) 1);
    }
}
```

这段代码的作用：

- 启动项目时自动创建一个 Kafka Topic
- 这样你不用手动去 Kafka 里建 Topic，更适合新手练习

**Step 2: 创建 Producer `OrderCreatedProducer.java`**

```java
package com.example.kafkaorderevent.kafka;

import com.example.kafkaorderevent.common.Constants;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public OrderCreatedProducer(KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(OrderCreatedEvent event) {
        kafkaTemplate.send(Constants.ORDER_CREATED_TOPIC, event.getOrderNo(), event);
    }
}
```

这段代码的作用：

- 负责把订单创建事件发送到 Kafka
- 这里把 `orderNo` 作为消息 Key，一方面便于追踪，一方面为后续有序场景做铺垫

**Step 3: 创建 Consumer `OrderCreatedConsumer.java`**

```java
package com.example.kafkaorderevent.kafka;

import com.example.kafkaorderevent.entity.NotifyLog;
import com.example.kafkaorderevent.repository.NotifyLogRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OrderCreatedConsumer {

    private final NotifyLogRepository notifyLogRepository;

    public OrderCreatedConsumer(NotifyLogRepository notifyLogRepository) {
        this.notifyLogRepository = notifyLogRepository;
    }

    @KafkaListener(topics = "order-created-topic", groupId = "order-notify-group")
    public void listen(OrderCreatedEvent event) {
        NotifyLog notifyLog = new NotifyLog();
        notifyLog.setOrderNo(event.getOrderNo());
        notifyLog.setNotifyStatus("SUCCESS");
        notifyLog.setRemark("Kafka 消费成功，已模拟发送通知");
        notifyLog.setCreateTime(LocalDateTime.now());
        notifyLogRepository.save(notifyLog);
    }
}
```

这段代码的作用：

- 监听 `order-created-topic`
- 只要 Producer 发消息过来，这个方法就会被调用
- 方法里把“消费成功”的结果落到数据库，便于你直观看到效果

**Step 4: 创建 Service**

`OrderService.java`

```java
package com.example.kafkaorderevent.service;

import com.example.kafkaorderevent.dto.CreateOrderRequest;
import com.example.kafkaorderevent.dto.OrderResponse;
import com.example.kafkaorderevent.entity.NotifyLog;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    List<NotifyLog> listNotifyLogs();
}
```

这段代码的作用：

- 定义订单模块对外提供的业务能力
- 让 Controller 只依赖接口，不直接操作数据库和 Kafka

`OrderServiceImpl.java`

```java
package com.example.kafkaorderevent.service.impl;

import com.example.kafkaorderevent.dto.CreateOrderRequest;
import com.example.kafkaorderevent.dto.OrderResponse;
import com.example.kafkaorderevent.entity.NotifyLog;
import com.example.kafkaorderevent.entity.OrderInfo;
import com.example.kafkaorderevent.kafka.OrderCreatedEvent;
import com.example.kafkaorderevent.kafka.OrderCreatedProducer;
import com.example.kafkaorderevent.repository.NotifyLogRepository;
import com.example.kafkaorderevent.repository.OrderInfoRepository;
import com.example.kafkaorderevent.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderInfoRepository orderInfoRepository;
    private final NotifyLogRepository notifyLogRepository;
    private final OrderCreatedProducer orderCreatedProducer;

    public OrderServiceImpl(OrderInfoRepository orderInfoRepository,
                            NotifyLogRepository notifyLogRepository,
                            OrderCreatedProducer orderCreatedProducer) {
        this.orderInfoRepository = orderInfoRepository;
        this.notifyLogRepository = notifyLogRepository;
        this.orderCreatedProducer = orderCreatedProducer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(CreateOrderRequest request) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo(generateOrderNo());
        orderInfo.setUserId(request.getUserId());
        orderInfo.setProductName(request.getProductName());
        orderInfo.setAmount(request.getAmount());
        orderInfo.setStatus("CREATED");
        orderInfo.setCreateTime(LocalDateTime.now());
        orderInfo.setUpdateTime(LocalDateTime.now());
        orderInfoRepository.save(orderInfo);

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderNo(orderInfo.getOrderNo());
        event.setUserId(orderInfo.getUserId());
        event.setProductName(orderInfo.getProductName());
        event.setAmount(orderInfo.getAmount());
        event.setEventTime(LocalDateTime.now());
        orderCreatedProducer.send(event);

        return new OrderResponse(orderInfo.getId(), orderInfo.getOrderNo(), orderInfo.getStatus());
    }

    @Override
    public List<NotifyLog> listNotifyLogs() {
        return notifyLogRepository.findAllByOrderByIdDesc();
    }

    private String generateOrderNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ORD" + timePart + randomPart;
    }
}
```

这段代码的作用：

- 先创建订单并保存到 MySQL
- 再组装一个事件对象发到 Kafka
- 这就是最典型的“主业务完成后发送异步事件”

**Step 5: 创建控制器 `OrderController.java`**

```java
package com.example.kafkaorderevent.controller;

import com.example.kafkaorderevent.common.Result;
import com.example.kafkaorderevent.dto.CreateOrderRequest;
import com.example.kafkaorderevent.dto.OrderResponse;
import com.example.kafkaorderevent.entity.NotifyLog;
import com.example.kafkaorderevent.service.OrderService;
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

    @GetMapping("/notify/logs")
    public Result<List<NotifyLog>> listNotifyLogs() {
        return Result.success(orderService.listNotifyLogs());
    }
}
```

这段代码的作用：

- 提供一个创建订单接口
- 提供一个查看消费结果日志的接口
- 这样你可以完整验证“发消息”和“收消息”的闭环

---

### Task 4: 启动项目并验证 Kafka 链路

**Step 1: 启动中间件**

Run:

```bash
docker compose up -d
```

这条命令的作用：

- 启动 MySQL 和 Kafka
- 让你的 Spring Boot 应用有可连接的外部中间件

**Step 2: 启动应用**

Run:

```bash
mvn spring-boot:run
```

这条命令的作用：

- 启动 Spring Boot 服务
- 启动后会自动连接 MySQL 和 Kafka，并自动创建 Topic

**Step 3: 创建订单**

Run:

```bash
curl -X POST "http://localhost:8121/orders" ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\":1001,\"productName\":\"Kafka实战课\",\"amount\":299.00}"
```

这条命令的作用：

- 调用创建订单接口
- 触发“保存订单 + 发送 Kafka 事件”这条业务链路

**Step 4: 查询通知日志**

Run:

```bash
curl "http://localhost:8121/orders/notify/logs"
```

这条命令的作用：

- 查看 Kafka 消费者是否已经成功消费消息
- 如果返回日志记录，说明异步事件链路已经跑通

---

### Task 5: 常见错误排查

**问题 1：启动时报 Kafka 连接失败**

排查：

- `docker compose up -d` 是否已执行
- `application.yml` 中 `bootstrap-servers` 是否写成 `localhost:9093`
- Kafka 容器是否启动成功

**问题 2：消息发送成功，但消费者没有收到**

排查：

- Topic 名称是否一致
- `@KafkaListener` 监听的 Topic 是否写对
- 消费者的反序列化类型是否配置成 `OrderCreatedEvent`

**问题 3：为什么 Producer 和 Consumer 都要关心事件对象**

原因：

- Producer 负责发送什么结构的消息
- Consumer 负责接收并解析这个结构
- 这就是“事件驱动”里最核心的载体

---

## 你做完这个项目后应该掌握什么

1. Kafka 最基础的生产和消费流程是什么
2. 为什么订单主流程和后续动作适合用 Kafka 解耦
3. Topic、Producer、Consumer 分别负责什么
4. 为什么消费结果通常要落库方便排查
