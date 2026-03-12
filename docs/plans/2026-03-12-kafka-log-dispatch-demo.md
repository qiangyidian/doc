# kafka-log-dispatch-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot + Kafka 项目，完成“业务操作日志发送到一个 Topic，并被两个不同 Consumer Group 分别消费”的练习模块，让初学者理解 Kafka 的 Consumer Group 语义。

**Architecture:** 这个项目的核心链路是 `Controller -> Service -> MySQL -> Kafka Producer -> Topic -> 多个 Consumer Group -> MySQL`。同一条日志事件会被不同组分别消费，审计组写审计表，告警组写告警表，这正是 Kafka 很典型的事件分发场景。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Web, Spring for Apache Kafka, Spring Data JPA, MySQL 8.0, Apache Kafka Docker Image, Docker Compose, Maven, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 Kafka 的 Consumer Group

很多系统里，一条业务事件并不只给一个下游系统使用。  
比如一条“用户执行了敏感操作”的日志，可能同时被：

- 审计系统消费
- 告警系统消费
- 数据分析系统消费

Kafka 的 Consumer Group 很适合这个场景：

- 同一组里，一条消息只会被组内一个消费者消费
- 不同组之间，都会各自收到这条消息

---

## 二、最终目录结构

```text
kafka-log-dispatch-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/kafkalogdispatch
│   │   │   ├── KafkaLogDispatchApplication.java
│   │   │   ├── common
│   │   │   │   ├── Constants.java
│   │   │   │   └── Result.java
│   │   │   ├── controller
│   │   │   │   └── LogController.java
│   │   │   ├── dto
│   │   │   │   ├── LogDispatchResponse.java
│   │   │   │   └── SendLogRequest.java
│   │   │   ├── entity
│   │   │   │   ├── AlertLog.java
│   │   │   │   ├── AuditLog.java
│   │   │   │   └── OperationLog.java
│   │   │   ├── kafka
│   │   │   │   ├── AlertLogConsumer.java
│   │   │   │   ├── KafkaTopicConfig.java
│   │   │   │   ├── OperationLogEvent.java
│   │   │   │   ├── OperationLogProducer.java
│   │   │   │   └── AuditLogConsumer.java
│   │   │   ├── repository
│   │   │   │   ├── AlertLogRepository.java
│   │   │   │   ├── AuditLogRepository.java
│   │   │   │   └── OperationLogRepository.java
│   │   │   └── service
│   │   │       ├── LogService.java
│   │   │       └── impl
│   │   │           └── LogServiceImpl.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── data.sql
│   │       └── schema.sql
│   └── test
│       └── java/com/example/kafkalogdispatch/KafkaLogDispatchApplicationTests.java
```

这段目录结构的作用：

- 把“原始日志”、“审计日志”、“告警日志”三类数据分开
- 把两个不同消费者拆成独立类，便于你观察 Consumer Group 的差异

---

## 三、端口规划

- 应用端口：`8122`
- MySQL：`3320`
- Kafka：`9094`

这段端口规划的作用：

- 避免和其它 Kafka 练习项目冲突
- 方便同一台机器同时保留多个练习工程

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/kafkalogdispatch/KafkaLogDispatchApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/kafkalogdispatch/KafkaLogDispatchApplicationTests.java`

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
    <artifactId>kafka-log-dispatch-demo</artifactId>
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

- 给项目引入 Web、Kafka、JPA、MySQL 这些核心依赖
- 这一套依赖已经足够支撑“业务入库 + Kafka 分发 + 多消费者落库”

**Step 2: 创建 `docker-compose.yml`**

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: kafka-log-dispatch-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: kafka_log_dispatch_db
    ports:
      - "3320:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - kafka-log-dispatch-mysql-data:/var/lib/mysql

  kafka:
    image: apache/kafka:latest
    container_name: kafka-log-dispatch-broker
    restart: always
    ports:
      - "9094:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9094
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0

volumes:
  kafka-log-dispatch-mysql-data:
```

这段代码的作用：

- 启动 Kafka 单机 Broker，给日志事件提供消息通道
- 启动 MySQL，用来分别保存原始日志、审计日志和告警日志

**Step 3: 创建启动类 `KafkaLogDispatchApplication.java`**

```java
package com.example.kafkalogdispatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KafkaLogDispatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaLogDispatchApplication.class, args);
    }
}
```

这段代码的作用：

- 启动整个 Spring Boot 项目
- 是 Kafka、JPA、Web 等能力装配的入口

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8122

spring:
  application:
    name: kafka-log-dispatch-demo

  datasource:
    url: jdbc:mysql://localhost:3320/kafka_log_dispatch_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
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
    bootstrap-servers: localhost:9094
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
        spring.json.value.default.type: com.example.kafkalogdispatch.kafka.OperationLogEvent
```

这段代码的作用：

- 配置数据库和 Kafka 地址
- 配置 Kafka 的 JSON 序列化与反序列化
- 由于这个项目会有多个 Consumer Group，所以这里不把 group-id 写死在全局配置里

**Step 5: 创建 `schema.sql`**

```sql
DROP TABLE IF EXISTS t_alert_log;
DROP TABLE IF EXISTS t_audit_log;
DROP TABLE IF EXISTS t_operation_log;

CREATE TABLE t_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    operator_name VARCHAR(64) NOT NULL COMMENT '操作人',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    log_level VARCHAR(16) NOT NULL COMMENT '日志级别',
    content VARCHAR(255) NOT NULL COMMENT '日志内容',
    create_time DATETIME NOT NULL COMMENT '创建时间'
);

CREATE TABLE t_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    operator_name VARCHAR(64) NOT NULL COMMENT '操作人',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    content VARCHAR(255) NOT NULL COMMENT '内容',
    create_time DATETIME NOT NULL COMMENT '创建时间'
);

CREATE TABLE t_alert_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    log_level VARCHAR(16) NOT NULL COMMENT '日志级别',
    content VARCHAR(255) NOT NULL COMMENT '内容',
    alert_status VARCHAR(32) NOT NULL COMMENT '告警状态',
    create_time DATETIME NOT NULL COMMENT '创建时间'
);
```

这段代码的作用：

- `t_operation_log` 保存原始业务日志
- `t_audit_log` 模拟审计系统消费结果
- `t_alert_log` 模拟告警系统消费结果

**Step 6: 创建 `data.sql`**

```sql
-- 当前项目不需要初始化数据，直接走发送日志接口测试即可。
```

这段代码的作用：

- 明确告诉你这个项目的入口就是发送日志接口
- 启动后不用依赖预置数据

**Step 7: 创建测试类 `KafkaLogDispatchApplicationTests.java`**

```java
package com.example.kafkalogdispatch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class KafkaLogDispatchApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

这段代码的作用：

- 只验证应用能不能正常启动
- 是新项目最基础的检查点

---

### Task 2: 创建通用类、实体、Repository、DTO、事件对象

**Files:**
- Create: `src/main/java/com/example/kafkalogdispatch/common/Constants.java`
- Create: `src/main/java/com/example/kafkalogdispatch/common/Result.java`
- Create: `src/main/java/com/example/kafkalogdispatch/entity/OperationLog.java`
- Create: `src/main/java/com/example/kafkalogdispatch/entity/AuditLog.java`
- Create: `src/main/java/com/example/kafkalogdispatch/entity/AlertLog.java`
- Create: `src/main/java/com/example/kafkalogdispatch/repository/OperationLogRepository.java`
- Create: `src/main/java/com/example/kafkalogdispatch/repository/AuditLogRepository.java`
- Create: `src/main/java/com/example/kafkalogdispatch/repository/AlertLogRepository.java`
- Create: `src/main/java/com/example/kafkalogdispatch/dto/SendLogRequest.java`
- Create: `src/main/java/com/example/kafkalogdispatch/dto/LogDispatchResponse.java`
- Create: `src/main/java/com/example/kafkalogdispatch/kafka/OperationLogEvent.java`

**Step 1: 创建 `Constants.java`**

```java
package com.example.kafkalogdispatch.common;

public final class Constants {

    private Constants() {
    }

    public static final String OPERATION_LOG_TOPIC = "operation-log-topic";
    public static final String AUDIT_GROUP = "audit-log-group";
    public static final String ALERT_GROUP = "alert-log-group";
}
```

这段代码的作用：

- 统一管理 Topic 名称和 Consumer Group 名称
- 让 Producer、Consumer 之间的配置保持一致

**Step 2: 创建 `Result.java`**

```java
package com.example.kafkalogdispatch.common;

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

- 统一接口输出结构
- 让你测试接口时更容易看懂返回内容

**Step 3: 创建实体和 Repository**

`OperationLog.java`

```java
package com.example.kafkalogdispatch.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_operation_log")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String operatorName;

    private String operationType;

    private String logLevel;

    private String content;

    private LocalDateTime createTime;
}
```

这段代码的作用：

- 保存系统最原始的业务操作日志
- 发送 Kafka 消息前，先把日志落到主表

`AuditLog.java`

```java
package com.example.kafkalogdispatch.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String operatorName;

    private String operationType;

    private String content;

    private LocalDateTime createTime;
}
```

这段代码的作用：

- 模拟“审计系统消费后的存储结果”
- 你可以通过它看到审计组有没有收到这条消息

`AlertLog.java`

```java
package com.example.kafkalogdispatch.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_alert_log")
public class AlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String logLevel;

    private String content;

    private String alertStatus;

    private LocalDateTime createTime;
}
```

这段代码的作用：

- 模拟“告警系统消费后的结果”
- 你可以通过它看到另一个 Consumer Group 是否也收到了消息

`OperationLogRepository.java`

```java
package com.example.kafkalogdispatch.repository;

import com.example.kafkalogdispatch.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
}
```

这段代码的作用：

- 提供原始日志表的基础持久化能力

`AuditLogRepository.java`

```java
package com.example.kafkalogdispatch.repository;

import com.example.kafkalogdispatch.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByIdDesc();
}
```

这段代码的作用：

- 提供审计日志表的持久化能力
- 增加一个倒序查询方法，方便测试查看最新数据

`AlertLogRepository.java`

```java
package com.example.kafkalogdispatch.repository;

import com.example.kafkalogdispatch.entity.AlertLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {

    List<AlertLog> findAllByOrderByIdDesc();
}
```

这段代码的作用：

- 提供告警日志表的持久化能力
- 用来确认第二个 Consumer Group 是否独立消费成功

**Step 4: 创建 DTO 和事件对象**

`SendLogRequest.java`

```java
package com.example.kafkalogdispatch.dto;

import lombok.Data;

@Data
public class SendLogRequest {

    private String operatorName;

    private String operationType;

    private String logLevel;

    private String content;
}
```

这段代码的作用：

- 表示发送日志接口的请求体
- 用来承接前端或 curl 传进来的日志内容

`LogDispatchResponse.java`

```java
package com.example.kafkalogdispatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LogDispatchResponse {

    private Long logId;

    private String message;
}
```

这段代码的作用：

- 表示发送日志成功后的响应
- 给调用方一个明确的结果反馈

`OperationLogEvent.java`

```java
package com.example.kafkalogdispatch.kafka;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogEvent {

    private String operatorName;

    private String operationType;

    private String logLevel;

    private String content;

    private LocalDateTime eventTime;
}
```

这段代码的作用：

- 这是发到 Kafka Topic 中的日志事件结构
- 两个 Consumer Group 都会接收并处理这个对象

---

### Task 3: 创建 Kafka 组件、Service 和 Controller

**Files:**
- Create: `src/main/java/com/example/kafkalogdispatch/kafka/KafkaTopicConfig.java`
- Create: `src/main/java/com/example/kafkalogdispatch/kafka/OperationLogProducer.java`
- Create: `src/main/java/com/example/kafkalogdispatch/kafka/AuditLogConsumer.java`
- Create: `src/main/java/com/example/kafkalogdispatch/kafka/AlertLogConsumer.java`
- Create: `src/main/java/com/example/kafkalogdispatch/service/LogService.java`
- Create: `src/main/java/com/example/kafkalogdispatch/service/impl/LogServiceImpl.java`
- Create: `src/main/java/com/example/kafkalogdispatch/controller/LogController.java`

**Step 1: 创建 Topic 配置和 Producer**

`KafkaTopicConfig.java`

```java
package com.example.kafkalogdispatch.kafka;

import com.example.kafkalogdispatch.common.Constants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic operationLogTopic() {
        return new NewTopic(Constants.OPERATION_LOG_TOPIC, 1, (short) 1);
    }
}
```

这段代码的作用：

- 自动创建日志 Topic
- 保证启动项目后，Producer 和 Consumer 有可用的 Topic

`OperationLogProducer.java`

```java
package com.example.kafkalogdispatch.kafka;

import com.example.kafkalogdispatch.common.Constants;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OperationLogProducer {

    private final KafkaTemplate<String, OperationLogEvent> kafkaTemplate;

    public OperationLogProducer(KafkaTemplate<String, OperationLogEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(OperationLogEvent event) {
        kafkaTemplate.send(Constants.OPERATION_LOG_TOPIC, event.getOperationType(), event);
    }
}
```

这段代码的作用：

- 把日志事件发送到 Kafka
- 这里用 `operationType` 做消息 Key，只是为了让消息更方便追踪

**Step 2: 创建两个不同的 Consumer**

`AuditLogConsumer.java`

```java
package com.example.kafkalogdispatch.kafka;

import com.example.kafkalogdispatch.common.Constants;
import com.example.kafkalogdispatch.entity.AuditLog;
import com.example.kafkalogdispatch.repository.AuditLogRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditLogConsumer {

    private final AuditLogRepository auditLogRepository;

    public AuditLogConsumer(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @KafkaListener(topics = Constants.OPERATION_LOG_TOPIC, groupId = Constants.AUDIT_GROUP)
    public void listen(OperationLogEvent event) {
        AuditLog auditLog = new AuditLog();
        auditLog.setOperatorName(event.getOperatorName());
        auditLog.setOperationType(event.getOperationType());
        auditLog.setContent(event.getContent());
        auditLog.setCreateTime(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }
}
```

这段代码的作用：

- 模拟“审计系统”作为一个独立的 Consumer Group
- 它会消费同一个 Topic，并写入自己的审计表

`AlertLogConsumer.java`

```java
package com.example.kafkalogdispatch.kafka;

import com.example.kafkalogdispatch.common.Constants;
import com.example.kafkalogdispatch.entity.AlertLog;
import com.example.kafkalogdispatch.repository.AlertLogRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AlertLogConsumer {

    private final AlertLogRepository alertLogRepository;

    public AlertLogConsumer(AlertLogRepository alertLogRepository) {
        this.alertLogRepository = alertLogRepository;
    }

    @KafkaListener(topics = Constants.OPERATION_LOG_TOPIC, groupId = Constants.ALERT_GROUP)
    public void listen(OperationLogEvent event) {
        AlertLog alertLog = new AlertLog();
        alertLog.setLogLevel(event.getLogLevel());
        alertLog.setContent(event.getContent());
        alertLog.setAlertStatus(
                ("ERROR".equalsIgnoreCase(event.getLogLevel()) || "WARN".equalsIgnoreCase(event.getLogLevel()))
                        ? "NEED_ALERT"
                        : "IGNORE"
        );
        alertLog.setCreateTime(LocalDateTime.now());
        alertLogRepository.save(alertLog);
    }
}
```

这段代码的作用：

- 模拟“告警系统”作为另一个独立的 Consumer Group
- 它也会收到同一条消息，但处理逻辑与审计组不同

**Step 3: 创建 Service 和 Controller**

`LogService.java`

```java
package com.example.kafkalogdispatch.service;

import com.example.kafkalogdispatch.dto.LogDispatchResponse;
import com.example.kafkalogdispatch.dto.SendLogRequest;
import com.example.kafkalogdispatch.entity.AlertLog;
import com.example.kafkalogdispatch.entity.AuditLog;

import java.util.List;

public interface LogService {

    LogDispatchResponse sendLog(SendLogRequest request);

    List<AuditLog> listAuditLogs();

    List<AlertLog> listAlertLogs();
}
```

这段代码的作用：

- 定义日志模块对外提供的服务能力
- 让 Controller 只负责接口，业务逻辑统一放到 Service

`LogServiceImpl.java`

```java
package com.example.kafkalogdispatch.service.impl;

import com.example.kafkalogdispatch.dto.LogDispatchResponse;
import com.example.kafkalogdispatch.dto.SendLogRequest;
import com.example.kafkalogdispatch.entity.AlertLog;
import com.example.kafkalogdispatch.entity.AuditLog;
import com.example.kafkalogdispatch.entity.OperationLog;
import com.example.kafkalogdispatch.kafka.OperationLogEvent;
import com.example.kafkalogdispatch.kafka.OperationLogProducer;
import com.example.kafkalogdispatch.repository.AlertLogRepository;
import com.example.kafkalogdispatch.repository.AuditLogRepository;
import com.example.kafkalogdispatch.repository.OperationLogRepository;
import com.example.kafkalogdispatch.service.LogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogServiceImpl implements LogService {

    private final OperationLogRepository operationLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final AlertLogRepository alertLogRepository;
    private final OperationLogProducer operationLogProducer;

    public LogServiceImpl(OperationLogRepository operationLogRepository,
                          AuditLogRepository auditLogRepository,
                          AlertLogRepository alertLogRepository,
                          OperationLogProducer operationLogProducer) {
        this.operationLogRepository = operationLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.alertLogRepository = alertLogRepository;
        this.operationLogProducer = operationLogProducer;
    }

    @Override
    public LogDispatchResponse sendLog(SendLogRequest request) {
        OperationLog operationLog = new OperationLog();
        operationLog.setOperatorName(request.getOperatorName());
        operationLog.setOperationType(request.getOperationType());
        operationLog.setLogLevel(request.getLogLevel());
        operationLog.setContent(request.getContent());
        operationLog.setCreateTime(LocalDateTime.now());
        operationLogRepository.save(operationLog);

        OperationLogEvent event = new OperationLogEvent();
        event.setOperatorName(request.getOperatorName());
        event.setOperationType(request.getOperationType());
        event.setLogLevel(request.getLogLevel());
        event.setContent(request.getContent());
        event.setEventTime(LocalDateTime.now());
        operationLogProducer.send(event);

        return new LogDispatchResponse(operationLog.getId(), "日志已发送到 Kafka");
    }

    @Override
    public List<AuditLog> listAuditLogs() {
        return auditLogRepository.findAllByOrderByIdDesc();
    }

    @Override
    public List<AlertLog> listAlertLogs() {
        return alertLogRepository.findAllByOrderByIdDesc();
    }
}
```

这段代码的作用：

- 先保存原始日志
- 再发送 Kafka 事件
- 然后通过两个不同 Consumer Group，把一条消息分别派发给两个不同下游

`LogController.java`

```java
package com.example.kafkalogdispatch.controller;

import com.example.kafkalogdispatch.common.Result;
import com.example.kafkalogdispatch.dto.LogDispatchResponse;
import com.example.kafkalogdispatch.dto.SendLogRequest;
import com.example.kafkalogdispatch.entity.AlertLog;
import com.example.kafkalogdispatch.entity.AuditLog;
import com.example.kafkalogdispatch.service.LogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping("/send")
    public Result<LogDispatchResponse> send(@RequestBody SendLogRequest request) {
        return Result.success(logService.sendLog(request));
    }

    @GetMapping("/audit")
    public Result<List<AuditLog>> auditLogs() {
        return Result.success(logService.listAuditLogs());
    }

    @GetMapping("/alert")
    public Result<List<AlertLog>> alertLogs() {
        return Result.success(logService.listAlertLogs());
    }
}
```

这段代码的作用：

- 提供一个发送日志接口
- 提供两个查询接口，用来分别查看审计组和告警组的消费结果

---

### Task 4: 启动项目并验证 Consumer Group 语义

**Step 1: 启动中间件**

Run:

```bash
docker compose up -d
```

这条命令的作用：

- 启动 MySQL 和 Kafka
- 给应用提供运行依赖

**Step 2: 启动应用**

Run:

```bash
mvn spring-boot:run
```

这条命令的作用：

- 启动 Spring Boot
- 让 Producer 和两个 Consumer 都进入工作状态

**Step 3: 发送一条操作日志**

Run:

```bash
curl -X POST "http://localhost:8122/logs/send" ^
  -H "Content-Type: application/json" ^
  -d "{\"operatorName\":\"zhangsan\",\"operationType\":\"DELETE_USER\",\"logLevel\":\"ERROR\",\"content\":\"删除用户失败，权限不足\"}"
```

这条命令的作用：

- 发送一条高风险业务日志
- 用这条消息验证“同一条消息被两个不同组分别消费”

**Step 4: 查看审计日志表**

Run:

```bash
curl "http://localhost:8122/logs/audit"
```

这条命令的作用：

- 验证审计组是否收到了日志事件

**Step 5: 查看告警日志表**

Run:

```bash
curl "http://localhost:8122/logs/alert"
```

这条命令的作用：

- 验证告警组是否也收到了同一条日志事件
- 如果两边都有记录，就说明不同 Consumer Group 都各自消费到了消息

---

### Task 5: 常见错误排查

**问题 1：为什么两个消费者都能收到同一条消息**

原因：

- 它们属于两个不同的 Consumer Group
- Kafka 会把同一条消息分别提供给每个组

**问题 2：如果两个消费者在同一个组里会怎样**

说明：

- 同组内一条消息只会被其中一个消费者处理
- 这就是 Kafka 用来做“组内分摊消费”的机制

**问题 3：为什么这个场景比 RabbitMQ 更适合理解 Consumer Group**

原因：

- Consumer Group 是 Kafka 的核心概念之一
- 它天然适合做“一个 Topic 多个业务组分别处理”的模型

---

## 你做完这个项目后应该掌握什么

1. Consumer Group 到底是什么意思
2. 为什么不同组都能收到同一条消息
3. 为什么日志分发这类场景很适合 Kafka
4. Kafka 在“一个事件，多系统订阅”里的价值是什么
