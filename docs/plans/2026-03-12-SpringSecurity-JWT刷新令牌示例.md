# security-jwt-refresh-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot 项目，完成“登录返回 Access Token 和 Refresh Token，并支持续期和退出登录”的练习模块，让初学者理解 JWT 在生产中的基础生命周期设计。

**Architecture:** 这个项目仍然使用 JWT 做无状态认证，但把 Token 拆成两类：短期 Access Token 和长期 Refresh Token。Access Token 负责访问资源接口；Refresh Token 负责续签新的 Access Token，并且在数据库里保存状态，支持退出登录时失效。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Security, Spring Data JPA, JJWT 0.13.0, MySQL 8.0, Docker Compose, Maven, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 Refresh Token

如果 Access Token 有效期太长，会有安全风险。  
如果 Access Token 有效期太短，用户又会频繁掉登录态。

所以很多系统会采用：

- 短期 `accessToken`
- 长期 `refreshToken`

典型流程是：

1. 登录时同时返回两种 Token
2. `accessToken` 过期后，用 `refreshToken` 去换新的 `accessToken`
3. 退出登录时，让 `refreshToken` 失效

---

## 二、最终目录结构

```text
security-jwt-refresh-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/securityrefresh
│   │   │   ├── SecurityRefreshApplication.java
│   │   │   ├── common
│   │   │   │   └── Result.java
│   │   │   ├── controller
│   │   │   │   ├── AuthController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dto
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── LogoutRequest.java
│   │   │   │   ├── RefreshTokenRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   └── TokenPairResponse.java
│   │   │   ├── entity
│   │   │   │   ├── RefreshTokenRecord.java
│   │   │   │   └── UserAccount.java
│   │   │   ├── repository
│   │   │   │   ├── RefreshTokenRepository.java
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
│       └── java/com/example/securityrefresh/SecurityRefreshApplicationTests.java
```

---

## 三、端口规划

- 应用端口：`8113`
- MySQL：`3318`

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/securityrefresh/SecurityRefreshApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/securityrefresh/SecurityRefreshApplicationTests.java`

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
    <artifactId>security-jwt-refresh-demo</artifactId>
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
    container_name: security-jwt-refresh-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: security_refresh_db
    ports:
      - "3318:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - security-jwt-refresh-mysql-data:/var/lib/mysql

volumes:
  security-jwt-refresh-mysql-data:
```

**Step 3: 创建启动类 `SecurityRefreshApplication.java`**

```java
package com.example.securityrefresh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SecurityRefreshApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityRefreshApplication.class, args);
    }
}
```

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8113

spring:
  application:
    name: security-jwt-refresh-demo

  datasource:
    url: jdbc:mysql://localhost:3318/security_refresh_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
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
  access-expire-minutes: 30
  refresh-expire-days: 7
```

**Step 5: 创建 `schema.sql`**

```sql
DROP TABLE IF EXISTS t_refresh_token;
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

CREATE TABLE t_refresh_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    refresh_token VARCHAR(512) NOT NULL COMMENT 'refresh token',
    revoked TINYINT(1) NOT NULL COMMENT '是否失效',
    expire_at DATETIME NOT NULL COMMENT '过期时间',
    create_time DATETIME NOT NULL COMMENT '创建时间'
);
```

**Step 6: 创建 `data.sql`**

```sql
-- 当前项目不预置用户，建议先走注册接口。
```

**Step 7: 创建测试类 `SecurityRefreshApplicationTests.java`**

```java
package com.example.securityrefresh;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SecurityRefreshApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

---

### Task 2: 创建实体、Repository、DTO、Security 组件

**Files:**
- Create: `src/main/java/com/example/securityrefresh/common/Result.java`
- Create: `src/main/java/com/example/securityrefresh/entity/UserAccount.java`
- Create: `src/main/java/com/example/securityrefresh/entity/RefreshTokenRecord.java`
- Create: `src/main/java/com/example/securityrefresh/repository/UserAccountRepository.java`
- Create: `src/main/java/com/example/securityrefresh/repository/RefreshTokenRepository.java`
- Create: `src/main/java/com/example/securityrefresh/dto/RegisterRequest.java`
- Create: `src/main/java/com/example/securityrefresh/dto/LoginRequest.java`
- Create: `src/main/java/com/example/securityrefresh/dto/RefreshTokenRequest.java`
- Create: `src/main/java/com/example/securityrefresh/dto/LogoutRequest.java`
- Create: `src/main/java/com/example/securityrefresh/dto/TokenPairResponse.java`
- Create: `src/main/java/com/example/securityrefresh/security/CustomUserDetailsService.java`
- Create: `src/main/java/com/example/securityrefresh/security/JwtTokenService.java`
- Create: `src/main/java/com/example/securityrefresh/security/JwtAuthenticationFilter.java`
- Create: `src/main/java/com/example/securityrefresh/security/SecurityConfig.java`

**Step 1: 创建通用类和实体**

`Result.java`

```java
package com.example.securityrefresh.common;

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

`UserAccount.java`

```java
package com.example.securityrefresh.entity;

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

`RefreshTokenRecord.java`

```java
package com.example.securityrefresh.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_refresh_token")
public class RefreshTokenRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String refreshToken;

    private Boolean revoked;

    private LocalDateTime expireAt;

    private LocalDateTime createTime;
}
```

**Step 2: 创建 Repository**

`UserAccountRepository.java`

```java
package com.example.securityrefresh.repository;

import com.example.securityrefresh.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);
}
```

`RefreshTokenRepository.java`

```java
package com.example.securityrefresh.repository;

import com.example.securityrefresh.entity.RefreshTokenRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenRecord, Long> {

    Optional<RefreshTokenRecord> findByRefreshToken(String refreshToken);
}
```

**Step 3: 创建 DTO**

`RegisterRequest.java`

```java
package com.example.securityrefresh.dto;

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
package com.example.securityrefresh.dto;

import lombok.Data;

@Data
public class LoginRequest {

    private String username;

    private String password;
}
```

`RefreshTokenRequest.java`

```java
package com.example.securityrefresh.dto;

import lombok.Data;

@Data
public class RefreshTokenRequest {

    private String refreshToken;
}
```

`LogoutRequest.java`

```java
package com.example.securityrefresh.dto;

import lombok.Data;

@Data
public class LogoutRequest {

    private String refreshToken;
}
```

`TokenPairResponse.java`

```java
package com.example.securityrefresh.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenPairResponse {

    private String accessToken;

    private String refreshToken;
}
```

**Step 4: 创建 Security 组件**

`CustomUserDetailsService.java`

```java
package com.example.securityrefresh.security;

import com.example.securityrefresh.entity.UserAccount;
import com.example.securityrefresh.repository.UserAccountRepository;
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
                List.of(new SimpleGrantedAuthority("ROLE_" + userAccount.getRoleCode()))
        );
    }
}
```

`JwtTokenService.java`

```java
package com.example.securityrefresh.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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

    @Value("${jwt.access-expire-minutes}")
    private Long accessExpireMinutes;

    @Value("${jwt.refresh-expire-days}")
    private Long refreshExpireDays;

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), accessExpireMinutes * 60 * 1000);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), refreshExpireDays * 24 * 60 * 60 * 1000);
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private String buildToken(String username, long expireMillis) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireMillis);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(getSecretKey())
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
```

`JwtAuthenticationFilter.java`

```java
package com.example.securityrefresh.security;

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

`SecurityConfig.java`

```java
package com.example.securityrefresh.security;

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
                        .requestMatchers("/auth/register", "/auth/login", "/auth/refresh", "/auth/logout").permitAll()
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
- Create: `src/main/java/com/example/securityrefresh/service/AuthService.java`
- Create: `src/main/java/com/example/securityrefresh/service/impl/AuthServiceImpl.java`
- Create: `src/main/java/com/example/securityrefresh/controller/AuthController.java`
- Create: `src/main/java/com/example/securityrefresh/controller/UserController.java`

**Step 1: 创建 `AuthService.java`**

```java
package com.example.securityrefresh.service;

import com.example.securityrefresh.dto.*;

public interface AuthService {

    void register(RegisterRequest request);

    TokenPairResponse login(LoginRequest request);

    TokenPairResponse refresh(RefreshTokenRequest request);

    void logout(LogoutRequest request);
}
```

**Step 2: 创建 `AuthServiceImpl.java`**

```java
package com.example.securityrefresh.service.impl;

import com.example.securityrefresh.dto.*;
import com.example.securityrefresh.entity.RefreshTokenRecord;
import com.example.securityrefresh.entity.UserAccount;
import com.example.securityrefresh.repository.RefreshTokenRepository;
import com.example.securityrefresh.repository.UserAccountRepository;
import com.example.securityrefresh.security.CustomUserDetailsService;
import com.example.securityrefresh.security.JwtTokenService;
import com.example.securityrefresh.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenService jwtTokenService;

    public AuthServiceImpl(UserAccountRepository userAccountRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           CustomUserDetailsService customUserDetailsService,
                           JwtTokenService jwtTokenService) {
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public void register(RegisterRequest request) {
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
    public TokenPairResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.getUsername());
        String accessToken = jwtTokenService.generateAccessToken(userDetails);
        String refreshToken = jwtTokenService.generateRefreshToken(userDetails);

        RefreshTokenRecord record = new RefreshTokenRecord();
        record.setUsername(userDetails.getUsername());
        record.setRefreshToken(refreshToken);
        record.setRevoked(false);
        record.setExpireAt(LocalDateTime.ofInstant(jwtTokenService.getExpiration(refreshToken).toInstant(), ZoneId.systemDefault()));
        record.setCreateTime(LocalDateTime.now());
        refreshTokenRepository.save(record);

        return new TokenPairResponse(accessToken, refreshToken);
    }

    @Override
    public TokenPairResponse refresh(RefreshTokenRequest request) {
        if (!jwtTokenService.isTokenValid(request.getRefreshToken())) {
            throw new IllegalArgumentException("refresh token 无效或已过期");
        }

        RefreshTokenRecord record = refreshTokenRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("refresh token 不存在"));

        if (Boolean.TRUE.equals(record.getRevoked())) {
            throw new IllegalArgumentException("refresh token 已失效");
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(record.getUsername());
        String newAccessToken = jwtTokenService.generateAccessToken(userDetails);

        return new TokenPairResponse(newAccessToken, request.getRefreshToken());
    }

    @Override
    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByRefreshToken(request.getRefreshToken()).ifPresent(record -> {
            record.setRevoked(true);
            refreshTokenRepository.save(record);
        });
    }
}
```

**Step 3: 创建控制器**

`AuthController.java`

```java
package com.example.securityrefresh.controller;

import com.example.securityrefresh.common.Result;
import com.example.securityrefresh.dto.*;
import com.example.securityrefresh.service.AuthService;
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
    public Result<TokenPairResponse> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public Result<TokenPairResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    public Result<String> logout(@RequestBody LogoutRequest request) {
        authService.logout(request);
        return Result.success("退出登录成功");
    }
}
```

`UserController.java`

```java
package com.example.securityrefresh.controller;

import com.example.securityrefresh.common.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/me")
    public Result<String> me(@AuthenticationPrincipal UserDetails userDetails) {
        return Result.success("当前登录用户：" + userDetails.getUsername());
    }
}
```

---

### Task 4: 启动项目并验证 Token 生命周期

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

**Step 3: 注册并登录**

- 先调用 `/auth/register`
- 再调用 `/auth/login`
- 拿到 `accessToken` 和 `refreshToken`

**Step 4: 用 accessToken 访问 `/users/me`**

请求头：

```text
Authorization: Bearer 你的accessToken
```

**Step 5: 用 refreshToken 续期**

Run:

```bash
curl -X POST "http://localhost:8113/auth/refresh" ^
  -H "Content-Type: application/json" ^
  -d "{\"refreshToken\":\"这里替换成你的refreshToken\"}"
```

Expected:

- 返回新的 `accessToken`

**Step 6: 退出登录**

Run:

```bash
curl -X POST "http://localhost:8113/auth/logout" ^
  -H "Content-Type: application/json" ^
  -d "{\"refreshToken\":\"这里替换成你的refreshToken\"}"
```

Expected:

- 当前 refresh token 被标记为失效

---

### Task 5: 常见错误排查

**问题 1：为什么退出登录后旧 accessToken 还能短时间使用**

原因：

- 这个项目是无状态 JWT
- 退出登录时失效的是 refresh token
- 旧 access token 会在自然过期前继续有效

这正是很多系统为什么会把 access token 设计得更短。

**问题 2：为什么 refresh token 还要落库**

原因：

- 只有落库，才能知道它是否被吊销
- 也才能支持退出登录、手动失效等动作

**问题 3：为什么这里没有做 access token 黑名单**

说明：

- 这是更进阶的话题
- 这份练习文档先聚焦最基础、最好理解的 refresh token 方案

---

## 你做完这个项目后应该掌握什么

1. access token 和 refresh token 的职责区别
2. 为什么 refresh token 通常要有状态管理
3. 为什么退出登录并不等于“立刻杀死所有 access token”
4. JWT 生命周期在生产里通常怎么设计
