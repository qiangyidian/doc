# springboot-advanced-annotations-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot 项目，系统练习更贴近真实项目的常见进阶注解，包括条件装配、多实现注入、懒加载、生命周期回调、异步和定时任务。

**Architecture:** 这个练习项目围绕一个“通知中心”示例展开。项目会通过 `@Profile`、`@Primary`、`@Qualifier` 解决多实现 Bean 注入，通过 `@ConditionalOnProperty` 和 `@Import` 管理条件装配，通过 `@PostConstruct`、`@EnableAsync`、`@Async`、`@EnableScheduling`、`@Scheduled` 演示常见运行期能力。

**Tech Stack:** Java 17, Spring Boot 3.5.7, Spring Web, Spring Boot Actuator, Maven, Lombok, JUnit 5

---

## 一、这个专题在真实开发里为什么重要

很多人在学 Spring Boot 时，前面会很快学会：

- `@Component`
- `@Service`
- `@RestController`

但一到真实项目里，就会立刻遇到下面这些问题：

- 同一个接口有多个实现，Spring 到底注入哪一个
- 某些 Bean 只想在某个环境下生效，该怎么控制
- 某些 Bean 很重，不想一启动就创建，该怎么办
- 项目启动后要做一次初始化，该放哪里
- 定时任务和异步任务该怎么声明

这些问题背后，往往就是这批进阶注解在发挥作用：

- `@Profile`
- `@Primary`
- `@Qualifier`
- `@Lazy`
- `@Import`
- `@PostConstruct`
- `@EnableScheduling`
- `@Scheduled`
- `@EnableAsync`
- `@Async`
- `@ConditionalOnProperty`

这份文档的目标，就是把这些“真实项目里很常见，但初学者经常搞混”的注解放进一个完整练习里讲清楚。

---

## 二、最终目录结构

```text
springboot-advanced-annotations-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/springbootadvancedannotations
│   │   │   ├── SpringBootAdvancedAnnotationsApplication.java
│   │   │   ├── common
│   │   │   │   └── Result.java
│   │   │   ├── component
│   │   │   │   └── StartupLogger.java
│   │   │   ├── config
│   │   │   │   ├── BannerConfig.java
│   │   │   │   └── ThirdPartyToolConfig.java
│   │   │   ├── controller
│   │   │   │   └── NotifyController.java
│   │   │   ├── sender
│   │   │   │   ├── MessageSender.java
│   │   │   │   ├── EmailMessageSender.java
│   │   │   │   ├── SmsMessageSender.java
│   │   │   │   └── AuditMessageSender.java
│   │   │   └── service
│   │   │       ├── AsyncReportService.java
│   │   │       ├── MessageDispatchService.java
│   │   │       └── NotifyScheduler.java
│   │   └── resources
│   │       └── application.yml
│   └── test
│       └── java/com/example/springbootadvancedannotations
│           └── SpringBootAdvancedAnnotationsApplicationTests.java
```

这段目录结构的作用：

- 让你看到这份进阶专题不是只讲一个注解，而是讲一组在真实项目里经常一起出现的注解
- 这里的重点不是数据库和接口复杂度，而是 Bean 装配和运行期行为

---

### Task 1: 创建项目骨架

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/example/springbootadvancedannotations/SpringBootAdvancedAnnotationsApplication.java`
- Create: `src/main/resources/application.yml`
- Test: `src/test/java/com/example/springbootadvancedannotations/SpringBootAdvancedAnnotationsApplicationTests.java`

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
    <artifactId>springboot-advanced-annotations-demo</artifactId>
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
            <artifactId>spring-boot-starter-actuator</artifactId>
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

- 提供 Web 和 Actuator 能力
- 这份练习不需要数据库，但需要异步和定时任务的基础运行环境

**Step 2: 创建启动类 `SpringBootAdvancedAnnotationsApplication.java`**

```java
package com.example.springbootadvancedannotations;

import com.example.springbootadvancedannotations.config.ThirdPartyToolConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// @SpringBootApplication 是项目总入口。
@SpringBootApplication
// @EnableAsync 表示开启异步方法执行能力。
@EnableAsync
// @EnableScheduling 表示开启定时任务能力。
@EnableScheduling
// @Import 可以把外部配置类主动导入到当前 Spring 容器中。
@Import(ThirdPartyToolConfig.class)
public class SpringBootAdvancedAnnotationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootAdvancedAnnotationsApplication.class, args);
    }
}
```

这段代码的作用：

- 这里一次性演示了 3 个非常常见的进阶注解入口：`@EnableAsync`、`@EnableScheduling`、`@Import`
- 这些能力都需要在启动类层面显式开启

**Step 3: 创建 `application.yml`**

```yaml
server:
  port: 8193

spring:
  application:
    name: springboot-advanced-annotations-demo
  profiles:
    active: sms

notify:
  banner:
    enabled: true
```

这段代码的作用：

- `spring.profiles.active` 会决定哪个 `@Profile` Bean 生效
- `notify.banner.enabled` 后面会配合 `@ConditionalOnProperty` 使用

**Step 4: 创建测试类 `SpringBootAdvancedAnnotationsApplicationTests.java`**

```java
package com.example.springbootadvancedannotations;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringBootAdvancedAnnotationsApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

这段代码的作用：

- 用最基础的上下文测试先保证进阶配置没有导致启动失败
- 这对多注解组合场景尤其重要

---

### Task 2: 创建公共返回对象和配置类，练习 `@ConditionalOnProperty` 与 `@Import`

**Files:**
- Create: `src/main/java/com/example/springbootadvancedannotations/common/Result.java`
- Create: `src/main/java/com/example/springbootadvancedannotations/config/BannerConfig.java`
- Create: `src/main/java/com/example/springbootadvancedannotations/config/ThirdPartyToolConfig.java`

**Step 1: 创建 `Result.java`**

```java
package com.example.springbootadvancedannotations.common;

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
- 保持和前面两个专题的风格一致

**Step 2: 创建 `BannerConfig.java`**

```java
package com.example.springbootadvancedannotations.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// @ConditionalOnProperty 表示：只有当配置满足条件时，这个配置类里的 Bean 才会生效。
@ConditionalOnProperty(prefix = "notify.banner", name = "enabled", havingValue = "true")
public class BannerConfig {

    @Bean
    public String bannerText() {
        return "Spring Boot Advanced Annotation Banner";
    }
}
```

这段代码的作用：

- `@ConditionalOnProperty` 是 Spring Boot 自动装配体系里非常常见的条件注解
- 它特别适合做“功能开关”

**Step 3: 创建 `ThirdPartyToolConfig.java`**

```java
package com.example.springbootadvancedannotations.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThirdPartyToolConfig {

    // 当前配置类是通过启动类上的 @Import 导入到 Spring 容器中的。
    @Bean
    public String thirdPartyToolName() {
        return "third-party-tool";
    }
}
```

这段代码的作用：

- 这个类配合启动类上的 `@Import` 使用
- 用来演示“一个配置类不靠组件扫描，也可以被主动导入”

---

### Task 3: 创建 Sender 相关类，练习 `@Profile`、`@Primary`、`@Lazy`

**Files:**
- Create: `src/main/java/com/example/springbootadvancedannotations/sender/MessageSender.java`
- Create: `src/main/java/com/example/springbootadvancedannotations/sender/EmailMessageSender.java`
- Create: `src/main/java/com/example/springbootadvancedannotations/sender/SmsMessageSender.java`
- Create: `src/main/java/com/example/springbootadvancedannotations/sender/AuditMessageSender.java`

**Step 1: 创建 `MessageSender.java`**

```java
package com.example.springbootadvancedannotations.sender;

public interface MessageSender {

    String send(String message);
}
```

这段代码的作用：

- 先定义一个统一接口
- 后面多个实现类会演示 Spring 如何处理“同接口多实现”的注入问题

**Step 2: 创建 `EmailMessageSender.java`**

```java
package com.example.springbootadvancedannotations.sender;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
// @Profile("email") 表示当前 Bean 只会在 email 环境下生效。
@Profile("email")
public class EmailMessageSender implements MessageSender {

    @Override
    public String send(String message) {
        return "EMAIL:" + message;
    }
}
```

这段代码的作用：

- `@Profile` 可以根据当前激活环境决定 Bean 是否注册
- 这非常适合开发、测试、生产环境使用不同实现的场景

**Step 3: 创建 `SmsMessageSender.java`**

```java
package com.example.springbootadvancedannotations.sender;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("sms")
// @Primary 表示当同类型有多个候选 Bean 时，优先注入当前这个 Bean。
@Primary
public class SmsMessageSender implements MessageSender {

    @Override
    public String send(String message) {
        return "SMS:" + message;
    }
}
```

这段代码的作用：

- `@Primary` 用来解决“同一类型多个实现时默认用谁”的问题
- 这个注解在真实项目里非常常见

**Step 4: 创建 `AuditMessageSender.java`**

```java
package com.example.springbootadvancedannotations.sender;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
// @Lazy 表示当前 Bean 延迟初始化。
// 只有真正被使用时，Spring 才会创建它。
@Lazy
public class AuditMessageSender {

    public String record(String message) {
        return "AUDIT:" + message;
    }
}
```

这段代码的作用：

- `@Lazy` 适合那些创建成本较高、又不是一启动就必须用到的 Bean
- 它能帮助你理解“Bean 并不一定都要在启动时立刻创建”

---

### Task 4: 创建组件和服务类，练习 `@PostConstruct`、`@Async`、`@Qualifier`、`@Scheduled`

**Files:**
- Create: `src/main/java/com/example/springbootadvancedannotations/component/StartupLogger.java`
- Create: `src/main/java/com/example/springbootadvancedannotations/service/AsyncReportService.java`
- Create: `src/main/java/com/example/springbootadvancedannotations/service/MessageDispatchService.java`
- Create: `src/main/java/com/example/springbootadvancedannotations/service/NotifyScheduler.java`

**Step 1: 创建 `StartupLogger.java`**

```java
package com.example.springbootadvancedannotations.component;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class StartupLogger {

    // @PostConstruct 表示当前方法会在 Bean 初始化完成后自动执行一次。
    @PostConstruct
    public void afterInit() {
        System.out.println("StartupLogger initialized.");
    }
}
```

这段代码的作用：

- `@PostConstruct` 用来定义 Bean 初始化完成后的回调逻辑
- 这是 Spring 项目里非常常见的生命周期注解

**Step 2: 创建 `AsyncReportService.java`**

```java
package com.example.springbootadvancedannotations.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncReportService {

    // @Async 表示当前方法异步执行。
    @Async
    public void buildReport(String message) {
        System.out.println("async report start: " + message);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("async report end: " + message);
    }
}
```

这段代码的作用：

- `@Async` 让某些耗时动作异步执行
- 这是非常典型的“主流程不阻塞，后台异步处理”的注解写法

**Step 3: 创建 `MessageDispatchService.java`**

```java
package com.example.springbootadvancedannotations.service;

import com.example.springbootadvancedannotations.sender.AuditMessageSender;
import com.example.springbootadvancedannotations.sender.MessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageDispatchService {

    // 这里直接按类型注入 MessageSender。
    private final MessageSender messageSender;

    // @Qualifier 用来明确指定要注入哪个 Bean。
    @Qualifier("auditMessageSender")
    private final AuditMessageSender auditMessageSender;

    private final AsyncReportService asyncReportService;

    public String dispatch(String message) {
        String sendResult = messageSender.send(message);
        String auditResult = auditMessageSender.record(message);
        asyncReportService.buildReport(message);
        return sendResult + " | " + auditResult;
    }
}
```

这段代码的作用：

- 这里同时演示了默认按类型注入、`@Qualifier` 精确注入和 `@Async` 的组合使用
- 这是非常贴近真实项目的一段代码

**Step 4: 创建 `NotifyScheduler.java`**

```java
package com.example.springbootadvancedannotations.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotifyScheduler {

    // @Scheduled 表示这是一个定时任务方法。
    // fixedRate = 30000 表示每 30 秒执行一次。
    @Scheduled(fixedRate = 30000)
    public void ping() {
        System.out.println("scheduled ping from NotifyScheduler");
    }
}
```

这段代码的作用：

- `@Scheduled` 用来声明定时任务
- 它必须配合启动类上的 `@EnableScheduling` 一起使用

---

### Task 5: 创建 Controller，观察进阶注解是如何协同工作的

**Files:**
- Create: `src/main/java/com/example/springbootadvancedannotations/controller/NotifyController.java`

**Step 1: 创建 `NotifyController.java`**

```java
package com.example.springbootadvancedannotations.controller;

import com.example.springbootadvancedannotations.common.Result;
import com.example.springbootadvancedannotations.service.MessageDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final MessageDispatchService messageDispatchService;

    // ObjectProvider 可以安全地按需获取可能存在、也可能不存在的 Bean。
    private final ObjectProvider<String> bannerTextProvider;

    @GetMapping("/send")
    public Result<String> send(@RequestParam String message) {
        return Result.success(messageDispatchService.dispatch(message));
    }

    @GetMapping("/banner")
    public Result<String> banner() {
        return Result.success(bannerTextProvider.getIfAvailable(() -> "banner disabled"));
    }
}
```

这段代码的作用：

- 通过一个最小 Controller，把进阶注解相关功能对外暴露出来
- 你可以直接通过接口看到 `@ConditionalOnProperty`、`@Profile`、`@Async` 等注解的实际效果

---

## 三、启动项目

**Step 1: 启动 Spring Boot**

```bash
mvn spring-boot:run
```

这段命令的作用：

- 启动当前进阶注解练习项目
- 启动后应用会监听 `8193` 端口

---

## 四、接口测试

**Step 1: 发送一条通知**

```bash
curl "http://localhost:8193/notify/send?message=hello"
```

这段命令的作用：

- 触发 `MessageDispatchService`
- 同时观察 `@Profile`、`@Primary`、`@Qualifier` 和 `@Async` 的组合效果

**Step 2: 查看 banner 是否开启**

```bash
curl http://localhost:8193/notify/banner
```

这段命令的作用：

- 用来验证 `@ConditionalOnProperty` 是否生效
- 如果配置开启，你会看到 banner 文本；否则会返回 `banner disabled`

---

## 五、这个专题里要重点记住什么

### 1. `@Profile`

作用：

- 根据当前激活环境决定 Bean 是否注册
- 适合不同环境使用不同实现

### 2. `@Primary`

作用：

- 当同类型有多个 Bean 时，指定默认优先注入哪个

### 3. `@Qualifier`

作用：

- 当同类型 Bean 不止一个时，精确指定要注入哪一个

### 4. `@Lazy`

作用：

- 延迟初始化 Bean
- 适合昂贵对象或非启动必需对象

### 5. `@Import`

作用：

- 主动导入一个配置类或组件类
- 常见于模块化配置和第三方配置接入

### 6. `@PostConstruct`

作用：

- Bean 初始化完成后自动执行一次

### 7. `@EnableScheduling` + `@Scheduled`

作用：

- 开启并声明定时任务

### 8. `@EnableAsync` + `@Async`

作用：

- 开启并声明异步方法执行

### 9. `@ConditionalOnProperty`

作用：

- 根据配置开关决定是否注册某个 Bean
- 是 Spring Boot 自动装配里非常常见的条件注解

---

## 六、常见报错排查

### 1. `No qualifying bean of type 'MessageSender'`

原因：

- 当前激活的 profile 没有对应实现
- 比如你没开 `sms`，又没有 `email` 实现可用

### 2. `NoUniqueBeanDefinitionException`

原因：

- 同类型 Bean 太多
- 但你没有使用 `@Primary` 或 `@Qualifier` 解决冲突

### 3. `@Async` 没有异步效果

原因：

- 你漏写了启动类上的 `@EnableAsync`
- 或者你在同一个类里自调用 `@Async` 方法

### 4. `@Scheduled` 没有执行

原因：

- 你漏写了启动类上的 `@EnableScheduling`

### 5. banner 一直返回 disabled

原因：

- `notify.banner.enabled` 没开
- 或者 `@ConditionalOnProperty` 的前缀和属性名写错了

---

## 七、你在这个专题里学到了什么

做完这个项目后，你应该已经掌握：

- Spring Boot 里多实现 Bean 冲突怎么解决
- 条件装配、懒加载、生命周期回调分别适合什么场景
- 异步任务和定时任务的基础注解怎么写
- 为什么说进阶注解不是“冷门知识”，而是实际项目里经常出现的日常工具
