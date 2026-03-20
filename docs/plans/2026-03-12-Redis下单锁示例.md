# redis-order-lock-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot 项目，完成“使用 Redis 分布式锁防止重复下单”的练习模块，让初学者理解 Redis 在防重提交和并发控制里的常见用法。

**Architecture:** 这个项目的核心链路是 `Controller -> OrderService -> Redis Lock -> MySQL`。提交订单前先尝试从 Redis 获取短期锁，拿到锁才允许继续创建订单；业务完成后通过 Lua 脚本安全释放锁，避免误删别人的锁。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Maven, Spring Web, Spring Data Redis, MyBatis-Plus 3.5.15, MySQL 8.0, Redis 7.2, Docker Compose, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 Redis

有些接口很容易被用户重复点击，比如：

- 下单按钮
- 支付按钮
- 抢购按钮

如果后端没有保护，可能会出现：

- 一次点击生成多笔订单
- 同一个用户短时间重复提交

Redis 常用于做“短时间互斥锁”：

1. 请求进来先尝试加锁
2. 加锁成功才继续执行业务
3. 业务结束后释放锁

这样可以很直观地防止重复提交。

---

## 二、最终目录结构

```text
redis-order-lock-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/orderlock
│   │   │   ├── OrderLockApplication.java
│   │   │   ├── common
│   │   │   │   ├── Constants.java
│   │   │   │   └── Result.java
│   │   │   ├── controller
│   │   │   │   └── OrderController.java
│   │   │   ├── dto
│   │   │   │   ├── OrderResponse.java
│   │   │   │   └── SubmitOrderRequest.java
│   │   │   ├── entity
│   │   │   │   └── LockOrder.java
│   │   │   ├── mapper
│   │   │   │   └── LockOrderMapper.java
│   │   │   └── service
│   │   │       ├── OrderService.java
│   │   │       └── impl
│   │   │           └── OrderServiceImpl.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── data.sql
│   │       └── schema.sql
│   └── test
│       └── java/com/example/orderlock/OrderLockApplicationTests.java
```

---

## 三、端口规划

- 应用端口：`8093`
- MySQL：`3312`
- Redis：`6382`

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/orderlock/OrderLockApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/orderlock/OrderLockApplicationTests.java`

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
    <artifactId>redis-order-lock-demo</artifactId>
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
            <artifactId>spring-boot-starter-data-redis</artifactId>
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
    container_name: redis-order-lock-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: order_lock_db
    ports:
      - "3312:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - redis-order-lock-mysql-data:/var/lib/mysql

  redis:
    image: redis:7.2
    container_name: redis-order-lock-redis
    restart: always
    ports:
      - "6382:6379"
    command: ["redis-server", "--appendonly", "yes"]
    volumes:
      - redis-order-lock-redis-data:/data

volumes:
  redis-order-lock-mysql-data:
  redis-order-lock-redis-data:
```

**Step 3: 创建启动类 `OrderLockApplication.java`**

```java
package com.example.orderlock;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.orderlock.mapper")
public class OrderLockApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderLockApplication.class, args);
    }
}
```

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8093

spring:
  application:
    name: redis-order-lock-demo

  datasource:
    url: jdbc:mysql://localhost:3312/order_lock_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6382

  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

**Step 5: 创建 `schema.sql`**

```sql
DROP TABLE IF EXISTS t_lock_order;

CREATE TABLE t_lock_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    amount DECIMAL(10, 2) NOT NULL COMMENT '订单金额',
    status VARCHAR(32) NOT NULL COMMENT '订单状态',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
);
```

**Step 6: 创建 `data.sql`**

```sql
-- 当前项目不需要初始化订单数据，这里保留空文件即可。
```

**Step 7: 创建测试类 `OrderLockApplicationTests.java`**

```java
package com.example.orderlock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderLockApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

---

### Task 2: 创建通用类、实体类、Mapper 和 DTO

**Files:**
- Create: `src/main/java/com/example/orderlock/common/Constants.java`
- Create: `src/main/java/com/example/orderlock/common/Result.java`
- Create: `src/main/java/com/example/orderlock/entity/LockOrder.java`
- Create: `src/main/java/com/example/orderlock/mapper/LockOrderMapper.java`
- Create: `src/main/java/com/example/orderlock/dto/SubmitOrderRequest.java`
- Create: `src/main/java/com/example/orderlock/dto/OrderResponse.java`

**Step 1: 创建 `Constants.java`**

```java
package com.example.orderlock.common;

public final class Constants {

    private Constants() {
    }

    public static final String ORDER_LOCK_PREFIX = "order:submit:lock:";

    public static final long LOCK_EXPIRE_SECONDS = 10L;

    /**
     * 使用 Lua 安全释放锁：
     * 只有 Redis 中的 value 和当前请求传入的 requestId 一致时，才允许删除。
     * 这样可以避免误删别人的锁。
     */
    public static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else " +
            "return 0 " +
            "end";
}
```

**Step 2: 创建 `Result.java`**

```java
package com.example.orderlock.common;

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

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
}
```

**Step 3: 创建实体类 `LockOrder.java`**

```java
package com.example.orderlock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_lock_order")
public class LockOrder {

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

**Step 4: 创建 Mapper `LockOrderMapper.java`**

```java
package com.example.orderlock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.orderlock.entity.LockOrder;

public interface LockOrderMapper extends BaseMapper<LockOrder> {
}
```

**Step 5: 创建 DTO**

`SubmitOrderRequest.java`

```java
package com.example.orderlock.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubmitOrderRequest {

    private Long userId;

    private String productName;

    private BigDecimal amount;
}
```

`OrderResponse.java`

```java
package com.example.orderlock.dto;

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

---

### Task 3: 创建 Service 和 Controller

**Files:**
- Create: `src/main/java/com/example/orderlock/service/OrderService.java`
- Create: `src/main/java/com/example/orderlock/service/impl/OrderServiceImpl.java`
- Create: `src/main/java/com/example/orderlock/controller/OrderController.java`

**Step 1: 创建 `OrderService.java`**

```java
package com.example.orderlock.service;

import com.example.orderlock.dto.OrderResponse;
import com.example.orderlock.dto.SubmitOrderRequest;
import com.example.orderlock.entity.LockOrder;

import java.util.List;

public interface OrderService {

    OrderResponse submitOrder(SubmitOrderRequest request);

    List<LockOrder> listOrders();
}
```

**Step 2: 创建 `OrderServiceImpl.java`**

```java
package com.example.orderlock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.orderlock.common.Constants;
import com.example.orderlock.dto.OrderResponse;
import com.example.orderlock.dto.SubmitOrderRequest;
import com.example.orderlock.entity.LockOrder;
import com.example.orderlock.mapper.LockOrderMapper;
import com.example.orderlock.service.OrderService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class OrderServiceImpl implements OrderService {

    private final LockOrderMapper lockOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public OrderServiceImpl(LockOrderMapper lockOrderMapper,
                            StringRedisTemplate stringRedisTemplate) {
        this.lockOrderMapper = lockOrderMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse submitOrder(SubmitOrderRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }

        String lockKey = Constants.ORDER_LOCK_PREFIX + request.getUserId();
        String requestId = UUID.randomUUID().toString();

        // 核心动作：使用 setIfAbsent 模拟 SET key value NX EX seconds。
        // - 只有 key 不存在时，才会设置成功
        // - 同时给锁设置过期时间，避免死锁
        Boolean lockSuccess = stringRedisTemplate.opsForValue().setIfAbsent(
                lockKey,
                requestId,
                Constants.LOCK_EXPIRE_SECONDS,
                TimeUnit.SECONDS
        );

        if (Boolean.FALSE.equals(lockSuccess)) {
            throw new IllegalStateException("请勿重复提交订单");
        }

        try {
            LockOrder order = new LockOrder();
            order.setOrderNo(generateOrderNo());
            order.setUserId(request.getUserId());
            order.setProductName(request.getProductName());
            order.setAmount(request.getAmount());
            order.setStatus("CREATED");
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            lockOrderMapper.insert(order);

            return new OrderResponse(order.getId(), order.getOrderNo(), order.getStatus());
        } finally {
            releaseLock(lockKey, requestId);
        }
    }

    @Override
    public List<LockOrder> listOrders() {
        return lockOrderMapper.selectList(
                new LambdaQueryWrapper<LockOrder>().orderByDesc(LockOrder::getId)
        );
    }

    private void releaseLock(String lockKey, String requestId) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(Constants.UNLOCK_LUA);
        redisScript.setResultType(Long.class);

        stringRedisTemplate.execute(
                redisScript,
                Collections.singletonList(lockKey),
                requestId
        );
    }

    private String generateOrderNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "LOCK" + timePart + randomPart;
    }
}
```

**Step 3: 创建控制器 `OrderController.java`**

```java
package com.example.orderlock.controller;

import com.example.orderlock.common.Result;
import com.example.orderlock.dto.OrderResponse;
import com.example.orderlock.dto.SubmitOrderRequest;
import com.example.orderlock.entity.LockOrder;
import com.example.orderlock.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/submit")
    public Result<OrderResponse> submitOrder(@RequestBody SubmitOrderRequest request) {
        try {
            return Result.success(orderService.submitOrder(request));
        } catch (IllegalStateException ex) {
            return Result.fail(ex.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<LockOrder>> listOrders() {
        return Result.success(orderService.listOrders());
    }
}
```

---

### Task 4: 启动项目并验证分布式锁链路

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

**Step 3: 提交一笔订单**

Run:

```bash
curl -X POST "http://localhost:8093/orders/submit" ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\":3001,\"productName\":\"Redis并发训练营\",\"amount\":399.00}"
```

Expected:

- 返回 `code=200`
- 生成一笔订单

**Step 4: 短时间内重复提交**

再次执行同样的请求。

Expected:

- 如果两次请求足够接近，后一次会返回“请勿重复提交订单”
- 说明 Redis 锁已经生效

**Step 5: 查询订单列表**

Run:

```bash
curl "http://localhost:8093/orders/list"
```

Expected:

- 能看到实际成功创建的订单

---

### Task 5: 常见错误排查

**问题 1：为什么要给锁设置过期时间**

原因：

- 如果服务拿到锁后异常退出，锁不能永远不释放
- 过期时间是避免死锁的最基础手段

**问题 2：为什么不能直接 `get` 后再 `delete` 释放锁**

原因：

- 这样不是原子操作
- 可能误删别的请求加上的新锁

所以这里用了 Lua 脚本，把“比对 value”和“删除 key”放到 Redis 里一次完成。

**问题 3：为什么这个项目还不算完整生产锁方案**

原因：

- 这里只是最适合初学者理解的入门实现
- 真正生产里很多团队会用 Redisson
- 更复杂场景还会涉及锁续期、重入、公平锁等能力

---

## 你做完这个项目后应该掌握什么

1. Redis 分布式锁最基础的实现思路是什么
2. 为什么要加过期时间
3. 为什么释放锁需要“比对 value 再删除”
4. Redis 为什么适合做接口防重复提交
