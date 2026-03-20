# security-jwt-role-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot 项目，完成“基于 JWT 做角色权限控制”的练习模块，让初学者理解 Spring Security 的认证和授权是两层不同概念。

**Architecture:** 这个项目沿用 JWT 无状态认证，但在认证通过之后，进一步利用角色信息控制不同接口的访问权限。请求级权限通过 `authorizeHttpRequests` 控制，方法级权限通过 `@PreAuthorize` 控制，两者配合完成常见后台权限模型。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Security, Spring Data JPA, JJWT 0.13.0, MySQL 8.0, Docker Compose, Maven, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 Spring Security 的角色权限

在业务系统里，“已经登录”不代表“什么都能干”。  
最常见的需求是：

- 普通用户可以看自己的信息
- 管理员可以访问后台报表
- 某些接口只允许特定角色访问

所以权限通常分成两层：

1. 认证：先确认你是不是合法用户
2. 授权：再判断你有没有访问这个接口的权限

---

## 二、最终目录结构

```text
security-jwt-role-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/securityrole
│   │   │   ├── SecurityRoleApplication.java
│   │   │   ├── common
│   │   │   │   └── Result.java
│   │   │   ├── controller
│   │   │   │   ├── AdminController.java
│   │   │   │   ├── AuthController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dto
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── LoginResponse.java
│   │   │   │   └── RegisterRequest.java
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
│       └── java/com/example/securityrole/SecurityRoleApplicationTests.java
```

---

## 三、端口规划

- 应用端口：`8112`
- MySQL：`3317`

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/securityrole/SecurityRoleApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/securityrole/SecurityRoleApplicationTests.java`

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
    <artifactId>security-jwt-role-demo</artifactId>
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
    container_name: security-jwt-role-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: security_role_db
    ports:
      - "3317:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - security-jwt-role-mysql-data:/var/lib/mysql

volumes:
  security-jwt-role-mysql-data:
```

**Step 3: 创建启动类 `SecurityRoleApplication.java`**

```java
package com.example.securityrole;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class SecurityRoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityRoleApplication.class, args);
    }
}
```

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8112

spring:
  application:
    name: security-jwt-role-demo

  datasource:
    url: jdbc:mysql://localhost:3317/security_role_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
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
-- 当前项目建议通过注册接口分别创建 USER 和 ADMIN 账户做测试。
```

**Step 7: 创建测试类 `SecurityRoleApplicationTests.java`**

```java
package com.example.securityrole;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SecurityRoleApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

---

### Task 2: 创建实体、Repository、DTO、Security 组件

**Files:**
- Create: `src/main/java/com/example/securityrole/common/Result.java`
- Create: `src/main/java/com/example/securityrole/entity/UserAccount.java`
- Create: `src/main/java/com/example/securityrole/repository/UserAccountRepository.java`
- Create: `src/main/java/com/example/securityrole/dto/RegisterRequest.java`
- Create: `src/main/java/com/example/securityrole/dto/LoginRequest.java`
- Create: `src/main/java/com/example/securityrole/dto/LoginResponse.java`
- Create: `src/main/java/com/example/securityrole/security/CustomUserDetailsService.java`
- Create: `src/main/java/com/example/securityrole/security/JwtTokenService.java`
- Create: `src/main/java/com/example/securityrole/security/JwtAuthenticationFilter.java`
- Create: `src/main/java/com/example/securityrole/security/SecurityConfig.java`

**Step 1: 创建通用类和实体**

`Result.java`

```java
package com.example.securityrole.common;

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
package com.example.securityrole.entity;

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
package com.example.securityrole.repository;

import com.example.securityrole.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);
}
```

**Step 2: 创建 DTO**

`RegisterRequest.java`

```java
package com.example.securityrole.dto;

import lombok.Data;

@Data
public class RegisterRequest {

    private String username;

    private String password;

    private String nickname;

    private String roleCode;
}
```

`LoginRequest.java`

```java
package com.example.securityrole.dto;

import lombok.Data;

@Data
public class LoginRequest {

    private String username;

    private String password;
}
```

`LoginResponse.java`

```java
package com.example.securityrole.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private String roleCode;
}
```

**Step 3: 创建 Security 组件**

`CustomUserDetailsService.java`

```java
package com.example.securityrole.security;

import com.example.securityrole.entity.UserAccount;
import com.example.securityrole.repository.UserAccountRepository;
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
package com.example.securityrole.security;

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
        return Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
```

`JwtAuthenticationFilter.java`

```java
package com.example.securityrole.security;

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
package com.example.securityrole.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
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
- Create: `src/main/java/com/example/securityrole/service/AuthService.java`
- Create: `src/main/java/com/example/securityrole/service/impl/AuthServiceImpl.java`
- Create: `src/main/java/com/example/securityrole/controller/AuthController.java`
- Create: `src/main/java/com/example/securityrole/controller/UserController.java`
- Create: `src/main/java/com/example/securityrole/controller/AdminController.java`

**Step 1: 创建 `AuthService.java`**

```java
package com.example.securityrole.service;

import com.example.securityrole.dto.LoginRequest;
import com.example.securityrole.dto.LoginResponse;
import com.example.securityrole.dto.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
```

**Step 2: 创建 `AuthServiceImpl.java`**

```java
package com.example.securityrole.service.impl;

import com.example.securityrole.dto.LoginRequest;
import com.example.securityrole.dto.LoginResponse;
import com.example.securityrole.dto.RegisterRequest;
import com.example.securityrole.entity.UserAccount;
import com.example.securityrole.repository.UserAccountRepository;
import com.example.securityrole.security.JwtTokenService;
import com.example.securityrole.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
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
        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(request.getUsername());
        userAccount.setPassword(passwordEncoder.encode(request.getPassword()));
        userAccount.setNickname(request.getNickname());
        userAccount.setRoleCode(request.getRoleCode() == null ? "USER" : request.getRoleCode());
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
        User user = (User) authentication.getPrincipal();

        String token = jwtTokenService.generateToken(user);
        String roleCode = user.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        return new LoginResponse(token, roleCode);
    }
}
```

**Step 3: 创建控制器**

`AuthController.java`

```java
package com.example.securityrole.controller;

import com.example.securityrole.common.Result;
import com.example.securityrole.dto.LoginRequest;
import com.example.securityrole.dto.LoginResponse;
import com.example.securityrole.dto.RegisterRequest;
import com.example.securityrole.service.AuthService;
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
package com.example.securityrole.controller;

import com.example.securityrole.common.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/profile")
    public Result<String> profile(@AuthenticationPrincipal UserDetails userDetails) {
        return Result.success("当前用户：" + userDetails.getUsername());
    }
}
```

`AdminController.java`

```java
package com.example.securityrole.controller;

import com.example.securityrole.common.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> dashboard() {
        return Result.success("欢迎进入管理员报表页面");
    }
}
```

---

### Task 4: 启动项目并验证角色权限链路

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

**Step 3: 注册普通用户**

Run:

```bash
curl -X POST "http://localhost:8112/auth/register" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"user1\",\"password\":\"123456\",\"nickname\":\"普通用户\",\"roleCode\":\"USER\"}"
```

**Step 4: 注册管理员**

Run:

```bash
curl -X POST "http://localhost:8112/auth/register" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin1\",\"password\":\"123456\",\"nickname\":\"管理员\",\"roleCode\":\"ADMIN\"}"
```

**Step 5: 用普通用户登录并访问接口**

- 登录拿 token
- 访问 `/user/profile` 应该成功
- 访问 `/admin/dashboard` 应该返回 403

**Step 6: 用管理员登录并访问接口**

- 登录拿 token
- 访问 `/user/profile` 应该成功
- 访问 `/admin/dashboard` 也应该成功

---

### Task 5: 常见错误排查

**问题 1：明明登录成功了，但访问管理员接口还是 403**

排查：

- 角色是否写成了 `ADMIN`
- `SimpleGrantedAuthority` 是否拼成了 `ROLE_ADMIN`
- `hasRole("ADMIN")` 和 `ROLE_ADMIN` 是否对应上

**问题 2：为什么接口权限和方法权限都写了**

说明：

- 接口权限适合做大范围限制
- 方法权限适合做细粒度控制
- 真实项目里这两种经常一起用

**问题 3：为什么注册接口允许自己传 roleCode**

说明：

- 这只是练习写法，目的是方便你快速验证权限效果
- 真正生产中不会允许前台用户随意指定自己的角色

---

## 你做完这个项目后应该掌握什么

1. 认证和授权有什么区别
2. `ROLE_USER` 和 `ROLE_ADMIN` 是怎么起作用的
3. 403 和 401 分别代表什么问题
4. `authorizeHttpRequests` 和 `@PreAuthorize` 应该怎么配合使用
