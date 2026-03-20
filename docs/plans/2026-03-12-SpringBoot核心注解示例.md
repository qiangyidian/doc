# springboot-core-annotations-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot 项目，用一个最小可运行示例练习 Spring Boot 开发中最常见的核心注解，包括启动、配置绑定、Bean 注册和分层组件注解。

**Architecture:** 这个练习项目用一个简单的“用户资料展示”示例把核心注解串起来。主链路是 `Controller -> Service -> Repository`，同时通过 `@ConfigurationProperties`、`@Configuration`、`@Bean` 和 `@Value` 演示 Spring 容器和配置绑定的基础能力。

**Tech Stack:** Java 17, Spring Boot 3.3.13, Spring Web, Spring Validation, Maven, Lombok, JUnit 5

---

## 一、这个练习在真实开发里为什么重要

很多 Spring Boot 新手会背一堆注解名字，但不知道它们分别解决什么问题。

这一份文档就是专门解决这个问题的。  
你会在一个最小项目里看到这些注解分别出现在什么位置：

- `@SpringBootApplication`
- `@Configuration`
- `@Bean`
- `@ConfigurationProperties`
- `@ConfigurationPropertiesScan`
- `@Component`
- `@Service`
- `@Repository`
- `@Value`

当你把这几个注解真正放进一个项目里跑通后，Spring Boot 的很多“魔法感”就会消失，因为你会知道它到底在帮你做什么。

---

## 二、最终目录结构

```text
springboot-core-annotations-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/springbootcoreannotations
│   │   │   ├── SpringBootCoreAnnotationsApplication.java
│   │   │   ├── common
│   │   │   │   └── Result.java
│   │   │   ├── component
│   │   │   │   └── TimeFormatterComponent.java
│   │   │   ├── config
│   │   │   │   ├── AppProperties.java
│   │   │   │   └── BeanConfig.java
│   │   │   ├── controller
│   │   │   │   └── UserProfileController.java
│   │   │   ├── model
│   │   │   │   └── UserProfile.java
│   │   │   ├── repository
│   │   │   │   └── UserProfileRepository.java
│   │   │   └── service
│   │   │       └── UserProfileService.java
│   │   └── resources
│   │       └── application.yml
│   └── test
│       └── java/com/example/springbootcoreannotations
│           └── SpringBootCoreAnnotationsApplicationTests.java
```

这段目录结构的作用：

- 让你先看清整个练习项目到底要创建哪些文件
- 把“配置类”“组件类”“服务层”“仓储层”“控制器”分开，便于理解注解的职责边界

---

### Task 1: 创建项目骨架

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/example/springbootcoreannotations/SpringBootCoreAnnotationsApplication.java`
- Create: `src/main/resources/application.yml`
- Test: `src/test/java/com/example/springbootcoreannotations/SpringBootCoreAnnotationsApplicationTests.java`

**Step 1: 创建 `pom.xml`**

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
    <artifactId>springboot-core-annotations-demo</artifactId>
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

- 这是整个项目的 Maven 依赖入口
- `spring-boot-starter-web` 用来启动 Web 应用并提供控制器能力
- `spring-boot-starter-validation` 给后面参数校验留出基础能力

**Step 2: 创建启动类 `SpringBootCoreAnnotationsApplication.java`**

```java
package com.example.springbootcoreannotations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringBootCoreAnnotationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootCoreAnnotationsApplication.class, args);
    }
}
```

这段代码的作用：

- `@SpringBootApplication` 是 Spring Boot 项目最核心的启动注解
- 它本质上组合了配置、自动装配和组件扫描等能力
- `@ConfigurationPropertiesScan` 会让 Spring 自动扫描 `@ConfigurationProperties` 配置类

**Step 3: 创建 `application.yml`**

```yaml
server:
  port: 8171

spring:
  application:
    name: springboot-core-annotations-demo

app:
  profile:
    site-name: "Spring 注解练习站"
    owner-name: "demo-admin"
    default-city: "Shanghai"

custom:
  welcome-message: "欢迎来到 Spring Boot 核心注解练习项目"
```

这段代码的作用：

- 把应用端口和应用名固定下来
- `app.profile` 这组配置后面会绑定到 `@ConfigurationProperties` 对象上
- `custom.welcome-message` 后面会用 `@Value` 注解读取

**Step 4: 创建测试类 `SpringBootCoreAnnotationsApplicationTests.java`**

```java
package com.example.springbootcoreannotations;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringBootCoreAnnotationsApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

这段代码的作用：

- 这是最基础的上下文启动测试
- 它可以帮助你先确认 Spring Boot 项目能成功启动

---

### Task 2: 创建公共返回对象和模型

**Files:**
- Create: `src/main/java/com/example/springbootcoreannotations/common/Result.java`
- Create: `src/main/java/com/example/springbootcoreannotations/model/UserProfile.java`

**Step 1: 创建 `Result.java`**

```java
package com.example.springbootcoreannotations.common;

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
- 避免每个 Controller 方法都返回不同格式的数据

**Step 2: 创建 `UserProfile.java`**

```java
package com.example.springbootcoreannotations.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfile {

    private Long id;
    private String name;
    private String city;
    private String ownerName;
    private String welcomeMessage;
    private String generatedAt;
}
```

这段代码的作用：

- 这是这个练习项目里返回给前端的用户资料模型
- 后面 Service 会把配置数据、仓储数据和组件处理结果组装成这个对象

---

### Task 3: 创建配置类，练习 `@ConfigurationProperties`、`@Configuration`、`@Bean`

**Files:**
- Create: `src/main/java/com/example/springbootcoreannotations/config/AppProperties.java`
- Create: `src/main/java/com/example/springbootcoreannotations/config/BeanConfig.java`

**Step 1: 创建 `AppProperties.java`**

```java
package com.example.springbootcoreannotations.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.profile")
public class AppProperties {

    private String siteName;
    private String ownerName;
    private String defaultCity;
}
```

这段代码的作用：

- `@ConfigurationProperties` 会把 `application.yml` 里的 `app.profile` 配置绑定到这个对象
- 这比到处写 `@Value` 更适合读取一整组结构化配置

**Step 2: 创建 `BeanConfig.java`**

```java
package com.example.springbootcoreannotations.config;

import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    public DateTimeFormatter dateTimeFormatter() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }
}
```

这段代码的作用：

- `@Configuration` 表示这是一个配置类
- `@Bean` 表示把方法返回值交给 Spring 容器管理
- 这里注册了一个 `DateTimeFormatter`，后面组件类可以直接注入使用

---

### Task 4: 创建组件层、仓储层、服务层，练习 `@Component`、`@Repository`、`@Service`

**Files:**
- Create: `src/main/java/com/example/springbootcoreannotations/component/TimeFormatterComponent.java`
- Create: `src/main/java/com/example/springbootcoreannotations/repository/UserProfileRepository.java`
- Create: `src/main/java/com/example/springbootcoreannotations/service/UserProfileService.java`

**Step 1: 创建 `TimeFormatterComponent.java`**

```java
package com.example.springbootcoreannotations.component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimeFormatterComponent {

    private final DateTimeFormatter dateTimeFormatter;

    public String nowText() {
        return LocalDateTime.now().format(dateTimeFormatter);
    }
}
```

这段代码的作用：

- `@Component` 是最通用的 Spring 组件注解
- 这个类会被 Spring 自动扫描并注册成 Bean
- 它还演示了组件类如何直接注入前面 `@Bean` 注册的 `DateTimeFormatter`

**Step 2: 创建 `UserProfileRepository.java`**

```java
package com.example.springbootcoreannotations.repository;

import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class UserProfileRepository {

    public Map<Long, String> loadUserNames() {
        return Map.of(
            1L, "Alice",
            2L, "Bob",
            3L, "Charlie"
        );
    }
}
```

这段代码的作用：

- `@Repository` 通常放在数据访问层
- 这里虽然没有接数据库，但它的职责仍然是“提供数据”
- 在真实项目里，这一层通常会对接 MyBatis、JPA 或其他持久化框架

**Step 3: 创建 `UserProfileService.java`**

```java
package com.example.springbootcoreannotations.service;

import com.example.springbootcoreannotations.component.TimeFormatterComponent;
import com.example.springbootcoreannotations.config.AppProperties;
import com.example.springbootcoreannotations.model.UserProfile;
import com.example.springbootcoreannotations.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final AppProperties appProperties;
    private final TimeFormatterComponent timeFormatterComponent;

    @Value("${custom.welcome-message}")
    private String welcomeMessage;

    public UserProfile getProfile(Long userId) {
        String userName = userProfileRepository.loadUserNames().getOrDefault(userId, "Guest");

        return UserProfile.builder()
            .id(userId)
            .name(userName)
            .city(appProperties.getDefaultCity())
            .ownerName(appProperties.getOwnerName())
            .welcomeMessage(welcomeMessage)
            .generatedAt(timeFormatterComponent.nowText())
            .build();
    }
}
```

这段代码的作用：

- `@Service` 通常放在业务层
- 这个类把仓储层的数据、配置类的数据和组件层的时间格式化能力组合起来
- 这里同时演示了 `@ConfigurationProperties` 和 `@Value` 两种取配置方式

---

### Task 5: 创建 Controller，观察这些核心注解如何串成一个完整请求链路

**Files:**
- Create: `src/main/java/com/example/springbootcoreannotations/controller/UserProfileController.java`

**Step 1: 创建 `UserProfileController.java`**

```java
package com.example.springbootcoreannotations.controller;

import com.example.springbootcoreannotations.common.Result;
import com.example.springbootcoreannotations.model.UserProfile;
import com.example.springbootcoreannotations.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/{userId}")
    public Result<UserProfile> getProfile(@PathVariable Long userId) {
        return Result.success(userProfileService.getProfile(userId));
    }
}
```

这段代码的作用：

- 虽然这份文档重点不是 Web 注解，但一个最小可运行项目仍然需要一个接口入口
- 这个 Controller 会把请求交给 `UserProfileService`
- 到这里你就能清楚看到 Spring Boot 最基本的分层结构已经完整了

---

## 三、启动项目

**Step 1: 启动 Spring Boot**

```bash
mvn spring-boot:run
```

这段命令的作用：

- 启动当前 Spring Boot 项目
- 启动后应用会监听 `8171` 端口

---

## 四、接口测试

**Step 1: 查询用户资料**

```bash
curl http://localhost:8171/profiles/1
```

这段命令的作用：

- 调用 Controller
- 然后依次经过 Service、Repository、Component 和配置绑定对象
- 你能从一个接口结果里看到这些注解实际参与了哪些工作

---

## 五、这个项目里要重点记住什么

### 1. `@SpringBootApplication`

作用：

- Spring Boot 应用的总入口注解
- 通常放在主启动类上

### 2. `@ConfigurationProperties`

作用：

- 适合读取一组有层级结构的配置
- 比大量散落的 `@Value` 更清晰

### 3. `@Configuration` + `@Bean`

作用：

- 适合手动声明需要注册到 Spring 容器中的对象
- 常见于第三方工具类、格式化器、客户端对象等

### 4. `@Component`、`@Service`、`@Repository`

作用：

- 它们本质上都能被 Spring 扫描为 Bean
- 但语义不同，分别适合通用组件、业务层、数据访问层

### 5. `@Value`

作用：

- 适合读取单个简单配置
- 如果是一大组配置，优先考虑 `@ConfigurationProperties`

---

## 六、常见报错排查

### 1. `No qualifying bean of type 'AppProperties'`

原因：

- 你漏写了 `@ConfigurationPropertiesScan`
- 或者 `AppProperties` 的包不在 Spring Boot 扫描范围内

### 2. `Could not resolve placeholder 'custom.welcome-message'`

原因：

- 你在 `application.yml` 里没有配置这个属性
- 或者属性名写错了

### 3. `Parameter 0 of constructor required a bean of type 'DateTimeFormatter'`

原因：

- 你漏写了 `BeanConfig` 上的 `@Configuration`
- 或者漏写了 `dateTimeFormatter()` 上的 `@Bean`

---

## 七、你在这个练习里学到了什么

做完这个项目后，你应该已经掌握：

- Spring Boot 项目是怎么启动的
- Bean 是怎么被 Spring 容器扫描和注册的
- 配置是怎么绑定到 Java 对象里的
- `@Component`、`@Service`、`@Repository` 的职责区别
- 为什么说 Spring Boot 的很多注解，本质上是在帮你做对象管理和依赖注入
