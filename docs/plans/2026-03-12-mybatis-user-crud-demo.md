# mybatis-user-crud-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot + MyBatis 项目，完成“用户管理单表 CRUD”的练习模块，让初学者理解 MyBatis 最基础的 `Mapper 接口 + XML SQL` 写法。

**Architecture:** 这个项目的核心链路是 `Controller -> Service -> Mapper -> MySQL`。Controller 负责接收接口请求，Service 负责业务编排，Mapper 接口负责定义方法，真正的 SQL 写在 `resources/mapper/*.xml` 中，这是最经典的 MyBatis 分层方式。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Web, MyBatis Spring Boot Starter 3.0.4, MySQL 8.0, Docker Compose, Maven, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 MyBatis

在很多后台管理系统里，最常见的模块就是：

- 用户管理
- 角色管理
- 菜单管理
- 字典管理

这类模块往往就是标准的单表 CRUD。  
这时很多团队会选择 MyBatis，因为：

1. SQL 是自己写的，可控性高
2. 查询字段、条件、排序都能自己决定
3. 非常适合做“我知道自己要写什么 SQL”的业务

---

## 二、最终目录结构

```text
mybatis-user-crud-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/usercrud
│   │   │   ├── UserCrudApplication.java
│   │   │   ├── common
│   │   │   │   └── Result.java
│   │   │   ├── controller
│   │   │   │   └── UserController.java
│   │   │   ├── dto
│   │   │   │   ├── CreateUserRequest.java
│   │   │   │   └── UpdateUserRequest.java
│   │   │   ├── entity
│   │   │   │   └── UserInfo.java
│   │   │   ├── mapper
│   │   │   │   └── UserInfoMapper.java
│   │   │   └── service
│   │   │       ├── UserService.java
│   │   │       └── impl
│   │   │           └── UserServiceImpl.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── data.sql
│   │       ├── mapper
│   │       │   └── UserInfoMapper.xml
│   │       └── schema.sql
│   └── test
│       └── java/com/example/usercrud/UserCrudApplicationTests.java
```

---

## 三、端口规划

- 应用端口：`8101`
- MySQL：`3313`

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/usercrud/UserCrudApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/usercrud/UserCrudApplicationTests.java`

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
    <artifactId>mybatis-user-crud-demo</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
        <mybatis.version>3.0.4</mybatis.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- MyBatis 官方 Spring Boot Starter -->
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>${mybatis.version}</version>
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
    container_name: mybatis-user-crud-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: user_crud_db
    ports:
      - "3313:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    volumes:
      - mybatis-user-crud-mysql-data:/var/lib/mysql

volumes:
  mybatis-user-crud-mysql-data:
```

**Step 3: 创建启动类 `UserCrudApplication.java`**

```java
package com.example.usercrud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目启动类。
 *
 * @MapperScan 用来扫描 mapper 接口，
 * 这样 MyBatis 才能为接口生成代理对象。
 */
@SpringBootApplication
@MapperScan("com.example.usercrud.mapper")
public class UserCrudApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserCrudApplication.class, args);
    }
}
```

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8101

spring:
  application:
    name: mybatis-user-crud-demo

  datasource:
    url: jdbc:mysql://localhost:3313/user_crud_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.usercrud.entity
  configuration:
    map-underscore-to-camel-case: true
```

说明：

- `mapper-locations` 告诉 MyBatis 去哪里找 XML
- `type-aliases-package` 让 XML 里可以直接写实体类名
- `map-underscore-to-camel-case: true` 让 `user_name` 自动映射到 `userName`

**Step 5: 创建 `schema.sql`**

```sql
DROP TABLE IF EXISTS t_user_info;

CREATE TABLE t_user_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_name VARCHAR(64) NOT NULL COMMENT '用户名',
    real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
    age INT NOT NULL COMMENT '年龄',
    phone VARCHAR(32) NOT NULL COMMENT '手机号',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
);
```

**Step 6: 创建 `data.sql`**

```sql
INSERT INTO t_user_info (user_name, real_name, age, phone, create_time, update_time)
VALUES ('zhangsan', '张三', 18, '13800000001', NOW(), NOW()),
       ('lisi', '李四', 20, '13800000002', NOW(), NOW());
```

**Step 7: 创建测试类 `UserCrudApplicationTests.java`**

```java
package com.example.usercrud;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserCrudApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

---

### Task 2: 创建通用类、实体类、DTO、Mapper 接口和 XML

**Files:**
- Create: `src/main/java/com/example/usercrud/common/Result.java`
- Create: `src/main/java/com/example/usercrud/entity/UserInfo.java`
- Create: `src/main/java/com/example/usercrud/dto/CreateUserRequest.java`
- Create: `src/main/java/com/example/usercrud/dto/UpdateUserRequest.java`
- Create: `src/main/java/com/example/usercrud/mapper/UserInfoMapper.java`
- Create: `src/main/resources/mapper/UserInfoMapper.xml`

**Step 1: 创建统一返回对象 `Result.java`**

```java
package com.example.usercrud.common;

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

**Step 2: 创建实体类 `UserInfo.java`**

```java
package com.example.usercrud.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类，对应数据库表 t_user_info。
 */
@Data
public class UserInfo {

    private Long id;

    private String userName;

    private String realName;

    private Integer age;

    private String phone;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

**Step 3: 创建 DTO**

`CreateUserRequest.java`

```java
package com.example.usercrud.dto;

import lombok.Data;

@Data
public class CreateUserRequest {

    private String userName;

    private String realName;

    private Integer age;

    private String phone;
}
```

`UpdateUserRequest.java`

```java
package com.example.usercrud.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {

    private String realName;

    private Integer age;

    private String phone;
}
```

**Step 4: 创建 Mapper 接口 `UserInfoMapper.java`**

```java
package com.example.usercrud.mapper;

import com.example.usercrud.entity.UserInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mapper 接口只定义方法，不写具体 SQL。
 *
 * 真正的 SQL 在 UserInfoMapper.xml 里。
 */
public interface UserInfoMapper {

    int insertUser(UserInfo userInfo);

    UserInfo selectById(@Param("id") Long id);

    List<UserInfo> selectAll();

    int updateUser(UserInfo userInfo);

    int deleteById(@Param("id") Long id);
}
```

**Step 5: 创建 XML `UserInfoMapper.xml`**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.usercrud.mapper.UserInfoMapper">

    <!--
        单表 CRUD 是最适合初学者理解 MyBatis 的场景。
        namespace 必须和 Mapper 接口全限定名一致。
    -->

    <insert id="insertUser" parameterType="UserInfo" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO t_user_info (
            user_name,
            real_name,
            age,
            phone,
            create_time,
            update_time
        ) VALUES (
            #{userName},
            #{realName},
            #{age},
            #{phone},
            #{createTime},
            #{updateTime}
        )
    </insert>

    <select id="selectById" resultType="UserInfo">
        SELECT
            id,
            user_name,
            real_name,
            age,
            phone,
            create_time,
            update_time
        FROM t_user_info
        WHERE id = #{id}
    </select>

    <select id="selectAll" resultType="UserInfo">
        SELECT
            id,
            user_name,
            real_name,
            age,
            phone,
            create_time,
            update_time
        FROM t_user_info
        ORDER BY id DESC
    </select>

    <update id="updateUser" parameterType="UserInfo">
        UPDATE t_user_info
        SET
            real_name = #{realName},
            age = #{age},
            phone = #{phone},
            update_time = #{updateTime}
        WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM t_user_info
        WHERE id = #{id}
    </delete>

</mapper>
```

说明：

- `id` 必须和 Mapper 接口中的方法名一致
- `#{}` 是 MyBatis 的参数占位符
- `resultType="UserInfo"` 表示查询结果映射到 `UserInfo`
- `useGeneratedKeys="true"` 表示插入后自动回填主键

---

### Task 3: 创建 Service 和 Controller

**Files:**
- Create: `src/main/java/com/example/usercrud/service/UserService.java`
- Create: `src/main/java/com/example/usercrud/service/impl/UserServiceImpl.java`
- Create: `src/main/java/com/example/usercrud/controller/UserController.java`

**Step 1: 创建 `UserService.java`**

```java
package com.example.usercrud.service;

import com.example.usercrud.dto.CreateUserRequest;
import com.example.usercrud.dto.UpdateUserRequest;
import com.example.usercrud.entity.UserInfo;

import java.util.List;

public interface UserService {

    UserInfo createUser(CreateUserRequest request);

    UserInfo getById(Long id);

    List<UserInfo> listUsers();

    UserInfo updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);
}
```

**Step 2: 创建 `UserServiceImpl.java`**

```java
package com.example.usercrud.service.impl;

import com.example.usercrud.dto.CreateUserRequest;
import com.example.usercrud.dto.UpdateUserRequest;
import com.example.usercrud.entity.UserInfo;
import com.example.usercrud.mapper.UserInfoMapper;
import com.example.usercrud.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserInfoMapper userInfoMapper;

    public UserServiceImpl(UserInfoMapper userInfoMapper) {
        this.userInfoMapper = userInfoMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfo createUser(CreateUserRequest request) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserName(request.getUserName());
        userInfo.setRealName(request.getRealName());
        userInfo.setAge(request.getAge());
        userInfo.setPhone(request.getPhone());
        userInfo.setCreateTime(LocalDateTime.now());
        userInfo.setUpdateTime(LocalDateTime.now());

        userInfoMapper.insertUser(userInfo);
        return userInfo;
    }

    @Override
    public UserInfo getById(Long id) {
        return userInfoMapper.selectById(id);
    }

    @Override
    public List<UserInfo> listUsers() {
        return userInfoMapper.selectAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfo updateUser(Long id, UpdateUserRequest request) {
        UserInfo userInfo = userInfoMapper.selectById(id);
        if (userInfo == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        userInfo.setRealName(request.getRealName());
        userInfo.setAge(request.getAge());
        userInfo.setPhone(request.getPhone());
        userInfo.setUpdateTime(LocalDateTime.now());

        userInfoMapper.updateUser(userInfo);
        return userInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        userInfoMapper.deleteById(id);
    }
}
```

**Step 3: 创建控制器 `UserController.java`**

```java
package com.example.usercrud.controller;

import com.example.usercrud.common.Result;
import com.example.usercrud.dto.CreateUserRequest;
import com.example.usercrud.dto.UpdateUserRequest;
import com.example.usercrud.entity.UserInfo;
import com.example.usercrud.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public Result<UserInfo> createUser(@RequestBody CreateUserRequest request) {
        return Result.success(userService.createUser(request));
    }

    @GetMapping("/{id}")
    public Result<UserInfo> getUser(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @GetMapping
    public Result<List<UserInfo>> listUsers() {
        return Result.success(userService.listUsers());
    }

    @PutMapping("/{id}")
    public Result<UserInfo> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        return Result.success(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除成功");
    }
}
```

---

### Task 4: 启动项目并验证 CRUD 链路

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

**Step 3: 查询用户列表**

Run:

```bash
curl "http://localhost:8101/users"
```

**Step 4: 新增用户**

Run:

```bash
curl -X POST "http://localhost:8101/users" ^
  -H "Content-Type: application/json" ^
  -d "{\"userName\":\"wangwu\",\"realName\":\"王五\",\"age\":22,\"phone\":\"13800000003\"}"
```

**Step 5: 根据 ID 查询用户**

Run:

```bash
curl "http://localhost:8101/users/1"
```

**Step 6: 更新用户**

Run:

```bash
curl -X PUT "http://localhost:8101/users/1" ^
  -H "Content-Type: application/json" ^
  -d "{\"realName\":\"张三丰\",\"age\":28,\"phone\":\"13800009999\"}"
```

**Step 7: 删除用户**

Run:

```bash
curl -X DELETE "http://localhost:8101/users/2"
```

---

### Task 5: 常见错误排查

**问题 1：启动报 `Invalid bound statement`**

原因通常是：

- `UserInfoMapper.xml` 的 `namespace` 写错了
- XML 文件不在 `resources/mapper` 下
- `mybatis.mapper-locations` 配置错误

**问题 2：字段查出来是 `null`**

排查：

- `map-underscore-to-camel-case` 是否开启
- 数据库字段名和实体字段名是否能自动对应上

**问题 3：为什么 Mapper 接口里没有 SQL**

说明：

- 这就是 MyBatis 的经典写法
- 方法签名在接口里
- SQL 在 XML 里
- 接口和 XML 通过 `namespace + id` 关联起来

---

## 你做完这个项目后应该掌握什么

1. MyBatis 最基础的开发结构是什么
2. `Mapper 接口` 和 `Mapper.xml` 是怎么对应的
3. 单表 CRUD 的 SQL 应该怎么写
4. 为什么很多后台系统喜欢用 MyBatis 做管理模块
