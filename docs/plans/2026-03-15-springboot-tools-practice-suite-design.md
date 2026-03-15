# Spring Boot 开发常用工具专题设计文档

**Date:** 2026-03-15

## 目标

提供 1 套专门面向 Spring Boot 初学者的“开发常用工具专题”文档，帮助你系统理解在 Spring Boot 项目开发里，经常会一起出现的几类工具：

1. 编码和项目初始化工具
2. 本地开发、调试和联调工具
3. 常见中间件与本地启动方式

这套专题的目标不是只列工具名字，而是让你知道：

- 这个工具是干什么的
- 在 Spring Boot 项目里什么时候会用到
- 最小可用配置怎么写
- 初学者最容易踩什么坑

---

## 为什么要单独学习“工具专题”

很多初学者刚学 Spring Boot 时，会把注意力全部放在 Controller、Service、Mapper 这些代码层面。

但真正做项目时，你每天接触到的不只是业务代码，还包括：

- 用什么工具创建项目
- 用什么方式管理依赖
- 怎么快速启动本地环境
- 怎么调接口、看日志、查健康状态
- 怎么连 MySQL、Redis、MQ、搜索引擎

如果这些工具没有系统学过，后面开发会一直“能抄代码，但不清楚整个工程为什么这样组织”。

---

## 这套专题拆成 3 份独立文档

### 1. 编码与项目初始化工具

文档文件：

- `2026-03-15-springboot-coding-tools-guide.md`

重点内容：

- JDK
- IntelliJ IDEA
- Spring Initializr
- Maven 和 Maven Wrapper
- Lombok
- Git

学习目标：

- 搭好 Spring Boot 项目的基础开发环境
- 理解项目从“空目录”到“可运行”的初始化过程
- 养成比较规范的开发习惯

### 2. 开发调试与联调工具

文档文件：

- `2026-03-15-springboot-dev-debug-tools-guide.md`

重点内容：

- Spring Boot DevTools
- Spring Boot Actuator
- 日志配置
- curl / Postman
- Docker Compose

学习目标：

- 提高本地开发效率
- 学会基本调试和接口联调
- 学会观察服务健康状态和运行信息

### 3. 常见中间件与本地启动方式

文档文件：

- `2026-03-15-springboot-middleware-guide.md`

重点内容：

- MySQL
- Redis
- RabbitMQ
- Kafka
- Elasticsearch
- Nginx

学习目标：

- 知道这些中间件分别解决什么问题
- 知道 Spring Boot 中通常如何接入
- 知道本地开发时怎么用 Docker 启动它们

---

## 统一技术基线

为了避免你在学习工具时因为版本差异遇到太多无关问题，这套文档统一采用下面的基线：

- Java 17
- Spring Boot 3.5.7
- Maven 3.9+
- IntelliJ IDEA
- Docker Desktop + Docker Compose
- Windows PowerShell 命令演示

说明：

- `Spring Boot 3.5.7` 更接近当前 Spring Boot 3.x 稳定线的常见使用方式
- `Java 17` 是当前 Spring Boot 3.x 项目非常常见的基础版本
- `Docker Compose` 是本地启动中间件最省事的方式

---

## 为什么选择这些工具

### 编码工具

- `JDK`：Spring Boot 项目的运行基础
- `IntelliJ IDEA`：Java 和 Spring Boot 开发里最常见的 IDE
- `Spring Initializr`：官方项目初始化工具
- `Maven`：依赖管理和构建工具
- `Lombok`：减少样板代码
- `Git`：版本管理基础工具

### 开发调试工具

- `DevTools`：提高本地热部署效率
- `Actuator`：查看健康状态和监控端点
- `日志配置`：排查问题的第一入口
- `curl / Postman`：接口调试最常用
- `Docker Compose`：本地一键拉起依赖环境

### 中间件

- `MySQL`：关系型数据存储
- `Redis`：缓存、分布式锁、会话、计数器
- `RabbitMQ`：异步消息和解耦
- `Kafka`：高吞吐事件流和日志分发
- `Elasticsearch`：搜索和日志检索
- `Nginx`：反向代理、静态资源、网关前置层

---

## 这套专题的学习顺序

建议你按下面顺序学习：

1. 先看“编码与项目初始化工具”
2. 再看“开发调试与联调工具”
3. 最后看“常见中间件与本地启动方式”

这样安排的原因：

- 先把开发环境搭清楚
- 再学会如何运行、调试、排错
- 最后再进入数据库、缓存、MQ、搜索等依赖组件

---

## 文档统一写法

为了让你和之前的 MQ、Redis、MyBatis 文档保持同样的阅读体验，这套文档统一遵守下面规则：

- 全部使用中文
- 每个命令块或配置块后面都补“这段代码的作用”
- 配置示例尽量给出最小可用写法
- 先讲“为什么要用”，再讲“怎么配置”
- 偏向初学者视角，不做过早抽象

---

## 你学完之后应该掌握什么

学完这 3 份文档后，你应该能建立一个比较完整的 Spring Boot 开发工具认知：

- 知道如何创建和管理一个 Spring Boot 项目
- 知道如何本地运行、调试和联调
- 知道常见中间件在项目中的定位
- 知道本地环境优先用 Docker 启动依赖
- 知道遇到问题时先看哪里、怎么排查

---

## 对应文档清单

1. `2026-03-15-springboot-tools-practice-suite-design.md`
2. `2026-03-15-springboot-coding-tools-guide.md`
3. `2026-03-15-springboot-dev-debug-tools-guide.md`
4. `2026-03-15-springboot-middleware-guide.md`
