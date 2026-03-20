# security-jwt-login-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot 项目，完成“用户注册、登录签发 JWT，并使用 JWT 访问受保护接口”的练习模块，让初学者掌握 Spring Security + JWT 最基础的认证链路。

**Architecture:** 这个项目的核心链路是 `Controller -> Service -> Repository -> MySQL`，并由 `SecurityFilterChain + JWT Filter` 负责认证。用户登录成功后，服务端签发 JWT；后续请求携带 `Authorization: Bearer <token>`，过滤器负责解析 Token、校验用户身份，并把认证信息放进 Spring Security 上下文。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Security, Spring Data JPA, JJWT 0.13.0, MySQL 8.0, Docker Compose, Maven, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 Spring Security + JWT

很多前后端分离系统不再使用传统的服务端 Session，而是改用 JWT。

原因通常有 3 个：

1. 前端只需要保存 Token，不需要依赖服务端 Session
2. 服务端更容易做无状态扩展
3. 受保护接口只要校验 Bearer Token 就能识别用户身份

这个项目做的就是最小可跑通版本：

- 注册
- 登录
- 签发 JWT
- 使用 JWT 访问 `/users/me`

---

## 二、最终目录结构

```text
security-jwt-login-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/securitylogin
│   │   │   ├── SecurityLoginApplication.java
│   │   │   ├── common
│   │   │   │   └── Result.java
│   │   │   ├── controller
│   │   │   │   ├── AuthController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dto
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── LoginResponse.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   └── UserProfileResponse.java
│   │   │   ├── entity
│   │   │   │   └── UserAccount.java
│   │   │   ├── repository
│   │   │   │   └── UserAccountRepository.java
│   │   │   ├── security
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtTokenService.java
│   │   │   │   └── SecurityConfig.java
│   │   │   └── service
│   │   │       ├── AuthService.java
│   │   │       └── impl
│   │   │           └── AuthServiceImpl.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── data.sql
│   │       └── schema.sql
│   └── test
│       └── java/com/example/securitylogin/SecurityLoginApplicationTests.java
```

---

## 三、端口规划

- 应用端口：`8111`
- MySQL：`3316`

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/securitylogin/SecurityLoginApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/securitylogin/SecurityLoginApplicationTests.java`

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
    <artifactId>security-jwt-login-demo</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
        <jjwt.version>0.13.0</jjwt.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
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
    container_name: security-jwt-login-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: security_login_db
    ports:
      - "3316:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - security-jwt-login-mysql-data:/var/lib/mysql

volumes:
  security-jwt-login-mysql-data:
```

**Step 3: 创建启动类 `SecurityLoginApplication.java`**

```java
package com.example.securitylogin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SecurityLoginApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityLoginApplication.class, args);
    }
}
```

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8111

spring:
  application:
    name: security-jwt-login-demo

  datasource:
    url: jdbc:mysql://localhost:3316/security_login_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
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

jwt:
  secret: this-is-a-demo-jwt-secret-key-with-more-than-32-bytes
  expire-minutes: 30
```

**Step 5: 创建 `schema.sql`**

```sql
DROP TABLE IF EXISTS t_user_account;

CREATE TABLE t_user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码密文',
    nickname VARCHAR(64) NOT NULL COMMENT '昵称',
    role_code VARCHAR(32) NOT NULL COMMENT '角色编码',
    enabled TINYINT(1) NOT NULL COMMENT '是否启用',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
);
```

**Step 6: 创建 `data.sql`**

```sql
-- 当前项目不预置用户数据，建议先调用注册接口创建测试用户。
```

**Step 7: 创建测试类 `SecurityLoginApplicationTests.java`**

```java
package com.example.securitylogin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SecurityLoginApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

---

### Task 2: 创建实体、Repository、DTO、Security 组件

**Files:**
- Create: `src/main/java/com/example/securitylogin/common/Result.java`
- Create: `src/main/java/com/example/securitylogin/entity/UserAccount.java`
- Create: `src/main/java/com/example/securitylogin/repository/UserAccountRepository.java`
- Create: `src/main/java/com/example/securitylogin/dto/RegisterRequest.java`
- Create: `src/main/java/com/example/securitylogin/dto/LoginRequest.java`
- Create: `src/main/java/com/example/securitylogin/dto/LoginResponse.java`
- Create: `src/main/java/com/example/securitylogin/dto/UserProfileResponse.java`
- Create: `src/main/java/com/example/securitylogin/security/CustomUserDetailsService.java`
- Create: `src/main/java/com/example/securitylogin/security/JwtTokenService.java`
- Create: `src/main/java/com/example/securitylogin/security/JwtAuthenticationFilter.java`
- Create: `src/main/java/com/example/securitylogin/security/SecurityConfig.java`

**Step 1: 创建 `Result.java`**

```java
package com.example.securitylogin.common;

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

**Step 2: 创建实体和 Repository**

`UserAccount.java`

```java
package com.example.securitylogin.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_user_account")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String roleCode;

    private Boolean enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

`UserAccountRepository.java`

```java
package com.example.securitylogin.repository;

import com.example.securitylogin.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);
}
```

**Step 3: 创建 DTO**

`RegisterRequest.java`

```java
package com.example.securitylogin.dto;

import lombok.Data;

@Data
public class RegisterRequest {

    private String username;

    private String password;

    private String nickname;
}
```

`LoginRequest.java`

```java
package com.example.securitylogin.dto;

import lombok.Data;

@Data
public class LoginRequest {

    private String username;

    private String password;
}
```

`LoginResponse.java`

```java
package com.example.securitylogin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private Long expireMinutes;
}
```

`UserProfileResponse.java`

```java
package com.example.securitylogin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponse {

    private Long userId;

    private String username;

    private String nickname;

    private String roleCode;
}
```

**Step 4: 创建 `CustomUserDetailsService.java`**

```java
package com.example.securitylogin.security;

import com.example.securitylogin.entity.UserAccount;
import com.example.securitylogin.repository.UserAccountRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public CustomUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount userAccount = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        return new User(
                userAccount.getUsername(),
                userAccount.getPassword(),
                Boolean.TRUE.equals(userAccount.getEnabled()),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + userAccount.getRoleCode()))
        );
    }
}
```

**Step 5: 创建 `JwtTokenService.java`**

```java
package com.example.securitylogin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtTokenService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-minutes}")
    private Long expireMinutes;

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireMinutes * 60 * 1000);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(getSecretKey())
                .compact();
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
```

**Step 6: 创建 `JwtAuthenticationFilter.java`**

```java
package com.example.securitylogin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   CustomUserDetailsService customUserDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtTokenService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtTokenService.getUsername(token);
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request, response);
    }
}
```

**Step 7: 创建 `SecurityConfig.java`**

```java
package com.example.securitylogin.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService customUserDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

### Task 3: 创建 Service 和 Controller

**Files:**
- Create: `src/main/java/com/example/securitylogin/service/AuthService.java`
- Create: `src/main/java/com/example/securitylogin/service/impl/AuthServiceImpl.java`
- Create: `src/main/java/com/example/securitylogin/controller/AuthController.java`
- Create: `src/main/java/com/example/securitylogin/controller/UserController.java`

**Step 1: 创建 `AuthService.java`**

```java
package com.example.securitylogin.service;

import com.example.securitylogin.dto.LoginRequest;
import com.example.securitylogin.dto.LoginResponse;
import com.example.securitylogin.dto.RegisterRequest;
import com.example.securitylogin.dto.UserProfileResponse;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserProfileResponse currentUser(String username);
}
```

**Step 2: 创建 `AuthServiceImpl.java`**

```java
package com.example.securitylogin.service.impl;

import com.example.securitylogin.dto.LoginRequest;
import com.example.securitylogin.dto.LoginResponse;
import com.example.securitylogin.dto.RegisterRequest;
import com.example.securitylogin.dto.UserProfileResponse;
import com.example.securitylogin.entity.UserAccount;
import com.example.securitylogin.repository.UserAccountRepository;
import com.example.securitylogin.security.JwtTokenService;
import com.example.securitylogin.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AuthServiceImpl(UserAccountRepository userAccountRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtTokenService jwtTokenService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public void register(RegisterRequest request) {
        if (userAccountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(request.getUsername());
        userAccount.setPassword(passwordEncoder.encode(request.getPassword()));
        userAccount.setNickname(request.getNickname());
        userAccount.setRoleCode("USER");
        userAccount.setEnabled(true);
        userAccount.setCreateTime(LocalDateTime.now());
        userAccount.setUpdateTime(LocalDateTime.now());
        userAccountRepository.save(userAccount);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenService.generateToken(userDetails);
        return new LoginResponse(token, 30L);
    }

    @Override
    public UserProfileResponse currentUser(String username) {
        UserAccount userAccount = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        return new UserProfileResponse(
                userAccount.getId(),
                userAccount.getUsername(),
                userAccount.getNickname(),
                userAccount.getRoleCode()
        );
    }
}
```

**Step 3: 创建控制器**

`AuthController.java`

```java
package com.example.securitylogin.controller;

import com.example.securitylogin.common.Result;
import com.example.securitylogin.dto.LoginRequest;
import com.example.securitylogin.dto.LoginResponse;
import com.example.securitylogin.dto.RegisterRequest;
import com.example.securitylogin.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }
}
```

`UserController.java`

```java
package com.example.securitylogin.controller;

import com.example.securitylogin.common.Result;
import com.example.securitylogin.dto.UserProfileResponse;
import com.example.securitylogin.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public Result<UserProfileResponse> currentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return Result.success(authService.currentUser(userDetails.getUsername()));
    }
}
```

---

### Task 4: 启动项目并验证 JWT 登录链路

**Step 1: 启动 MySQL**

Run:

```bash
docker compose up -d
```

**Step 2: 启动应用**

Run:

```bash
mvn spring-boot:run
```

**Step 3: 注册用户**

Run:

```bash
curl -X POST "http://localhost:8111/auth/register" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"zhangsan\",\"password\":\"123456\",\"nickname\":\"张三\"}"
```

**Step 4: 登录获取 JWT**

Run:

```bash
curl -X POST "http://localhost:8111/auth/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"zhangsan\",\"password\":\"123456\"}"
```

Expected:

- 返回一串 `token`
- `expireMinutes=30`

**Step 5: 携带 Token 访问受保护接口**

Run:

```bash
curl "http://localhost:8111/users/me" ^
  -H "Authorization: Bearer 这里替换成你的token"
```

Expected:

- 返回当前登录用户信息

---

### Task 5: 常见错误排查

**问题 1：访问 `/users/me` 返回 401**

排查：

- 有没有带 `Authorization` 请求头
- 格式是不是 `Bearer 空格 token`
- token 是否过期

**问题 2：登录时报密码错误**

排查：

- 注册时密码是否保存成功
- 是否用了 `BCryptPasswordEncoder`

**问题 3：为什么还需要 JWT Filter**

原因：

- Spring Security 默认不会自己识别你自定义的 JWT
- 你需要在过滤器里解析请求头、校验 Token、把用户身份放到上下文里

---

## 你做完这个项目后应该掌握什么

1. Spring Security + JWT 最基础的认证链路是什么
2. Bearer Token 为什么能访问受保护接口
3. `SecurityFilterChain` 和自定义 JWT Filter 分别负责什么
4. 为什么密码要加密保存
