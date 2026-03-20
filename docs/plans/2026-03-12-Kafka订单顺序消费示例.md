# kafka-order-sequence-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot + Kafka 项目，完成“同一个订单号作为消息 Key，按顺序发送和消费订单状态事件”的练习模块，让初学者理解 Kafka 中 Key、Partition 和顺序消费的关系。

**Architecture:** 这个项目的核心链路是 `Controller -> Service -> Kafka Producer -> Topic(多分区) -> Consumer -> MySQL`。Producer 使用 `orderNo` 作为消息 Key 发送状态事件，Kafka 会把相同 Key 的消息路由到同一个分区，Consumer 再按该分区内的顺序处理并落库。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Web, Spring for Apache Kafka, Spring Data JPA, MySQL 8.0, Apache Kafka Docker Image, Docker Compose, Maven, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会强调消息 Key

很多业务事件并不是“随便什么顺序都行”，例如订单状态流转：

1. 订单创建
2. 支付成功
3. 已发货
4. 已完成

如果顺序错了，业务就会混乱。  
Kafka 的一个关键点是：

- Topic 可以有多个分区
- 同一个 Key 的消息会进入同一个分区
- 同一个分区内的消息是有序的

---

## 二、最终目录结构

```text
kafka-order-sequence-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/kafkaordersequence
│   │   │   ├── KafkaOrderSequenceApplication.java
│   │   │   ├── common
│   │   │   │   ├── Constants.java
│   │   │   │   └── Result.java
│   │   │   ├── controller
│   │   │   │   └── OrderEventController.java
│   │   │   ├── dto
│   │   │   │   ├── OrderEventRequest.java
│   │   │   │   └── OrderEventResponse.java
│   │   │   ├── entity
│   │   │   │   └── OrderStatusLog.java
│   │   │   ├── kafka
│   │   │   │   ├── KafkaTopicConfig.java
│   │   │   │   ├── OrderStatusConsumer.java
│   │   │   │   ├── OrderStatusEvent.java
│   │   │   │   └── OrderStatusProducer.java
│   │   │   ├── repository
│   │   │   │   └── OrderStatusLogRepository.java
│   │   │   └── service
│   │   │       ├── OrderEventService.java
│   │   │       └── impl
│   │   │           └── OrderEventServiceImpl.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── data.sql
│   │       └── schema.sql
│   └── test
│       └── java/com/example/kafkaordersequence/KafkaOrderSequenceApplicationTests.java
```

这段目录结构的作用：

- 把“事件发送”和“事件消费后日志记录”拆成清晰的模块
- 方便你专门观察 Kafka 的顺序处理结果

---

## 三、端口规划

- 应用端口：`8123`
- MySQL：`3321`
- Kafka：`9095`

这段端口规划的作用：

- 和前两个 Kafka 练习项目互不冲突
- 让你可以独立练习“顺序消费”这个知识点

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/kafkaordersequence/KafkaOrderSequenceApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/kafkaordersequence/KafkaOrderSequenceApplicationTests.java`

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
    <artifactId>kafka-order-sequence-demo</artifactId>
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
</project>
```

这段代码的作用：

- 提供 Kafka 顺序事件项目所需的基础依赖
- 用 JPA 保存消费后的顺序日志，方便观察分区和偏移量

**Step 2: 创建 `docker-compose.yml`**

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: kafka-order-sequence-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: kafka_order_sequence_db
    ports:
      - "3321:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - kafka-order-sequence-mysql-data:/var/lib/mysql

  kafka:
    image: apache/kafka:latest
    container_name: kafka-order-sequence-broker
    restart: always
    ports:
      - "9095:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9095
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0

volumes:
  kafka-order-sequence-mysql-data:
```

这段代码的作用：

- 启动 Kafka 和 MySQL
- Kafka 负责接收顺序事件，MySQL 负责保存消费后的处理日志

**Step 3: 创建启动类 `KafkaOrderSequenceApplication.java`**

```java
package com.example.kafkaordersequence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KafkaOrderSequenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaOrderSequenceApplication.class, args);
    }
}
```

这段代码的作用：

- 作为整个顺序事件项目的启动入口

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8123

spring:
  application:
    name: kafka-order-sequence-demo

  datasource:
    url: jdbc:mysql://localhost:3321/kafka_order_sequence_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
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
    bootstrap-servers: localhost:9095
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: order-sequence-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
        spring.json.value.default.type: com.example.kafkaordersequence.kafka.OrderStatusEvent
```

这段代码的作用：

- 配置 Kafka 的生产者和消费者
- 这里的重点是“允许发送 String Key + JSON Value”的消息结构

**Step 5: 创建 `schema.sql`**

```sql
DROP TABLE IF EXISTS t_order_status_log;

CREATE TABLE t_order_status_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    order_status VARCHAR(32) NOT NULL COMMENT '订单状态',
    kafka_partition INT NOT NULL COMMENT 'Kafka分区',
    kafka_offset BIGINT NOT NULL COMMENT 'Kafka偏移量',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL COMMENT '创建时间'
);
```

这段代码的作用：

- 保存消费者处理后的订单状态日志
- 同时记录分区号和偏移量，方便你观察“同一个 orderNo 是否进入了同一个分区”

**Step 6: 创建 `data.sql`**

```sql
-- 当前项目不预置数据，建议直接通过接口发送状态事件。
```

这段代码的作用：

- 让你直接从发事件开始练习
- 不依赖预置测试数据

**Step 7: 创建测试类 `KafkaOrderSequenceApplicationTests.java`**

```java
package com.example.kafkaordersequence;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class KafkaOrderSequenceApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

这段代码的作用：

- 只验证 Spring Boot 项目能否正常启动

---

### Task 2: 创建通用类、实体、Repository、DTO、事件对象

**Files:**
- Create: `src/main/java/com/example/kafkaordersequence/common/Constants.java`
- Create: `src/main/java/com/example/kafkaordersequence/common/Result.java`
- Create: `src/main/java/com/example/kafkaordersequence/entity/OrderStatusLog.java`
- Create: `src/main/java/com/example/kafkaordersequence/repository/OrderStatusLogRepository.java`
- Create: `src/main/java/com/example/kafkaordersequence/dto/OrderEventRequest.java`
- Create: `src/main/java/com/example/kafkaordersequence/dto/OrderEventResponse.java`
- Create: `src/main/java/com/example/kafkaordersequence/kafka/OrderStatusEvent.java`

**Step 1: 创建 `Constants.java`**

```java
package com.example.kafkaordersequence.common;

public final class Constants {

    private Constants() {
    }

    public static final String ORDER_STATUS_TOPIC = "order-status-topic";
}
```

这段代码的作用：

- 统一管理订单状态 Topic 名称
- 避免 Topic 名称在多个文件里散落

**Step 2: 创建 `Result.java`**

```java
package com.example.kafkaordersequence.common;

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

- 统一接口返回结构

**Step 3: 创建实体和 Repository**

`OrderStatusLog.java`

```java
package com.example.kafkaordersequence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_order_status_log")
public class OrderStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNo;

    private String orderStatus;

    private Integer kafkaPartition;

    private Long kafkaOffset;

    private String remark;

    private LocalDateTime createTime;
}
```

这段代码的作用：

- 保存每一条被消费的订单状态事件
- 重点记录分区号和偏移量，便于你验证顺序和分区关系

`OrderStatusLogRepository.java`

```java
package com.example.kafkaordersequence.repository;

import com.example.kafkaordersequence.entity.OrderStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusLogRepository extends JpaRepository<OrderStatusLog, Long> {

    List<OrderStatusLog> findAllByOrderNoOrderByIdAsc(String orderNo);
}
```

这段代码的作用：

- 提供按订单号顺序查询日志的方法
- 方便你查看同一个订单的状态流转顺序

**Step 4: 创建 DTO 和事件对象**

`OrderEventRequest.java`

```java
package com.example.kafkaordersequence.dto;

import lombok.Data;

@Data
public class OrderEventRequest {

    private String orderNo;

    private String orderStatus;
}
```

这段代码的作用：

- 作为发送订单状态事件接口的请求体

`OrderEventResponse.java`

```java
package com.example.kafkaordersequence.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderEventResponse {

    private String orderNo;

    private String orderStatus;

    private String message;
}
```

这段代码的作用：

- 表示发送事件接口的响应

`OrderStatusEvent.java`

```java
package com.example.kafkaordersequence.kafka;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderStatusEvent {

    private String orderNo;

    private String orderStatus;

    private LocalDateTime eventTime;
}
```

这段代码的作用：

- 这是 Kafka Topic 中真正流转的订单状态事件
- 同一个订单的不同状态，都会用同一个 `orderNo` 作为消息 Key

---

### Task 3: 创建 Kafka 组件、Service 和 Controller

**Files:**
- Create: `src/main/java/com/example/kafkaordersequence/kafka/KafkaTopicConfig.java`
- Create: `src/main/java/com/example/kafkaordersequence/kafka/OrderStatusProducer.java`
- Create: `src/main/java/com/example/kafkaordersequence/kafka/OrderStatusConsumer.java`
- Create: `src/main/java/com/example/kafkaordersequence/service/OrderEventService.java`
- Create: `src/main/java/com/example/kafkaordersequence/service/impl/OrderEventServiceImpl.java`
- Create: `src/main/java/com/example/kafkaordersequence/controller/OrderEventController.java`

**Step 1: 创建 Topic 配置和 Producer**

`KafkaTopicConfig.java`

```java
package com.example.kafkaordersequence.kafka;

import com.example.kafkaordersequence.common.Constants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderStatusTopic() {
        return new NewTopic(Constants.ORDER_STATUS_TOPIC, 3, (short) 1);
    }
}
```

这段代码的作用：

- 创建一个有 3 个分区的 Topic
- 这样才有“相同 Key 进入同一分区，不同 Key 可能分到不同分区”的观察空间

`OrderStatusProducer.java`

```java
package com.example.kafkaordersequence.kafka;

import com.example.kafkaordersequence.common.Constants;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusProducer {

    private final KafkaTemplate<String, OrderStatusEvent> kafkaTemplate;

    public OrderStatusProducer(KafkaTemplate<String, OrderStatusEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(OrderStatusEvent event) {
        kafkaTemplate.send(Constants.ORDER_STATUS_TOPIC, event.getOrderNo(), event);
    }
}
```

这段代码的作用：

- 发送订单状态事件
- 最关键的一点是：把 `orderNo` 当作消息 Key 发送出去

**Step 2: 创建 Consumer**

```java
package com.example.kafkaordersequence.kafka;

import com.example.kafkaordersequence.entity.OrderStatusLog;
import com.example.kafkaordersequence.repository.OrderStatusLogRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OrderStatusConsumer {

    private final OrderStatusLogRepository orderStatusLogRepository;

    public OrderStatusConsumer(OrderStatusLogRepository orderStatusLogRepository) {
        this.orderStatusLogRepository = orderStatusLogRepository;
    }

    @KafkaListener(topics = "order-status-topic", groupId = "order-sequence-group")
    public void listen(ConsumerRecord<String, OrderStatusEvent> record) {
        OrderStatusEvent event = record.value();

        OrderStatusLog log = new OrderStatusLog();
        log.setOrderNo(event.getOrderNo());
        log.setOrderStatus(event.getOrderStatus());
        log.setKafkaPartition(record.partition());
        log.setKafkaOffset(record.offset());
        log.setRemark("同一个 orderNo 会被路由到同一个分区");
        log.setCreateTime(LocalDateTime.now());
        orderStatusLogRepository.save(log);
    }
}
```

这段代码的作用：

- 使用 `ConsumerRecord` 接收完整 Kafka 消息
- 这样不仅能拿到事件内容，还能拿到分区号和偏移量
- 这正是验证“顺序消费”和“分区路由”的关键

**Step 3: 创建 Service 和 Controller**

`OrderEventService.java`

```java
package com.example.kafkaordersequence.service;

import com.example.kafkaordersequence.dto.OrderEventRequest;
import com.example.kafkaordersequence.dto.OrderEventResponse;
import com.example.kafkaordersequence.entity.OrderStatusLog;

import java.util.List;

public interface OrderEventService {

    OrderEventResponse sendEvent(OrderEventRequest request);

    List<OrderStatusLog> listByOrderNo(String orderNo);
}
```

这段代码的作用：

- 定义发送事件和查看消费结果的业务接口

`OrderEventServiceImpl.java`

```java
package com.example.kafkaordersequence.service.impl;

import com.example.kafkaordersequence.dto.OrderEventRequest;
import com.example.kafkaordersequence.dto.OrderEventResponse;
import com.example.kafkaordersequence.entity.OrderStatusLog;
import com.example.kafkaordersequence.kafka.OrderStatusEvent;
import com.example.kafkaordersequence.kafka.OrderStatusProducer;
import com.example.kafkaordersequence.repository.OrderStatusLogRepository;
import com.example.kafkaordersequence.service.OrderEventService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderEventServiceImpl implements OrderEventService {

    private final OrderStatusProducer orderStatusProducer;
    private final OrderStatusLogRepository orderStatusLogRepository;

    public OrderEventServiceImpl(OrderStatusProducer orderStatusProducer,
                                 OrderStatusLogRepository orderStatusLogRepository) {
        this.orderStatusProducer = orderStatusProducer;
        this.orderStatusLogRepository = orderStatusLogRepository;
    }

    @Override
    public OrderEventResponse sendEvent(OrderEventRequest request) {
        OrderStatusEvent event = new OrderStatusEvent();
        event.setOrderNo(request.getOrderNo());
        event.setOrderStatus(request.getOrderStatus());
        event.setEventTime(LocalDateTime.now());
        orderStatusProducer.send(event);

        return new OrderEventResponse(request.getOrderNo(), request.getOrderStatus(), "订单状态事件已发送");
    }

    @Override
    public List<OrderStatusLog> listByOrderNo(String orderNo) {
        return orderStatusLogRepository.findAllByOrderNoOrderByIdAsc(orderNo);
    }
}
```

这段代码的作用：

- 把接口传进来的订单状态包装成 Kafka 事件并发出去
- 同时提供按订单号查询消费日志的能力

`OrderEventController.java`

```java
package com.example.kafkaordersequence.controller;

import com.example.kafkaordersequence.common.Result;
import com.example.kafkaordersequence.dto.OrderEventRequest;
import com.example.kafkaordersequence.dto.OrderEventResponse;
import com.example.kafkaordersequence.entity.OrderStatusLog;
import com.example.kafkaordersequence.service.OrderEventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order-events")
public class OrderEventController {

    private final OrderEventService orderEventService;

    public OrderEventController(OrderEventService orderEventService) {
        this.orderEventService = orderEventService;
    }

    @PostMapping
    public Result<OrderEventResponse> sendEvent(@RequestBody OrderEventRequest request) {
        return Result.success(orderEventService.sendEvent(request));
    }

    @GetMapping("/{orderNo}")
    public Result<List<OrderStatusLog>> listByOrderNo(@PathVariable String orderNo) {
        return Result.success(orderEventService.listByOrderNo(orderNo));
    }
}
```

这段代码的作用：

- 提供一个发送订单状态事件的接口
- 提供一个查询同一订单消费日志的接口

---

### Task 4: 启动项目并验证顺序消费

**Step 1: 启动中间件**

Run:

```bash
docker compose up -d
```

这条命令的作用：

- 启动 Kafka 和 MySQL
- 让你有一个可用的顺序事件实验环境

**Step 2: 启动应用**

Run:

```bash
mvn spring-boot:run
```

这条命令的作用：

- 启动 Producer 和 Consumer
- 自动创建多分区 Topic

**Step 3: 连续发送同一个订单号的状态事件**

Run:

```bash
curl -X POST "http://localhost:8123/order-events" ^
  -H "Content-Type: application/json" ^
  -d "{\"orderNo\":\"ORD202603120001\",\"orderStatus\":\"CREATED\"}"
```

```bash
curl -X POST "http://localhost:8123/order-events" ^
  -H "Content-Type: application/json" ^
  -d "{\"orderNo\":\"ORD202603120001\",\"orderStatus\":\"PAID\"}"
```

```bash
curl -X POST "http://localhost:8123/order-events" ^
  -H "Content-Type: application/json" ^
  -d "{\"orderNo\":\"ORD202603120001\",\"orderStatus\":\"SHIPPED\"}"
```

```bash
curl -X POST "http://localhost:8123/order-events" ^
  -H "Content-Type: application/json" ^
  -d "{\"orderNo\":\"ORD202603120001\",\"orderStatus\":\"FINISHED\"}"
```

这几段命令的作用：

- 连续发送同一个 `orderNo` 的不同状态
- 用来验证这些消息是否进入同一分区并按顺序被消费

**Step 4: 查询这个订单号的消费日志**

Run:

```bash
curl "http://localhost:8123/order-events/ORD202603120001"
```

这条命令的作用：

- 查看消费后的状态顺序
- 查看每条消息对应的分区号和偏移量

Expected:

- `CREATED -> PAID -> SHIPPED -> FINISHED` 顺序应保持一致
- 同一个 `orderNo` 的 `kafkaPartition` 一般相同

---

### Task 5: 常见错误排查

**问题 1：为什么要把 `orderNo` 当作消息 Key**

原因：

- Kafka 会根据 Key 做分区路由
- 相同 Key 的消息会进入同一个分区
- 同一个分区内消息是有序的

**问题 2：为什么全局顺序很难保证**

原因：

- Kafka 的有序性通常是“分区内有序”
- 一旦 Topic 有多个分区，不同分区之间并不保证全局顺序

**问题 3：为什么这里把 Topic 设置成 3 个分区**

原因：

- 如果只有 1 个分区，你看不出“Key 决定分区”的意义
- 设置多个分区后，这个知识点才更容易理解

---

## 你做完这个项目后应该掌握什么

1. Kafka 的消息 Key 是干什么的
2. 为什么相同 Key 的消息通常会进入同一分区
3. Kafka 里的“有序”一般指什么范围内的有序
4. 为什么很多订单类业务会特别关注消息顺序
