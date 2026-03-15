# springboot-core-annotations-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot 项目，用一个最小可运行示例系统练习 Spring Boot 开发中最常见的核心注解，包括启动、配置绑定、Bean 注册和分层组件注解。

**Architecture:** 这个练习项目用一个简单的“站点资料查询”示例把核心注解串起来。主链路是 `Controller -> Service -> Repository`，并通过 `@ConfigurationProperties`、`@Configuration`、`@Bean` 和 `@Value` 演示 Spring 容器和配置绑定的基础能力。

**Tech Stack:** Java 17, Spring Boot 3.5.7, Spring Web, Spring Validation, Maven, Lombok, JUnit 5

---

## 一、这个专题在真实开发里为什么重要

很多 Spring Boot 新手会背一堆注解名，但不知道它们分别解决什么问题。

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
│   │   │   │   └── TimeTextComponent.java
│   │   │   ├── config
│   │   │   │   ├── SiteProperties.java
│   │   │   │   └── ToolConfig.java
│   │   │   ├── controller
│   │   │   │   └── SiteController.java
│   │   │   ├── model
│   │   │   │   └── SiteInfo.java
│   │   │   ├── repository
│   │   │   │   └── SiteRepository.java
│   │   │   └── service
│   │   │       └── SiteService.java
│   │   └── resources
│   │       └── application.yml
│   └── test
│       └── java/com/example/springbootcoreannotations
│           └── SpringBootCoreAnnotationsApplicationTests.java
```

这段目录结构的作用：

- 让你先看清整个练习项目要创建哪些文件
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
        <version>3.5.7</version>
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

// @SpringBootApplication 是 Spring Boot 项目的总入口注解。
// 它本质上组合了 @SpringBootConfiguration、@EnableAutoConfiguration、@ComponentScan。
@SpringBootApplication
// @ConfigurationPropertiesScan 会自动扫描并注册 @ConfigurationProperties 类。
// 这样我们就不需要手动把配置绑定类写成 Bean。
@ConfigurationPropertiesScan
public class SpringBootCoreAnnotationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootCoreAnnotationsApplication.class, args);
    }
}
```

这段代码的作用：

- `@SpringBootApplication` 是 Spring Boot 项目最核心的启动注解
- `@ConfigurationPropertiesScan` 会让 Spring 自动扫描配置绑定类

**Step 3: 创建 `application.yml`**

```yaml
server:
  port: 8191

spring:
  application:
    name: springboot-core-annotations-demo

site:
  profile:
    site-name: "Spring Boot 注解练习站"
    owner-name: "demo-admin"
    default-city: "Shanghai"

custom:
  welcome-text: "欢迎进入 Spring Boot 核心注解专题"
```

这段代码的作用：

- 把应用端口和应用名固定下来
- `site.profile` 这组配置后面会绑定到 `@ConfigurationProperties` 对象上
- `custom.welcome-text` 后面会用 `@Value` 注解读取

**Step 4: 创建测试类 `SpringBootCoreAnnotationsApplicationTests.java`**

```java
package com.example.springbootcoreannotations;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// @SpringBootTest 会启动完整的 Spring Boot 测试上下文。
// 这里的目的不是测业务逻辑，而是先确认项目能够正常启动。
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
- Create: `src/main/java/com/example/springbootcoreannotations/model/SiteInfo.java`

**Step 1: 创建 `Result.java`**

```java
package com.example.springbootcoreannotations.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// @Data 会自动生成 getter、setter、toString 等常用方法。
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

**Step 2: 创建 `SiteInfo.java`**

```java
package com.example.springbootcoreannotations.model;

import lombok.Builder;
import lombok.Data;

// @Data 用来减少样板代码。
@Data
// @Builder 让我们在构建对象时写法更清晰。
@Builder
public class SiteInfo {

    private String siteName;
    private String ownerName;
    private String city;
    private String welcomeText;
    private String currentTimeText;
}
```

这段代码的作用：

- 这是这个练习项目里返回给前端的站点信息模型
- 后面 Service 会把配置数据、仓储数据和组件处理结果组装成这个对象

---

### Task 3: 创建配置类，练习 `@ConfigurationProperties`、`@Configuration`、`@Bean`

**Files:**
- Create: `src/main/java/com/example/springbootcoreannotations/config/SiteProperties.java`
- Create: `src/main/java/com/example/springbootcoreannotations/config/ToolConfig.java`

**Step 1: 创建 `SiteProperties.java`**

```java
package com.example.springbootcoreannotations.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

// @Data 负责自动生成 getter、setter。
@Data
// @ConfigurationProperties 会把 application.yml 里指定前缀的配置绑定到当前对象。
// 这里绑定的是 site.profile 这一组配置。
@ConfigurationProperties(prefix = "site.profile")
public class SiteProperties {

    private String siteName;
    private String ownerName;
    private String defaultCity;
}
```

这段代码的作用：

- `@ConfigurationProperties` 会把 `application.yml` 里的 `site.profile` 配置绑定到这个对象
- 这比到处写 `@Value` 更适合读取一整组结构化配置

**Step 2: 创建 `ToolConfig.java`**

```java
package com.example.springbootcoreannotations.config;

import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// @Configuration 表示这是一个配置类。
// Spring 在启动时会把它当成 Bean 定义的来源之一。
@Configuration
public class ToolConfig {

    // @Bean 表示把当前方法返回的对象交给 Spring 容器管理。
    // 后面其他 Bean 就可以直接注入这个 DateTimeFormatter。
    @Bean
    public DateTimeFormatter dateTimeFormatter() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }
}
```

这段代码的作用：

- `@Configuration` 表示这是一个配置类
- `@Bean` 表示把方法返回值交给 Spring 容器管理

---

### Task 4: 创建组件层、仓储层、服务层，练习 `@Component`、`@Repository`、`@Service`

**Files:**
- Create: `src/main/java/com/example/springbootcoreannotations/component/TimeTextComponent.java`
- Create: `src/main/java/com/example/springbootcoreannotations/repository/SiteRepository.java`
- Create: `src/main/java/com/example/springbootcoreannotations/service/SiteService.java`

**Step 1: 创建 `TimeTextComponent.java`**

```java
package com.example.springbootcoreannotations.component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// @Component 是最通用的 Spring 组件注解。
// 只要类被扫描到，Spring 就会把它注册成一个 Bean。
@Component
@RequiredArgsConstructor
public class TimeTextComponent {

    private final DateTimeFormatter dateTimeFormatter;

    public String nowText() {
        return LocalDateTime.now().format(dateTimeFormatter);
    }
}
```

这段代码的作用：

- `@Component` 是最通用的 Spring 组件注解
- 这个类会被 Spring 自动扫描并注册成 Bean

**Step 2: 创建 `SiteRepository.java`**

```java
package com.example.springbootcoreannotations.repository;

import java.util.Map;
import org.springframework.stereotype.Repository;

// @Repository 通常放在数据访问层。
// 真实项目里这里可能会调用 MyBatis、JPA 或外部存储。
@Repository
public class SiteRepository {

    public Map<String, String> loadExtraInfo() {
        return Map.of("tagline", "learn by running code");
    }
}
```

这段代码的作用：

- `@Repository` 通常放在数据访问层
- 这里虽然没有接数据库，但它的职责仍然是“提供数据”

**Step 3: 创建 `SiteService.java`**

```java
package com.example.springbootcoreannotations.service;

import com.example.springbootcoreannotations.component.TimeTextComponent;
import com.example.springbootcoreannotations.config.SiteProperties;
import com.example.springbootcoreannotations.model.SiteInfo;
import com.example.springbootcoreannotations.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// @Service 通常放在业务层。
// 它本质上也是一个组件注解，但语义上更强调“这里是业务逻辑”。
@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteProperties siteProperties;
    private final TimeTextComponent timeTextComponent;
    private final SiteRepository siteRepository;

    // @Value 适合读取单个简单配置。
    // 如果是一大组配置，更推荐用 @ConfigurationProperties。
    @Value("${custom.welcome-text}")
    private String welcomeText;

    public SiteInfo getSiteInfo() {
        siteRepository.loadExtraInfo();

        return SiteInfo.builder()
            .siteName(siteProperties.getSiteName())
            .ownerName(siteProperties.getOwnerName())
            .city(siteProperties.getDefaultCity())
            .welcomeText(welcomeText)
            .currentTimeText(timeTextComponent.nowText())
            .build();
    }
}
```

这段代码的作用：

- `@Service` 通常放在业务层
- 这个类把配置类的数据、仓储层的数据和组件层的能力组合起来
- 同时演示了 `@ConfigurationProperties` 和 `@Value` 两种取配置方式

---

### Task 5: 创建 Controller，观察这些核心注解如何串成完整请求链路

**Files:**
- Create: `src/main/java/com/example/springbootcoreannotations/controller/SiteController.java`

**Step 1: 创建 `SiteController.java`**

```java
package com.example.springbootcoreannotations.controller;

import com.example.springbootcoreannotations.common.Result;
import com.example.springbootcoreannotations.model.SiteInfo;
import com.example.springbootcoreannotations.service.SiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @RestController 表示这是一个返回 JSON 的控制器。
// 它相当于 @Controller + @ResponseBody 的组合。
@RestController
// @RequestMapping 用来给当前控制器定义统一的路径前缀。
@RequestMapping("/site")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    // @GetMapping 表示这个方法处理 GET 请求。
    @GetMapping("/info")
    public Result<SiteInfo> info() {
        return Result.success(siteService.getSiteInfo());
    }
}
```

这段代码的作用：

- 虽然这份文档重点是核心容器注解，但一个最小可运行项目仍然需要一个接口入口
- 这个 Controller 会把请求交给 `SiteService`

---

## 三、启动项目

**Step 1: 启动 Spring Boot**

```bash
mvn spring-boot:run
```

这段命令的作用：

- 启动当前 Spring Boot 项目
- 启动后应用会监听 `8191` 端口

---

## 四、接口测试

**Step 1: 查询站点信息**

```bash
curl http://localhost:8191/site/info
```

这段命令的作用：

- 调用 Controller
- 然后依次经过 Service、Repository、Component 和配置绑定对象
- 你能从一个接口结果里看到这些注解实际参与了哪些工作

---

## 五、这个专题里要重点记住什么

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
- 常见于工具类、客户端对象、第三方组件

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

### 1. `No qualifying bean of type 'SiteProperties'`

原因：

- 你漏写了 `@ConfigurationPropertiesScan`
- 或者 `SiteProperties` 的包不在 Spring Boot 扫描范围内

### 2. `Could not resolve placeholder 'custom.welcome-text'`

原因：

- 你在 `application.yml` 里没有配置这个属性
- 或者属性名写错了

### 3. `Parameter 0 of constructor required a bean of type 'DateTimeFormatter'`

原因：

- 你漏写了 `ToolConfig` 上的 `@Configuration`
- 或者漏写了 `dateTimeFormatter()` 上的 `@Bean`

---

## 七、你在这个专题里学到了什么

做完这个项目后，你应该已经掌握：

- Spring Boot 项目是怎么启动的
- Bean 是怎么被 Spring 容器扫描和注册的
- 配置是怎么绑定到 Java 对象里的
- `@Component`、`@Service`、`@Repository` 的职责区别
- 为什么说 Spring Boot 的很多注解，本质上是在帮你做对象管理和依赖注入
