# springcloud-common-annotations-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 用一个最小可运行的 Spring Cloud 示例，练习微服务开发里最常见的一批注解，包括服务发现、Feign 声明式调用、负载均衡客户端和可刷新的配置 Bean。

**Architecture:** 这个练习包含一个 `user-service` 提供用户接口，一个 `order-service` 通过 Feign 和 `RestTemplate` 两种方式调用它。服务注册中心使用 Consul，通过 Docker Compose 启动，目的是把 `@EnableDiscoveryClient`、`@EnableFeignClients`、`@FeignClient`、`@LoadBalanced`、`@RefreshScope` 串成一个最小闭环。

**Tech Stack:** Java 17, Spring Boot 3.3.13, Spring Cloud 2023.0.6, Spring Web, Spring Boot Actuator, Spring Cloud OpenFeign, Spring Cloud Consul Discovery, Spring Cloud LoadBalancer, Docker Compose, Maven, Lombok, JUnit 5

---

## 一、这个练习在真实开发里为什么重要

Spring Cloud 项目里，真正最常见的并不是某一个中间件名字，而是一批反复出现的“微服务注解”：

- `@EnableDiscoveryClient`
- `@EnableFeignClients`
- `@FeignClient`
- `@LoadBalanced`
- `@RefreshScope`

这些注解分别对应的能力是：

- 服务注册与发现
- 声明式远程调用
- 用服务名发请求
- 配置刷新后的 Bean 重新获取

如果你只看定义，很难理解它们为什么总是成组出现。  
这一份练习文档会把它们放到一个最小的微服务调用链里，让你看清：

- 为什么需要注册中心
- 为什么 Feign 只写接口就能远程调服务
- 为什么 `RestTemplate` 加了 `@LoadBalanced` 才能识别服务名 URL

---

## 二、这份练习里的一个重要说明

根据 Spring Cloud Commons 官方文档，`@EnableDiscoveryClient` 在当前 Spring Cloud 版本里通常已经**不是必须的**。

也就是说：

- 只要类路径里存在 `DiscoveryClient` 的实现
- 应用通常就能自动注册到注册中心

但这份练习仍然保留它，原因是：

- 它在大量历史项目里仍然非常常见
- 面试和老项目维护时经常会碰到
- 你这次学的是“常见注解”，保留它更有助于建立完整认知

这段说明的作用：

- 告诉你这个注解在“现在”和“历史项目”里的定位差异
- 避免你学完以后误以为它永远都是必须的

---

## 三、最终目录结构

```text
springcloud-common-annotations-demo
├── docker-compose.yml
├── order-service
│   ├── pom.xml
│   └── src
│       └── main
│           ├── java/com/example/orderservice
│           │   ├── OrderServiceApplication.java
│           │   ├── client
│           │   │   └── UserFeignClient.java
│           │   ├── config
│           │   │   └── LoadBalancerConfig.java
│           │   ├── controller
│           │   │   └── OrderController.java
│           │   ├── service
│           │   │   ├── OrderMessageHolder.java
│           │   │   └── OrderQueryService.java
│           │   └── dto
│           │       └── OrderView.java
│           └── resources
│               └── application.yml
└── user-service
    ├── pom.xml
    └── src
        └── main
            ├── java/com/example/userservice
            │   ├── UserServiceApplication.java
            │   └── controller
            │       └── UserController.java
            └── resources
                └── application.yml
```

这段目录结构的作用：

- 让你先知道这份文档不是一个单体应用，而是一个“两个微服务 + 一个注册中心”的最小组合
- `user-service` 负责提供数据，`order-service` 负责调用它

---

### Task 1: 使用 Docker 启动 Consul 注册中心

**Files:**
- Create: `docker-compose.yml`

**Step 1: 创建 `docker-compose.yml`**

```yaml
services:
  consul:
    image: hashicorp/consul:1.20
    container_name: springcloud-consul-demo
    restart: always
    command: "agent -server -bootstrap-expect=1 -ui -client=0.0.0.0"
    ports:
      - "8500:8500"
```

这段代码的作用：

- 启动一个最小可用的 Consul 服务
- `8500` 是 Consul 的 HTTP UI 和 API 端口
- 后面两个微服务都会把自己注册到这里

---

### Task 2: 创建 `user-service`，演示服务注册和提供方接口

**Files:**
- Create: `user-service/pom.xml`
- Create: `user-service/src/main/java/com/example/userservice/UserServiceApplication.java`
- Create: `user-service/src/main/java/com/example/userservice/controller/UserController.java`
- Create: `user-service/src/main/resources/application.yml`

**Step 1: 创建 `user-service/pom.xml`**

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.13</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>user-service</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.6</spring-cloud.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-consul-discovery</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

这段代码的作用：

- 通过 `spring-cloud-dependencies` BOM 统一管理 Spring Cloud 依赖版本
- `spring-cloud-starter-consul-discovery` 提供 Consul 注册发现能力
- `actuator` 会给服务健康检查和监控暴露基础能力

**Step 2: 创建 `UserServiceApplication.java`**

```java
package com.example.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

这段代码的作用：

- `@SpringBootApplication` 是服务启动入口
- `@EnableDiscoveryClient` 在当前版本里通常是可选的，这里保留它是为了让你明确感知“这个服务需要注册到注册中心”

**Step 3: 创建 `UserController.java`**

```java
package com.example.userservice.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{userId}")
    public Map<String, Object> getUser(@PathVariable Long userId) {
        return Map.of(
            "userId", userId,
            "userName", userId == 1L ? "Alice" : "Bob",
            "level", userId == 1L ? "VIP" : "NORMAL"
        );
    }
}
```

这段代码的作用：

- 提供一个最小的用户服务接口
- 这个接口后面会被 `order-service` 通过 Feign 和 `RestTemplate` 两种方式调用

**Step 4: 创建 `user-service/application.yml`**

```yaml
server:
  port: 8181

spring:
  application:
    name: user-service
  cloud:
    consul:
      host: localhost
      port: 8500
      discovery:
        prefer-ip-address: true

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

这段代码的作用：

- 把 `user-service` 的服务名固定为 `user-service`
- 注册中心地址指向本地 Docker 启动的 Consul
- 这个服务名后面就是 Feign 和负载均衡调用时使用的逻辑服务名

---

### Task 3: 创建 `order-service`，集中练习微服务常见注解

**Files:**
- Create: `order-service/pom.xml`
- Create: `order-service/src/main/java/com/example/orderservice/OrderServiceApplication.java`
- Create: `order-service/src/main/java/com/example/orderservice/client/UserFeignClient.java`
- Create: `order-service/src/main/java/com/example/orderservice/config/LoadBalancerConfig.java`
- Create: `order-service/src/main/java/com/example/orderservice/dto/OrderView.java`
- Create: `order-service/src/main/java/com/example/orderservice/service/OrderMessageHolder.java`
- Create: `order-service/src/main/java/com/example/orderservice/service/OrderQueryService.java`
- Create: `order-service/src/main/java/com/example/orderservice/controller/OrderController.java`
- Create: `order-service/src/main/resources/application.yml`

**Step 1: 创建 `order-service/pom.xml`**

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.13</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>order-service</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.6</spring-cloud.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-consul-discovery</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

这段代码的作用：

- `spring-cloud-starter-openfeign` 提供声明式远程调用能力
- `spring-cloud-starter-loadbalancer` 提供基于服务名的客户端负载均衡能力
- `spring-cloud-starter` 让 `@RefreshScope` 等通用 Spring Cloud Context 能力可用

**Step 2: 创建 `OrderServiceApplication.java`**

```java
package com.example.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

这段代码的作用：

- `@EnableFeignClients` 会扫描并启用 `@FeignClient` 接口
- 没有它，Feign 接口只是普通接口，Spring 不会为它创建代理对象

**Step 3: 创建 `UserFeignClient.java`**

```java
package com.example.orderservice.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserFeignClient {

    @GetMapping("/users/{userId}")
    Map<String, Object> getUser(@PathVariable("userId") Long userId);
}
```

这段代码的作用：

- `@FeignClient(name = "user-service")` 表示这个接口对应一个叫 `user-service` 的远程服务
- 你只需要写接口方法和映射，Feign 会帮你生成远程调用代理

**Step 4: 创建 `LoadBalancerConfig.java`**

```java
package com.example.orderservice.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LoadBalancerConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
```

这段代码的作用：

- `@LoadBalanced` 会让这个 `RestTemplate` 具备“识别服务名”的能力
- 没有它时，`http://user-service/users/1` 这样的地址是无法解析的

**Step 5: 创建 `OrderView.java`**

```java
package com.example.orderservice.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderView {

    private Long orderId;
    private Map<String, Object> feignUser;
    private Map<String, Object> loadBalancedUser;
    private String message;
}
```

这段代码的作用：

- 这个对象用于聚合展示订单服务调用结果
- 它会同时展示 Feign 调用结果和 `RestTemplate` 调用结果

**Step 6: 创建 `OrderMessageHolder.java`**

```java
package com.example.orderservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class OrderMessageHolder {

    @Value("${custom.order-message}")
    private String orderMessage;

    public String currentMessage() {
        return orderMessage;
    }
}
```

这段代码的作用：

- `@RefreshScope` 适合放在那些依赖“可刷新的配置值”的 Bean 上
- 这份练习先演示最小写法，真实项目里它通常会和 Config Server、Consul Config、Bus 等能力一起使用

**Step 7: 创建 `OrderQueryService.java`**

```java
package com.example.orderservice.service;

import com.example.orderservice.client.UserFeignClient;
import com.example.orderservice.dto.OrderView;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final UserFeignClient userFeignClient;
    private final RestTemplate restTemplate;
    private final OrderMessageHolder orderMessageHolder;

    public OrderView queryOrder(Long orderId, Long userId) {
        Map<String, Object> feignUser = userFeignClient.getUser(userId);

        Map<String, Object> loadBalancedUser =
            restTemplate.getForObject("http://user-service/users/" + userId, Map.class);

        return OrderView.builder()
            .orderId(orderId)
            .feignUser(feignUser)
            .loadBalancedUser(loadBalancedUser)
            .message(orderMessageHolder.currentMessage())
            .build();
    }
}
```

这段代码的作用：

- 这个 Service 把 3 个注解能力串了起来
- `@FeignClient` 负责一种调用方式
- `@LoadBalanced RestTemplate` 负责另一种调用方式
- `@RefreshScope` 管理的配置 Bean 负责提供当前消息

**Step 8: 创建 `OrderController.java`**

```java
package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderView;
import com.example.orderservice.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderQueryService orderQueryService;

    @GetMapping("/{orderId}")
    public OrderView queryOrder(@PathVariable Long orderId,
                                @RequestParam Long userId) {
        return orderQueryService.queryOrder(orderId, userId);
    }
}
```

这段代码的作用：

- 暴露一个最小查询接口，把微服务调用结果直接展示出来
- 这样你调用一次接口，就能同时验证 Feign 和 `@LoadBalanced RestTemplate`

**Step 9: 创建 `order-service/application.yml`**

```yaml
server:
  port: 8182

spring:
  application:
    name: order-service
  cloud:
    consul:
      host: localhost
      port: 8500
      discovery:
        prefer-ip-address: true

custom:
  order-message: "当前订单服务消息来自 @RefreshScope Bean"

management:
  endpoints:
    web:
      exposure:
        include: health,info,refresh
```

这段代码的作用：

- 把 `order-service` 自己也注册到 Consul
- `custom.order-message` 是给 `@RefreshScope` Bean 读取的演示配置
- 暴露 `refresh` 端点，是为了让你知道动态刷新通常依赖这个入口

---

## 四、启动顺序

**Step 1: 启动 Consul**

```bash
docker compose up -d
```

这段命令的作用：

- 启动注册中心
- 之后 `user-service` 和 `order-service` 都会注册到它上面

**Step 2: 启动 `user-service`**

```bash
mvn spring-boot:run
```

这段命令的作用：

- 在 `user-service` 目录里执行时，会启动用户服务
- 启动后它会监听 `8181`

**Step 3: 启动 `order-service`**

```bash
mvn spring-boot:run
```

这段命令的作用：

- 在 `order-service` 目录里执行时，会启动订单服务
- 启动后它会监听 `8182`

---

## 五、接口测试

**Step 1: 先检查 Consul UI**

浏览器打开：

```text
http://localhost:8500
```

这段地址的作用：

- 让你确认 `user-service` 和 `order-service` 是否都注册成功
- 如果没注册成功，后面的服务调用一定会失败

**Step 2: 通过订单服务触发远程调用**

```bash
curl "http://localhost:8182/orders/1001?userId=1"
```

这段命令的作用：

- 通过 `order-service` 查询订单
- 同时触发 Feign 调用和 `RestTemplate` 调用
- 你可以在一个返回结果里直观看到两种方式都拿到了 `user-service` 的数据

---

## 六、这个练习里要重点记住什么

### 1. `@EnableDiscoveryClient`

作用：

- 表示这个服务具备注册发现能力
- 在当前版本里通常是可选的，但历史项目里很常见

### 2. `@EnableFeignClients`

作用：

- 开启 Feign 接口扫描
- 没有它，`@FeignClient` 不会生效

### 3. `@FeignClient`

作用：

- 用声明式接口方式调用远程服务
- 代码可读性通常比手写 HTTP 客户端更高

### 4. `@LoadBalanced`

作用：

- 让 `RestTemplate` 能识别逻辑服务名
- 没有它时，服务名 URL 不能被负载均衡客户端解析

### 5. `@RefreshScope`

作用：

- 让 Bean 在配置刷新后重新创建
- 适合那些依赖动态配置的组件

---

## 七、常见报错排查

### 1. `No instances available for user-service`

原因：

- `user-service` 没有成功注册到 Consul
- 或者 `order-service` 启动时，注册中心里还没有可用实例

### 2. `@FeignClient` 注入失败

原因：

- 你漏写了 `@EnableFeignClients`
- 或者 Feign 接口所在包没有被 Spring Boot 扫描到

### 3. `UnknownHostException: user-service`

原因：

- 你在 `RestTemplate` 调用里用了服务名 URL
- 但对应的 `RestTemplate` Bean 上没有加 `@LoadBalanced`

### 4. `@RefreshScope` 看起来没有效果

原因：

- 这份最小示例只是演示注解写法
- 真正的动态刷新通常还需要外部配置中心或配置变更事件

---

## 八、你在这个练习里学到了什么

做完这个项目后，你应该已经掌握：

- Spring Cloud 最常见的一批注解分别适合解决什么问题
- Feign 为什么只写接口就能远程调服务
- `@LoadBalanced` 为什么能让你用服务名发请求
- `@RefreshScope` 为什么通常和配置中心一起出现
- 微服务注解并不是孤立存在的，它们通常会共同构成一个最小调用闭环
