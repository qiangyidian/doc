# redis-login-token-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot 项目，完成“用户登录后，把 Token 会话信息存入 Redis”的练习模块，让初学者理解 Redis 在登录态管理里的常见用法。

**Architecture:** 这个项目的核心链路是 `Controller -> AuthService -> MySQL / Redis`。登录时先校验 MySQL 中的用户名密码，再生成 token，把登录用户信息以 JSON 的形式写入 Redis 并设置 TTL；查询当前用户和退出登录则直接操作 Redis 中的 token key。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Maven, Spring Web, Spring Data Redis, MyBatis-Plus 3.5.15, MySQL 8.0, Redis 7.2, Docker Compose, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 Redis

登录态如果只保存在应用进程内，会有几个问题：

1. 应用一重启，登录态就丢了
2. 多实例部署时，登录态难共享
3. 会话过期不好统一管理

Redis 很适合做这件事，因为它支持：

- 快速读写
- 设置过期时间
- 多实例共享会话

常见做法就是：

1. 用户登录成功
2. 生成 token
3. 把 token 对应的用户信息写入 Redis
4. 后续接口根据 token 从 Redis 取当前登录用户

---

## 二、最终目录结构

```text
redis-login-token-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/logintoken
│   │   │   ├── LoginTokenApplication.java
│   │   │   ├── common
│   │   │   │   ├── Constants.java
│   │   │   │   └── Result.java
│   │   │   ├── controller
│   │   │   │   └── AuthController.java
│   │   │   ├── dto
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── LoginResponse.java
│   │   │   │   └── LoginUserInfo.java
│   │   │   ├── entity
│   │   │   │   └── UserAccount.java
│   │   │   ├── mapper
│   │   │   │   └── UserAccountMapper.java
│   │   │   └── service
│   │   │       ├── AuthService.java
│   │   │       └── impl
│   │   │           └── AuthServiceImpl.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── data.sql
│   │       └── schema.sql
│   └── test
│       └── java/com/example/logintoken/LoginTokenApplicationTests.java
```

---

## 三、端口规划

- 应用端口：`8092`
- MySQL：`3311`
- Redis：`6381`

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/logintoken/LoginTokenApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/logintoken/LoginTokenApplicationTests.java`

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
    <artifactId>redis-login-token-demo</artifactId>
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
    container_name: redis-login-token-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: login_token_db
    ports:
      - "3311:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - redis-login-token-mysql-data:/var/lib/mysql

  redis:
    image: redis:7.2
    container_name: redis-login-token-redis
    restart: always
    ports:
      - "6381:6379"
    command: ["redis-server", "--appendonly", "yes"]
    volumes:
      - redis-login-token-redis-data:/data

volumes:
  redis-login-token-mysql-data:
  redis-login-token-redis-data:
```

**Step 3: 创建启动类 `LoginTokenApplication.java`**

```java
package com.example.logintoken;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.logintoken.mapper")
public class LoginTokenApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoginTokenApplication.class, args);
    }
}
```

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8092

spring:
  application:
    name: redis-login-token-demo

  datasource:
    url: jdbc:mysql://localhost:3311/login_token_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6381

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
DROP TABLE IF EXISTS t_user_account;

CREATE TABLE t_user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password VARCHAR(64) NOT NULL COMMENT '密码',
    nickname VARCHAR(64) NOT NULL COMMENT '昵称',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
);
```

**Step 6: 创建 `data.sql`**

```sql
INSERT INTO t_user_account (username, password, nickname, create_time, update_time)
VALUES ('zhangsan', '123456', '张三', NOW(), NOW()),
       ('lisi', '123456', '李四', NOW(), NOW());
```

**Step 7: 创建测试类 `LoginTokenApplicationTests.java`**

```java
package com.example.logintoken;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LoginTokenApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

---

### Task 2: 创建通用类、实体类、Mapper 和 DTO

**Files:**
- Create: `src/main/java/com/example/logintoken/common/Constants.java`
- Create: `src/main/java/com/example/logintoken/common/Result.java`
- Create: `src/main/java/com/example/logintoken/entity/UserAccount.java`
- Create: `src/main/java/com/example/logintoken/mapper/UserAccountMapper.java`
- Create: `src/main/java/com/example/logintoken/dto/LoginRequest.java`
- Create: `src/main/java/com/example/logintoken/dto/LoginResponse.java`
- Create: `src/main/java/com/example/logintoken/dto/LoginUserInfo.java`

**Step 1: 创建 `Constants.java`**

```java
package com.example.logintoken.common;

public final class Constants {

    private Constants() {
    }

    public static final String LOGIN_TOKEN_PREFIX = "login:token:";

    /**
     * Token 默认过期时间，单位：分钟。
     */
    public static final long TOKEN_EXPIRE_MINUTES = 30L;
}
```

**Step 2: 创建 `Result.java`**

```java
package com.example.logintoken.common;

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

**Step 3: 创建实体类 `UserAccount.java`**

```java
package com.example.logintoken.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_account")
public class UserAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

**Step 4: 创建 Mapper `UserAccountMapper.java`**

```java
package com.example.logintoken.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.logintoken.entity.UserAccount;

public interface UserAccountMapper extends BaseMapper<UserAccount> {
}
```

**Step 5: 创建 DTO**

`LoginRequest.java`

```java
package com.example.logintoken.dto;

import lombok.Data;

@Data
public class LoginRequest {

    private String username;

    private String password;
}
```

`LoginResponse.java`

```java
package com.example.logintoken.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private Long expireMinutes;
}
```

`LoginUserInfo.java`

```java
package com.example.logintoken.dto;

import lombok.Data;

/**
 * 这个对象表示“当前登录用户”写入 Redis 后的结构。
 */
@Data
public class LoginUserInfo {

    private Long userId;

    private String username;

    private String nickname;
}
```

---

### Task 3: 创建 Service 和 Controller

**Files:**
- Create: `src/main/java/com/example/logintoken/service/AuthService.java`
- Create: `src/main/java/com/example/logintoken/service/impl/AuthServiceImpl.java`
- Create: `src/main/java/com/example/logintoken/controller/AuthController.java`

**Step 1: 创建 `AuthService.java`**

```java
package com.example.logintoken.service;

import com.example.logintoken.dto.LoginRequest;
import com.example.logintoken.dto.LoginResponse;
import com.example.logintoken.dto.LoginUserInfo;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginUserInfo getCurrentUser(String token);

    void logout(String token);
}
```

**Step 2: 创建 `AuthServiceImpl.java`**

```java
package com.example.logintoken.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.logintoken.common.Constants;
import com.example.logintoken.dto.LoginRequest;
import com.example.logintoken.dto.LoginResponse;
import com.example.logintoken.dto.LoginUserInfo;
import com.example.logintoken.entity.UserAccount;
import com.example.logintoken.mapper.UserAccountMapper;
import com.example.logintoken.service.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserAccountMapper userAccountMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public AuthServiceImpl(UserAccountMapper userAccountMapper,
                           StringRedisTemplate stringRedisTemplate,
                           ObjectMapper objectMapper) {
        this.userAccountMapper = userAccountMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        UserAccount userAccount = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>()
                        .eq(UserAccount::getUsername, request.getUsername())
                        .eq(UserAccount::getPassword, request.getPassword())
                        .last("limit 1")
        );

        if (userAccount == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        String token = UUID.randomUUID().toString().replace("-", "");

        LoginUserInfo loginUserInfo = new LoginUserInfo();
        loginUserInfo.setUserId(userAccount.getId());
        loginUserInfo.setUsername(userAccount.getUsername());
        loginUserInfo.setNickname(userAccount.getNickname());

        try {
            stringRedisTemplate.opsForValue().set(
                    Constants.LOGIN_TOKEN_PREFIX + token,
                    objectMapper.writeValueAsString(loginUserInfo),
                    Constants.TOKEN_EXPIRE_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("登录用户信息序列化失败", e);
        }

        return new LoginResponse(token, Constants.TOKEN_EXPIRE_MINUTES);
    }

    @Override
    public LoginUserInfo getCurrentUser(String token) {
        String cacheValue = stringRedisTemplate.opsForValue().get(Constants.LOGIN_TOKEN_PREFIX + token);
        if (cacheValue == null) {
            return null;
        }

        try {
            return objectMapper.readValue(cacheValue, LoginUserInfo.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("登录用户信息反序列化失败", e);
        }
    }

    @Override
    public void logout(String token) {
        // 退出登录本质上就是删除 token 对应的 Redis key。
        stringRedisTemplate.delete(Constants.LOGIN_TOKEN_PREFIX + token);
    }
}
```

**Step 3: 创建控制器 `AuthController.java`**

```java
package com.example.logintoken.controller;

import com.example.logintoken.common.Result;
import com.example.logintoken.dto.LoginRequest;
import com.example.logintoken.dto.LoginResponse;
import com.example.logintoken.dto.LoginUserInfo;
import com.example.logintoken.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<LoginUserInfo> currentUser(@RequestParam String token) {
        return Result.success(authService.getCurrentUser(token));
    }

    @PostMapping("/logout")
    public Result<String> logout(@RequestParam String token) {
        authService.logout(token);
        return Result.success("退出登录成功");
    }
}
```

---

### Task 4: 启动项目并验证 Token 会话链路

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

**Step 3: 登录获取 token**

Run:

```bash
curl -X POST "http://localhost:8092/auth/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"zhangsan\",\"password\":\"123456\"}"
```

Expected:

- 返回一串 `token`
- 返回 `expireMinutes=30`

**Step 4: 用 token 查询当前登录用户**

Run:

```bash
curl "http://localhost:8092/auth/me?token=这里替换成你的token"
```

Expected:

- 能返回 `userId`、`username`、`nickname`

**Step 5: 退出登录**

Run:

```bash
curl -X POST "http://localhost:8092/auth/logout?token=这里替换成你的token"
```

Expected:

- Redis 里对应 token key 被删除

**Step 6: 再次查询当前登录用户**

Run:

```bash
curl "http://localhost:8092/auth/me?token=这里替换成你的token"
```

Expected:

- 返回 `null`，表示 Redis 里已经没有该登录态

---

### Task 5: 常见错误排查

**问题 1：登录接口报用户名或密码错误**

排查：

- `data.sql` 是否已经插入了测试用户
- 用户名和密码是否输入成了别的值

**问题 2：登录成功了，但 `/auth/me` 查不到用户**

排查：

- 是否把 token 完整复制了
- `Constants.LOGIN_TOKEN_PREFIX` 是否和查询时保持一致
- Redis 端口是否是 `6381`

**问题 3：为什么要给 token 设置 TTL**

原因：

- 登录态不能永久有效
- TTL 能让 Redis 自动过期无效会话
- 这也是 Redis 管理会话的一个核心价值

---

## 你做完这个项目后应该掌握什么

1. Token 会话为什么适合存到 Redis
2. TTL 在登录态管理里的作用是什么
3. 为什么 `/auth/me` 可以只查 Redis 而不查 MySQL
4. 退出登录为什么本质上就是删除 Redis key
