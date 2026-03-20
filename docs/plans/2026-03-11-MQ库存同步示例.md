# mq-stock-sync-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot 项目，完成“扣减库存后，通过 RabbitMQ 异步同步外部系统”的练习模块，让初学者理解“跨系统同步为什么通常要异步做”。

**Architecture:** 这个项目的核心链路是 `Controller -> StockService -> MySQL -> Producer -> RabbitMQ -> Consumer -> ExternalSyncService -> MySQL`。库存扣减是主流程，外部同步是从流程，消费者同样采用手动确认模式。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Maven, Spring Web, Spring AMQP, RabbitMQ, MyBatis-Plus 3.5.15, MySQL 8.0, Docker Compose, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 MQ

库存变更后，常常还需要通知很多下游系统：

- 搜索系统更新库存状态
- 运营后台更新可售状态
- 第三方仓储系统同步库存

如果这些动作都同步执行，库存接口会变慢，而且很容易被外部系统不稳定拖垮。  
所以常见做法是：

1. 先完成库存扣减
2. 再发 MQ 消息
3. 让消费者异步处理外部同步

---

## 二、最终目录结构

```text
mq-stock-sync-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/stocksync
│   │   │   ├── StockSyncApplication.java
│   │   │   ├── common
│   │   │   │   ├── Constants.java
│   │   │   │   └── Result.java
│   │   │   ├── config
│   │   │   │   └── RabbitMqConfig.java
│   │   │   ├── controller
│   │   │   │   └── StockController.java
│   │   │   ├── dto
│   │   │   │   ├── CreateStockRequest.java
│   │   │   │   ├── DeductStockRequest.java
│   │   │   │   └── StockResponse.java
│   │   │   ├── entity
│   │   │   │   ├── ProductStock.java
│   │   │   │   └── StockSyncLog.java
│   │   │   ├── mapper
│   │   │   │   ├── ProductStockMapper.java
│   │   │   │   └── StockSyncLogMapper.java
│   │   │   ├── mq
│   │   │   │   ├── StockChangeConsumer.java
│   │   │   │   ├── StockChangeMessage.java
│   │   │   │   └── StockChangeProducer.java
│   │   │   └── service
│   │   │       ├── ExternalSyncService.java
│   │   │       ├── StockService.java
│   │   │       └── impl
│   │   │           ├── ExternalSyncServiceImpl.java
│   │   │           └── StockServiceImpl.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── data.sql
│   │       └── schema.sql
│   └── test
│       └── java/com/example/stocksync/StockSyncApplicationTests.java
```

---

## 三、端口规划

- 应用端口：`8083`
- MySQL：`3309`
- RabbitMQ AMQP：`5675`
- RabbitMQ 控制台：`15675`

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/stocksync/StockSyncApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/stocksync/StockSyncApplicationTests.java`

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
    <artifactId>mq-stock-sync-demo</artifactId>
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
    container_name: stock-sync-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: stock_sync_db
    ports:
      - "3309:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - stock-sync-mysql-data:/var/lib/mysql

  rabbitmq:
    image: rabbitmq:3-management
    container_name: stock-sync-rabbitmq
    restart: always
    ports:
      - "5675:5672"
      - "15675:15672"
    volumes:
      - stock-sync-rabbitmq-data:/var/lib/rabbitmq

volumes:
  stock-sync-mysql-data:
  stock-sync-rabbitmq-data:
```

**Step 3: 创建启动类 `StockSyncApplication.java`**

```java
package com.example.stocksync;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.stocksync.mapper")
public class StockSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockSyncApplication.class, args);
    }
}
```

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8083

spring:
  application:
    name: mq-stock-sync-demo

  datasource:
    url: jdbc:mysql://localhost:3309/stock_sync_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
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
    port: 5675
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
DROP TABLE IF EXISTS t_stock_sync_log;
DROP TABLE IF EXISTS t_product_stock;

CREATE TABLE t_product_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_no VARCHAR(64) NOT NULL COMMENT '商品编号',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    stock_count INT NOT NULL COMMENT '库存数量',
    status VARCHAR(32) NOT NULL COMMENT '状态',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
);

CREATE TABLE t_stock_sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_no VARCHAR(64) NOT NULL COMMENT '商品编号',
    change_count INT NOT NULL COMMENT '本次变更数量',
    sync_status VARCHAR(32) NOT NULL COMMENT '同步状态',
    target_system VARCHAR(64) NOT NULL COMMENT '目标系统',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL COMMENT '创建时间'
);
```

**Step 6: 创建 `data.sql`**

```sql
INSERT INTO t_product_stock (product_no, product_name, stock_count, status, create_time, update_time)
VALUES ('SKU_INIT_001', '初始化商品', 50, 'ENABLE', NOW(), NOW());
```

**Step 7: 创建测试类 `StockSyncApplicationTests.java`**

```java
package com.example.stocksync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StockSyncApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

---

### Task 2: 创建通用类、实体类、Mapper 和 DTO

**Files:**
- Create: `src/main/java/com/example/stocksync/common/Constants.java`
- Create: `src/main/java/com/example/stocksync/common/Result.java`
- Create: `src/main/java/com/example/stocksync/entity/ProductStock.java`
- Create: `src/main/java/com/example/stocksync/entity/StockSyncLog.java`
- Create: `src/main/java/com/example/stocksync/mapper/ProductStockMapper.java`
- Create: `src/main/java/com/example/stocksync/mapper/StockSyncLogMapper.java`
- Create: `src/main/java/com/example/stocksync/dto/CreateStockRequest.java`
- Create: `src/main/java/com/example/stocksync/dto/DeductStockRequest.java`
- Create: `src/main/java/com/example/stocksync/dto/StockResponse.java`

**Step 1: 创建 `Constants.java`**

```java
package com.example.stocksync.common;

public final class Constants {

    private Constants() {
    }

    public static final String STOCK_SYNC_EXCHANGE = "stock.sync.exchange";
    public static final String STOCK_SYNC_QUEUE = "stock.sync.queue";
    public static final String STOCK_SYNC_ROUTING_KEY = "stock.sync.routing";
}
```

**Step 2: 创建 `Result.java`**

```java
package com.example.stocksync.common;

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

`ProductStock.java`

```java
package com.example.stocksync.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_product_stock")
public class ProductStock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productNo;

    private String productName;

    private Integer stockCount;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

`StockSyncLog.java`

```java
package com.example.stocksync.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_stock_sync_log")
public class StockSyncLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productNo;

    private Integer changeCount;

    private String syncStatus;

    private String targetSystem;

    private String remark;

    private LocalDateTime createTime;
}
```

**Step 4: 创建 Mapper**

`ProductStockMapper.java`

```java
package com.example.stocksync.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stocksync.entity.ProductStock;

public interface ProductStockMapper extends BaseMapper<ProductStock> {
}
```

`StockSyncLogMapper.java`

```java
package com.example.stocksync.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.stocksync.entity.StockSyncLog;

public interface StockSyncLogMapper extends BaseMapper<StockSyncLog> {
}
```

**Step 5: 创建 DTO**

`CreateStockRequest.java`

```java
package com.example.stocksync.dto;

import lombok.Data;

@Data
public class CreateStockRequest {

    private String productNo;

    private String productName;

    private Integer stockCount;
}
```

`DeductStockRequest.java`

```java
package com.example.stocksync.dto;

import lombok.Data;

@Data
public class DeductStockRequest {

    private String productNo;

    private Integer deductCount;
}
```

`StockResponse.java`

```java
package com.example.stocksync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StockResponse {

    private Long stockId;

    private String productNo;

    private Integer stockCount;
}
```

---

### Task 3: 创建 Service 和 Controller

**Files:**
- Create: `src/main/java/com/example/stocksync/service/StockService.java`
- Create: `src/main/java/com/example/stocksync/service/ExternalSyncService.java`
- Create: `src/main/java/com/example/stocksync/service/impl/StockServiceImpl.java`
- Create: `src/main/java/com/example/stocksync/service/impl/ExternalSyncServiceImpl.java`
- Create: `src/main/java/com/example/stocksync/controller/StockController.java`

**Step 1: 创建 `StockService.java`**

```java
package com.example.stocksync.service;

import com.example.stocksync.dto.CreateStockRequest;
import com.example.stocksync.dto.DeductStockRequest;
import com.example.stocksync.dto.StockResponse;
import com.example.stocksync.entity.StockSyncLog;

import java.util.List;

public interface StockService {

    StockResponse createStock(CreateStockRequest request);

    StockResponse deductStock(DeductStockRequest request);

    List<StockSyncLog> listSyncLogs();
}
```

**Step 2: 创建 `ExternalSyncService.java`**

```java
package com.example.stocksync.service;

import com.example.stocksync.mq.StockChangeMessage;

public interface ExternalSyncService {

    void syncToExternalSystem(StockChangeMessage message);
}
```

**Step 3: 创建 `StockServiceImpl.java`**

```java
package com.example.stocksync.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.stocksync.dto.CreateStockRequest;
import com.example.stocksync.dto.DeductStockRequest;
import com.example.stocksync.dto.StockResponse;
import com.example.stocksync.entity.ProductStock;
import com.example.stocksync.entity.StockSyncLog;
import com.example.stocksync.mapper.ProductStockMapper;
import com.example.stocksync.mapper.StockSyncLogMapper;
import com.example.stocksync.mq.StockChangeMessage;
import com.example.stocksync.mq.StockChangeProducer;
import com.example.stocksync.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockServiceImpl implements StockService {

    private final ProductStockMapper productStockMapper;
    private final StockSyncLogMapper stockSyncLogMapper;
    private final StockChangeProducer stockChangeProducer;

    public StockServiceImpl(ProductStockMapper productStockMapper,
                            StockSyncLogMapper stockSyncLogMapper,
                            StockChangeProducer stockChangeProducer) {
        this.productStockMapper = productStockMapper;
        this.stockSyncLogMapper = stockSyncLogMapper;
        this.stockChangeProducer = stockChangeProducer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockResponse createStock(CreateStockRequest request) {
        ProductStock stock = new ProductStock();
        stock.setProductNo(request.getProductNo());
        stock.setProductName(request.getProductName());
        stock.setStockCount(request.getStockCount());
        stock.setStatus("ENABLE");
        stock.setCreateTime(LocalDateTime.now());
        stock.setUpdateTime(LocalDateTime.now());
        productStockMapper.insert(stock);
        return new StockResponse(stock.getId(), stock.getProductNo(), stock.getStockCount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StockResponse deductStock(DeductStockRequest request) {
        ProductStock stock = productStockMapper.selectOne(
                new LambdaQueryWrapper<ProductStock>()
                        .eq(ProductStock::getProductNo, request.getProductNo())
                        .last("limit 1")
        );

        if (stock == null) {
            throw new IllegalArgumentException("库存商品不存在");
        }
        if (request.getDeductCount() == null || request.getDeductCount() <= 0) {
            throw new IllegalArgumentException("扣减数量必须大于0");
        }
        if (stock.getStockCount() < request.getDeductCount()) {
            throw new IllegalArgumentException("库存不足");
        }

        int remainingStock = stock.getStockCount() - request.getDeductCount();
        stock.setStockCount(remainingStock);
        stock.setUpdateTime(LocalDateTime.now());
        productStockMapper.updateById(stock);

        StockChangeMessage message = new StockChangeMessage();
        message.setStockId(stock.getId());
        message.setProductNo(stock.getProductNo());
        message.setProductName(stock.getProductName());
        message.setChangeCount(request.getDeductCount());
        message.setRemainingStock(remainingStock);
        message.setMessageTime(LocalDateTime.now());

        // 主业务只负责扣减库存。
        // 外部系统同步通过消息异步处理。
        stockChangeProducer.send(message);

        return new StockResponse(stock.getId(), stock.getProductNo(), stock.getStockCount());
    }

    @Override
    public List<StockSyncLog> listSyncLogs() {
        return stockSyncLogMapper.selectList(
                new LambdaQueryWrapper<StockSyncLog>().orderByDesc(StockSyncLog::getId)
        );
    }
}
```

**Step 4: 创建 `ExternalSyncServiceImpl.java`**

```java
package com.example.stocksync.service.impl;

import com.example.stocksync.entity.StockSyncLog;
import com.example.stocksync.mapper.StockSyncLogMapper;
import com.example.stocksync.mq.StockChangeMessage;
import com.example.stocksync.service.ExternalSyncService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ExternalSyncServiceImpl implements ExternalSyncService {

    private final StockSyncLogMapper stockSyncLogMapper;

    public ExternalSyncServiceImpl(StockSyncLogMapper stockSyncLogMapper) {
        this.stockSyncLogMapper = stockSyncLogMapper;
    }

    @Override
    public void syncToExternalSystem(StockChangeMessage message) {
        StockSyncLog log = new StockSyncLog();
        log.setProductNo(message.getProductNo());
        log.setChangeCount(message.getChangeCount());
        log.setSyncStatus("SUCCESS");
        log.setTargetSystem("WAREHOUSE_SYSTEM");
        log.setRemark("模拟同步外部库存系统成功，剩余库存：" + message.getRemainingStock());
        log.setCreateTime(LocalDateTime.now());
        stockSyncLogMapper.insert(log);
    }
}
```

**Step 5: 创建控制器 `StockController.java`**

```java
package com.example.stocksync.controller;

import com.example.stocksync.common.Result;
import com.example.stocksync.dto.CreateStockRequest;
import com.example.stocksync.dto.DeductStockRequest;
import com.example.stocksync.dto.StockResponse;
import com.example.stocksync.entity.StockSyncLog;
import com.example.stocksync.service.StockService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping
    public Result<StockResponse> createStock(@RequestBody CreateStockRequest request) {
        return Result.success(stockService.createStock(request));
    }

    @PostMapping("/deduct")
    public Result<StockResponse> deductStock(@RequestBody DeductStockRequest request) {
        return Result.success(stockService.deductStock(request));
    }

    @GetMapping("/sync/logs")
    public Result<List<StockSyncLog>> listSyncLogs() {
        return Result.success(stockService.listSyncLogs());
    }
}
```

---

### Task 4: 创建 MQ 配置、消息体、生产者和消费者

**Files:**
- Create: `src/main/java/com/example/stocksync/config/RabbitMqConfig.java`
- Create: `src/main/java/com/example/stocksync/mq/StockChangeMessage.java`
- Create: `src/main/java/com/example/stocksync/mq/StockChangeProducer.java`
- Create: `src/main/java/com/example/stocksync/mq/StockChangeConsumer.java`

**Step 1: 创建 `RabbitMqConfig.java`**

```java
package com.example.stocksync.config;

import com.example.stocksync.common.Constants;
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
    public DirectExchange stockSyncExchange() {
        return new DirectExchange(Constants.STOCK_SYNC_EXCHANGE, true, false);
    }

    @Bean
    public Queue stockSyncQueue() {
        return new Queue(Constants.STOCK_SYNC_QUEUE, true);
    }

    @Bean
    public Binding stockSyncBinding(Queue stockSyncQueue, DirectExchange stockSyncExchange) {
        return BindingBuilder.bind(stockSyncQueue)
                .to(stockSyncExchange)
                .with(Constants.STOCK_SYNC_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

**Step 2: 创建 `StockChangeMessage.java`**

```java
package com.example.stocksync.mq;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockChangeMessage {

    private Long stockId;

    private String productNo;

    private String productName;

    private Integer changeCount;

    private Integer remainingStock;

    private LocalDateTime messageTime;
}
```

**Step 3: 创建 `StockChangeProducer.java`**

```java
package com.example.stocksync.mq;

import com.example.stocksync.common.Constants;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class StockChangeProducer {

    private final RabbitTemplate rabbitTemplate;

    public StockChangeProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(StockChangeMessage message) {
        rabbitTemplate.convertAndSend(
                Constants.STOCK_SYNC_EXCHANGE,
                Constants.STOCK_SYNC_ROUTING_KEY,
                message
        );
    }
}
```

**Step 4: 创建 `StockChangeConsumer.java`**

```java
package com.example.stocksync.mq;

import com.example.stocksync.common.Constants;
import com.example.stocksync.service.ExternalSyncService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class StockChangeConsumer {

    private final ExternalSyncService externalSyncService;

    public StockChangeConsumer(ExternalSyncService externalSyncService) {
        this.externalSyncService = externalSyncService;
    }

    @RabbitListener(queues = Constants.STOCK_SYNC_QUEUE)
    public void receive(StockChangeMessage message, Message amqpMessage, Channel channel) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();

        try {
            externalSyncService.syncToExternalSystem(message);
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

**Step 3: 新增库存商品**

Run:

```bash
curl -X POST "http://localhost:8083/stocks" ^
  -H "Content-Type: application/json" ^
  -d "{\"productNo\":\"SKU10001\",\"productName\":\"机械键盘\",\"stockCount\":50}"
```

**Step 4: 扣减库存**

Run:

```bash
curl -X POST "http://localhost:8083/stocks/deduct" ^
  -H "Content-Type: application/json" ^
  -d "{\"productNo\":\"SKU10001\",\"deductCount\":3}"
```

Expected:

- 返回新的剩余库存
- 外部同步不会阻塞主接口逻辑

**Step 5: 查询同步日志**

Run:

```bash
curl "http://localhost:8083/stocks/sync/logs"
```

Expected:

- 返回列表中有一条 `syncStatus=SUCCESS`
- 说明库存扣减消息已经被消费者处理

**Step 6: 打开 RabbitMQ 控制台**

地址：

```text
http://localhost:15675
```

---

### Task 6: 常见错误排查

**问题 1：库存扣减成功了，但没有同步日志**

排查：

- `StockServiceImpl#deductStock` 是否调用了 `stockChangeProducer.send`
- 队列名是否为 `stock.sync.queue`
- RabbitMQ 端口是否写成了 `5675`

**问题 2：扣减库存时报“库存不足”**

说明：

- 这是正常业务校验
- 你需要先插入足够库存的数据，或者减少扣减数量

**问题 3：为什么外部同步不直接写在扣减库存的方法里**

原因：

- 外部系统可能很慢
- 外部系统可能不稳定
- 一旦同步接口超时，会直接拖慢库存主流程

这也是 MQ 在生产环境中最常见的价值之一：隔离不稳定的下游依赖。

---

## 你做完这个项目后应该掌握什么

1. 为什么跨系统同步通常适合异步化
2. 为什么库存主流程不应该直接等待外部系统
3. MQ 在“系统解耦”和“失败隔离”里的价值是什么
