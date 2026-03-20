# springboot-web-annotations-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot Web 项目，系统练习控制器开发里最常用的一批注解，包括请求映射、参数绑定、请求体接收、参数校验和全局异常处理。

**Architecture:** 这个练习项目用一个最小的“订单管理接口”把 Web 注解串起来。主链路是 `Controller -> Service`，并通过 `@RestControllerAdvice` 统一处理接口异常，让你看到常见控制器注解是如何协同工作的。

**Tech Stack:** Java 17, Spring Boot 3.5.7, Spring Web, Spring Validation, Maven, Lombok, JUnit 5

---

## 一、这个专题在真实开发里为什么重要

Spring Boot 日常开发里，最常见的一批注解其实都集中在 Controller 这一层。

你写一个接口，几乎一定会碰到这些注解：

- `@RestController`
- `@RequestMapping`
- `@GetMapping`
- `@PostMapping`
- `@PutMapping`
- `@DeleteMapping`
- `@RequestParam`
- `@PathVariable`
- `@RequestBody`
- `@Valid`
- `@RestControllerAdvice`
- `@ExceptionHandler`

如果这些注解你只是背定义，而没有在一个小项目里真正用过，后面写接口时会很容易混淆：

- 哪种参数该用 `@PathVariable`
- 哪种参数该用 `@RequestParam`
- 请求体到底什么时候用 `@RequestBody`
- 校验失败和业务异常该怎么统一返回

这一份文档就是专门把这些问题串成一条完整练习链路。

---

## 二、最终目录结构

```text
springboot-web-annotations-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/springbootwebannotations
│   │   │   ├── SpringBootWebAnnotationsApplication.java
│   │   │   ├── advice
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── common
│   │   │   │   └── Result.java
│   │   │   ├── controller
│   │   │   │   └── OrderController.java
│   │   │   ├── dto
│   │   │   │   ├── CreateOrderRequest.java
│   │   │   │   └── UpdateOrderStatusRequest.java
│   │   │   ├── model
│   │   │   │   └── OrderInfo.java
│   │   │   └── service
│   │   │       └── OrderService.java
│   │   └── resources
│   │       └── application.yml
│   └── test
│       └── java/com/example/springbootwebannotations
│           └── SpringBootWebAnnotationsApplicationTests.java
```

这段目录结构的作用：

- 让你提前知道 Web 注解示例会落在哪些文件里
- 这份练习的重点不在数据库，所以项目结构刻意保持精简

---

### Task 1: 创建项目骨架

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/example/springbootwebannotations/SpringBootWebAnnotationsApplication.java`
- Create: `src/main/resources/application.yml`
- Test: `src/test/java/com/example/springbootwebannotations/SpringBootWebAnnotationsApplicationTests.java`

**Step 1: 创建 `pom.xml`**

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.7</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>springboot-web-annotations-demo</artifactId>
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
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
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

- 提供 Web 接口和参数校验的基本能力
- 这两个 starter 足够完成本次 Web 注解练习

**Step 2: 创建启动类 `SpringBootWebAnnotationsApplication.java`**

```java
package com.example.springbootwebannotations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication 是当前 Web 项目的启动入口。
@SpringBootApplication
public class SpringBootWebAnnotationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootWebAnnotationsApplication.class, args);
    }
}
```

这段代码的作用：

- 这是整个 Web 注解练习项目的启动入口
- 所有 Controller、Service、Advice 都会从这里开始被 Spring Boot 扫描

**Step 3: 创建 `application.yml`**

```yaml
server:
  port: 8192

spring:
  application:
    name: springboot-web-annotations-demo
```

这段代码的作用：

- 固定应用端口为 `8192`
- 让这个练习项目和其他专题端口区分开

**Step 4: 创建测试类 `SpringBootWebAnnotationsApplicationTests.java`**

```java
package com.example.springbootwebannotations;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// @SpringBootTest 会拉起完整的测试上下文。
@SpringBootTest
class SpringBootWebAnnotationsApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

这段代码的作用：

- 用最基础的测试确认 Spring 容器能正常启动
- 先保证项目骨架没问题，再继续写接口

---

### Task 2: 创建公共返回对象、DTO 和模型

**Files:**
- Create: `src/main/java/com/example/springbootwebannotations/common/Result.java`
- Create: `src/main/java/com/example/springbootwebannotations/dto/CreateOrderRequest.java`
- Create: `src/main/java/com/example/springbootwebannotations/dto/UpdateOrderStatusRequest.java`
- Create: `src/main/java/com/example/springbootwebannotations/model/OrderInfo.java`

**Step 1: 创建 `Result.java`**

```java
package com.example.springbootwebannotations.common;

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

这段代码的作用：

- 统一接口成功和失败时的返回结构
- 后面全局异常处理器会直接复用这个对象

**Step 2: 创建 `CreateOrderRequest.java`**

```java
package com.example.springbootwebannotations.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// @Data 负责生成 getter、setter。
@Data
public class CreateOrderRequest {

    // @NotBlank 表示字符串不能为空也不能是纯空格。
    @NotBlank(message = "订单标题不能为空")
    private String title;

    // @NotNull 表示这个字段不能为 null。
    @NotNull(message = "订单金额不能为空")
    // @DecimalMin 表示数字最小值约束。
    @DecimalMin(value = "0.01", message = "订单金额必须大于 0")
    private java.math.BigDecimal amount;

    @NotBlank(message = "下单人不能为空")
    private String customerName;
}
```

这段代码的作用：

- 这是创建订单接口的请求体对象
- `@Valid` 后面会配合这些校验注解一起工作

**Step 3: 创建 `UpdateOrderStatusRequest.java`**

```java
package com.example.springbootwebannotations.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {

    @NotBlank(message = "订单状态不能为空")
    private String status;
}
```

这段代码的作用：

- 这是更新订单状态接口的请求体对象
- 结构很小，适合拿来演示 `@RequestBody` 和 `@Valid`

**Step 4: 创建 `OrderInfo.java`**

```java
package com.example.springbootwebannotations.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderInfo {

    private Long id;
    private String title;
    private BigDecimal amount;
    private String customerName;
    private String status;
}
```

这段代码的作用：

- 这是接口返回的订单对象
- 本次练习不接数据库，所以直接用内存对象来保存订单状态

---

### Task 3: 创建 Service，准备一个最小的订单内存存储

**Files:**
- Create: `src/main/java/com/example/springbootwebannotations/service/OrderService.java`

**Step 1: 创建 `OrderService.java`**

```java
package com.example.springbootwebannotations.service;

import com.example.springbootwebannotations.dto.CreateOrderRequest;
import com.example.springbootwebannotations.dto.UpdateOrderStatusRequest;
import com.example.springbootwebannotations.model.OrderInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

// @Service 表示当前类属于业务层 Bean。
@Service
public class OrderService {

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, OrderInfo> orderStore = new ConcurrentHashMap<>();

    public OrderInfo create(CreateOrderRequest request) {
        Long id = idGenerator.getAndIncrement();
        OrderInfo orderInfo = OrderInfo.builder()
            .id(id)
            .title(request.getTitle())
            .amount(request.getAmount())
            .customerName(request.getCustomerName())
            .status("CREATED")
            .build();

        orderStore.put(id, orderInfo);
        return orderInfo;
    }

    public OrderInfo getById(Long orderId) {
        OrderInfo orderInfo = orderStore.get(orderId);
        if (orderInfo == null) {
            throw new IllegalArgumentException("订单不存在，orderId=" + orderId);
        }
        return orderInfo;
    }

    public List<OrderInfo> listByStatus(String status) {
        List<OrderInfo> results = new ArrayList<>();
        for (OrderInfo orderInfo : orderStore.values()) {
            if (status == null || status.equalsIgnoreCase(orderInfo.getStatus())) {
                results.add(orderInfo);
            }
        }
        return results;
    }

    public OrderInfo updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        OrderInfo orderInfo = getById(orderId);
        orderInfo.setStatus(request.getStatus());
        return orderInfo;
    }

    public void delete(Long orderId) {
        if (!orderStore.containsKey(orderId)) {
            throw new IllegalArgumentException("订单不存在，orderId=" + orderId);
        }
        orderStore.remove(orderId);
    }
}
```

这段代码的作用：

- 用一个最小的内存存储把订单接口跑起来
- 这样你可以专注于 Web 注解本身，而不是被数据库配置打断

---

### Task 4: 创建 Controller，集中练习最常见的 Web 注解

**Files:**
- Create: `src/main/java/com/example/springbootwebannotations/controller/OrderController.java`

**Step 1: 创建 `OrderController.java`**

```java
package com.example.springbootwebannotations.controller;

import com.example.springbootwebannotations.common.Result;
import com.example.springbootwebannotations.dto.CreateOrderRequest;
import com.example.springbootwebannotations.dto.UpdateOrderStatusRequest;
import com.example.springbootwebannotations.model.OrderInfo;
import com.example.springbootwebannotations.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// @RestController 表示这是一个 REST 控制器，返回值默认会写回响应体。
@RestController
// @RequestMapping 用来给一组接口定义统一前缀。
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // @PostMapping 表示处理 POST 请求。
    // @RequestBody 表示把 JSON 请求体绑定到方法参数上。
    // @Valid 表示触发 CreateOrderRequest 上的校验注解。
    @PostMapping
    public Result<OrderInfo> create(@Valid @RequestBody CreateOrderRequest request) {
        return Result.success(orderService.create(request));
    }

    // @GetMapping 表示处理 GET 请求。
    // @PathVariable 表示读取路径中的变量值。
    @GetMapping("/{orderId}")
    public Result<OrderInfo> getById(@PathVariable Long orderId) {
        return Result.success(orderService.getById(orderId));
    }

    // @RequestParam 适合接收查询参数。
    // required = false 表示这个参数可以不传。
    @GetMapping
    public Result<List<OrderInfo>> list(@RequestParam(required = false) String status) {
        return Result.success(orderService.listByStatus(status));
    }

    // @PutMapping 表示处理 PUT 请求，通常用于更新。
    @PutMapping("/{orderId}/status")
    public Result<OrderInfo> updateStatus(@PathVariable Long orderId,
                                          @Valid @RequestBody UpdateOrderStatusRequest request) {
        return Result.success(orderService.updateStatus(orderId, request));
    }

    // @DeleteMapping 表示处理 DELETE 请求。
    @DeleteMapping("/{orderId}")
    public Result<String> delete(@PathVariable Long orderId) {
        orderService.delete(orderId);
        return Result.success("删除成功");
    }
}
```

这段代码的作用：

- `@RestController`、`@RequestMapping`、各类 Mapping 注解、`@PathVariable`、`@RequestParam`、`@RequestBody`、`@Valid` 都集中出现在这里
- 这是最适合系统理解 Web 注解配合方式的一份代码

---

### Task 5: 创建全局异常处理器，练习 `@RestControllerAdvice` 和 `@ExceptionHandler`

**Files:**
- Create: `src/main/java/com/example/springbootwebannotations/advice/GlobalExceptionHandler.java`

**Step 1: 创建 `GlobalExceptionHandler.java`**

```java
package com.example.springbootwebannotations.advice;

import com.example.springbootwebannotations.common.Result;
import java.util.stream.Collectors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// @RestControllerAdvice 表示这是一个全局的 REST 异常处理器。
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @ExceptionHandler 指定当前方法处理哪一种异常。
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ":" + error.getDefaultMessage())
            .collect(Collectors.joining("; "));

        return Result.fail(message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<String> handleIllegalArgumentException(IllegalArgumentException exception) {
        return Result.fail(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleOtherException(Exception exception) {
        return Result.fail("系统异常：" + exception.getMessage());
    }
}
```

这段代码的作用：

- `@RestControllerAdvice` 表示这是一个全局的 REST 异常处理器
- `@ExceptionHandler` 会拦截指定类型的异常并统一返回结果

---

## 三、启动项目

**Step 1: 启动 Spring Boot**

```bash
mvn spring-boot:run
```

这段命令的作用：

- 启动当前 Web 注解练习项目
- 启动后应用会监听 `8192` 端口

---

## 四、接口测试

**Step 1: 创建订单**

```bash
curl -X POST http://localhost:8192/orders ^
  -H "Content-Type: application/json" ^
  -d "{\"title\":\"演示订单\",\"amount\":199.00,\"customerName\":\"Alice\"}"
```

这段命令的作用：

- 演示 `@PostMapping`、`@RequestBody` 和 `@Valid`
- 这是最典型的“新增资源”接口写法

**Step 2: 按 id 查询订单**

```bash
curl http://localhost:8192/orders/1
```

这段命令的作用：

- 演示 `@GetMapping` 和 `@PathVariable`
- 路径里的 `1` 会被绑定到方法参数 `orderId`

**Step 3: 按状态筛选订单**

```bash
curl "http://localhost:8192/orders?status=CREATED"
```

这段命令的作用：

- 演示 `@RequestParam`
- 查询字符串里的 `status` 会被绑定到方法参数上

**Step 4: 更新订单状态**

```bash
curl -X PUT http://localhost:8192/orders/1/status ^
  -H "Content-Type: application/json" ^
  -d "{\"status\":\"PAID\"}"
```

这段命令的作用：

- 演示 `@PutMapping`、`@PathVariable` 和 `@RequestBody`
- 这是最典型的“更新资源状态”接口写法

**Step 5: 删除订单**

```bash
curl -X DELETE http://localhost:8192/orders/1
```

这段命令的作用：

- 演示 `@DeleteMapping`
- 这是最典型的“删除资源”接口写法

---

## 五、这个专题里要重点记住什么

### 1. `@RestController`

作用：

- 表示当前类是 REST 控制器
- 返回值默认会被序列化成 JSON

### 2. `@RequestMapping`

作用：

- 可以加在类上，也可以加在方法上
- 用来定义统一路径前缀或基础映射规则

### 3. `@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping`

作用：

- 它们都是更语义化的请求映射注解
- 分别对应常见的查询、新增、更新、删除接口

### 4. `@PathVariable`

作用：

- 适合接路径参数
- 比如 `/orders/1` 里的 `1`

### 5. `@RequestParam`

作用：

- 适合接查询参数
- 比如 `?status=CREATED`

### 6. `@RequestBody`

作用：

- 适合接 JSON 请求体
- 常用于新增和更新接口

### 7. `@Valid`

作用：

- 触发参数对象上的校验规则
- 校验失败后通常会抛出 `MethodArgumentNotValidException`

### 8. `@RestControllerAdvice` + `@ExceptionHandler`

作用：

- 负责统一处理接口异常
- 让错误返回更一致，也让 Controller 更简洁

---

## 六、常见报错排查

### 1. 请求体为空或 400

原因：

- 你忘了加 `Content-Type: application/json`
- 或者请求体 JSON 格式不正确

### 2. 参数校验不生效

原因：

- 你漏写了 `@Valid`
- 或者 DTO 上没有加校验注解

### 3. 异常没有被统一处理

原因：

- 你漏写了 `@RestControllerAdvice`
- 或者 `@ExceptionHandler` 方法没有匹配到实际异常类型

---

## 七、你在这个专题里学到了什么

做完这个项目后，你应该已经掌握：

- Controller 层最常用的一批注解各自负责什么
- 路径参数、查询参数、请求体分别该怎么接
- 参数校验和全局异常处理该怎么配合使用
- 为什么说 Spring Boot Web 开发的核心，不只是“写接口”，而是“把接口语义表达清楚”
