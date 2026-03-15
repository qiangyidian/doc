# Spring Boot 编码与项目初始化工具指南

## 一、这一份文档解决什么问题

这份文档专门讲 Spring Boot 开发中最常见的一组“编码工具”和“项目初始化工具”。

很多人第一次学 Spring Boot，容易只盯着业务代码，却不知道项目最开始到底该怎么搭起来。  
实际上，一个 Spring Boot 项目通常会先经过下面这条链路：

1. 安装 JDK
2. 安装并配置 IDE
3. 用 Spring Initializr 创建项目
4. 用 Maven 管理依赖
5. 用 Lombok 减少样板代码
6. 用 Git 管理版本

---

## 二、JDK

### 1. JDK 是什么

JDK 是 Java 项目的运行基础。  
Spring Boot 本质上仍然是 Java 应用，所以没有 JDK，项目就无法编译和运行。

### 2. 在 PowerShell 中检查 JDK

```powershell
# 查看当前机器安装的 Java 版本
java -version

# 查看 Java 编译器版本
javac -version
```

这段代码的作用：

- `java -version` 用来确认当前运行时版本
- `javac -version` 用来确认当前编译器版本
- 如果这里看不到版本号，说明 JDK 还没有正确安装或环境变量还没有配置好

### 3. 你应该关注什么

- Spring Boot 3.x 通常要求 Java 17 或更高版本
- 初学阶段建议直接使用 Java 17
- 如果团队没有特殊要求，不要一开始就在多个 Java 版本之间来回切换

---

## 三、IntelliJ IDEA

### 1. IDEA 在 Spring Boot 开发里的作用

IDEA 是 Java 开发里非常常见的 IDE。  
在 Spring Boot 项目里，它最常见的价值有：

- 创建和管理 Maven 项目
- 自动识别 Spring Boot 启动类
- 调试接口和断点
- 管理依赖、配置文件和测试类

### 2. 建议优先关注的能力

你一开始不用把 IDEA 所有功能都学会，先掌握下面几项就够了：

- 打开项目
- 识别 `pom.xml`
- 运行启动类
- 搜索类和文件
- 设置断点
- 查看 Maven 依赖

### 3. 运行 Spring Boot 启动类时常见的 main 方法

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication 是 Spring Boot 应用的启动入口注解。
// IDEA 会自动把带这个注解的 main 方法识别为可运行的启动类。
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

这段代码的作用：

- 这是一个最基础的 Spring Boot 启动类
- IDEA 识别到 `main` 方法后，可以直接点击运行
- `@SpringBootApplication` 告诉 Spring Boot 从这个类开始做自动配置和组件扫描

---

## 四、Spring Initializr

### 1. Spring Initializr 是什么

Spring Initializr 是 Spring 官方提供的项目初始化工具。  
它的作用是：帮你快速生成一个标准的 Spring Boot 项目骨架。

你可以通过网页 [Spring Initializr](https://start.spring.io/) 直接选择：

- 项目类型
- Java 版本
- Spring Boot 版本
- Group 和 Artifact
- 依赖项

### 2. 一个典型的项目元信息

```text
Project: Maven
Language: Java
Spring Boot: 3.5.7
Group: com.example
Artifact: springboot-tools-demo
Name: springboot-tools-demo
Package name: com.example.springboottools
Packaging: Jar
Java: 17
Dependencies: Spring Web, Lombok, Spring Boot DevTools, Spring Boot Actuator
```

这段代码的作用：

- 这不是程序代码，而是一组初始化项目时要填写的核心参数
- `Group` 类似组织名或公司域名反写
- `Artifact` 通常就是项目名
- `Dependencies` 决定了项目一开始会带哪些 starter

### 3. 为什么推荐新手优先用它

- 结构规范
- 依赖选择直观
- 减少手动拼接项目骨架出错
- 更接近官方标准写法

---

## 五、Maven 与 Maven Wrapper

### 1. Maven 是什么

Maven 是 Java 项目最常见的依赖管理和构建工具之一。  
Spring Boot 项目里，你经常用 Maven 做这些事：

- 下载依赖
- 编译代码
- 运行测试
- 打包 jar
- 启动应用

### 2. `pom.xml` 的基础结构

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
    <artifactId>springboot-tools-demo</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
</project>
```

这段代码的作用：

- `parent` 指定 Spring Boot 官方父工程，统一常见依赖版本
- `java.version` 指定项目 Java 版本
- `spring-boot-starter-web` 用来提供 Web 开发基础能力

### 3. 常用 Maven 命令

```powershell
# 使用 Maven Wrapper 启动项目
.\mvnw.cmd spring-boot:run

# 清理并打包项目
.\mvnw.cmd clean package

# 运行测试
.\mvnw.cmd test
```

这段代码的作用：

- `spring-boot:run` 用于直接运行 Spring Boot 应用
- `clean package` 会先清理旧构建，再重新打包
- `test` 用于执行测试代码
- 优先用 `mvnw` 而不是系统全局 Maven，可以减少团队环境差异

---

## 六、Lombok

### 1. Lombok 解决什么问题

Java 类里经常会有很多重复样板代码，比如：

- `getter`
- `setter`
- `toString`
- 构造方法

Lombok 的作用是通过注解减少这些重复代码。

### 2. 添加 Lombok 依赖

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

这段代码的作用：

- 把 Lombok 引入项目
- 后面你可以直接使用 `@Data`、`@Getter`、`@Setter` 等注解
- 如果 IDEA 没有正确启用 Lombok 插件，代码可能会标红，但项目未必真的编译不过

### 3. Lombok 常见写法

```java
package com.example.demo.model;

import lombok.Data;

// @Data 会自动生成 getter、setter、toString、equals、hashCode。
// 它适合用在简单的数据传输对象或实体类上。
@Data
public class UserInfo {

    private Long id;

    private String username;

    private String email;
}
```

这段代码的作用：

- `@Data` 可以明显减少样板代码
- 对初学者来说，这能让你把注意力更多放在业务结构上
- 但在复杂领域模型里，不建议不加思考地到处使用 `@Data`

---

## 七、Git

### 1. Git 在 Spring Boot 项目里的作用

Git 不是 Spring Boot 专属工具，但在真实开发里几乎一定会和 Spring Boot 项目一起使用。

你通常会用它来：

- 记录改动历史
- 和团队协作
- 切换分支
- 回溯问题

### 2. 最基础的 Git 操作

```powershell
# 查看当前工作区状态
git status

# 把文件加入暂存区
git add pom.xml src

# 提交一次修改
git commit -m "feat: initialize spring boot project"
```

这段代码的作用：

- `git status` 用来看有哪些文件被修改
- `git add` 把改动加入本次提交
- `git commit` 把当前阶段的工作固化下来

### 3. 为什么要养成“小步提交”的习惯

- 容易回滚
- 容易定位问题
- 历史记录更清晰

---

## 八、推荐的项目初始化顺序

如果你从零开始创建一个 Spring Boot 项目，推荐按下面顺序来：

1. 先确认 JDK 版本
2. 用 Spring Initializr 创建 Maven 项目
3. 用 IDEA 打开项目
4. 先跑通启动类
5. 再补业务依赖
6. 再接数据库和中间件
7. 每完成一个阶段做一次 Git 提交

### 一个典型的初始化命令顺序

```powershell
# 进入项目目录
cd D:\project\springboot-tools-demo

# 先运行测试，确认骨架项目是正常的
.\mvnw.cmd test

# 再启动 Spring Boot 项目
.\mvnw.cmd spring-boot:run
```

这段代码的作用：

- 先执行测试，是为了确认项目骨架本身没有问题
- 再启动服务，是为了确认本地环境和依赖已经打通
- 这是一个比较稳妥的起步顺序

---

## 九、初学者最常见的坑

### 1. JDK 版本不对

表现：

- 项目一打开就报版本不兼容
- Maven 构建失败

### 2. IDEA 打开的不是 Maven 项目

表现：

- `pom.xml` 没被识别
- 依赖下载不下来

### 3. 没有使用 Maven Wrapper

表现：

- 团队里不同机器构建结果不一致

### 4. Lombok 插件没装好

表现：

- IDEA 标红
- 但命令行可能还能编译

---

## 十、这一份文档你应该记住什么

Spring Boot 开发不是“写业务类”这么简单。  
真正高频出现的第一批工具，其实是：

- JDK
- IDEA
- Spring Initializr
- Maven / Maven Wrapper
- Lombok
- Git

你只要先把这 6 个工具用顺手，后面学习数据库、缓存、消息队列时就会轻松很多。
