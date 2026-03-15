# Spring Boot 与 Spring Cloud 常见注解练习套件设计文档

**Date:** 2026-03-12

## 目标

提供 3 份中文练习文档，帮助初学者系统学习 Spring Boot 和 Spring Cloud 开发中最常见的一批注解：

1. `springboot-core-annotations-demo`：练习 Spring Boot 核心注解
2. `springboot-web-annotations-demo`：练习 Spring Web 常见控制器注解
3. `springcloud-common-annotations-demo`：练习 Spring Cloud 常见微服务注解

目标读者是 Spring 初学者，需要的是：

- 中文文档
- 按步骤创建文件
- 每段代码下方都有作用说明
- 尽量贴近真实开发，而不是只背注解定义

## 为什么这次不按“数据库 / MQ / 缓存”来拆，而是按“注解主题”来拆

你这次要练的是“常见注解”，不是某一个中间件。

所以更适合按注解使用场景来拆成 3 组：

- 应用启动和 Bean 管理
- Web 控制器和参数绑定
- 微服务注册发现与远程调用

这样拆的好处是：

- 你会更容易理解“这个注解到底在什么场景下用”
- 同一类注解会在同一个练习里集中出现
- 学完以后，你能把注解和业务代码联系起来，而不是死记硬背

## 为什么仍然保留“和之前一样的交付格式”

沿用你前面已经确认过的格式：

- 1 份总设计文档
- 3 份独立练习文档
- 每份文档都从目录结构开始
- 每份文档都给出完整代码
- 每段代码下方都写“这段代码的作用”

这样做的好处是：

- 你不需要重新适应新的文档风格
- 所有练习文档的阅读体验保持统一
- 后面你再继续生成别的专题时，也能继续复用同一种学习方式

## 统一技术路线

为了让 Spring Boot 和 Spring Cloud 的版本兼容更稳，这套文档统一采用：

- Java 17
- Spring Boot 3.3.13
- Spring Cloud 2023.0.6
- Maven
- Lombok
- Spring Web
- Spring Validation

Spring Cloud 文档里的服务注册中心使用：

- Consul
- Docker Compose 启动

## 为什么使用 Spring Boot 3.3.13 和 Spring Cloud 2023.0.6

这里不是随便拍一个版本，而是为了让 Spring Cloud 的注解示例更容易跑通。

根据 Spring 官方资料：

- Spring Cloud 2023.0.x 支持 Spring Boot 3.3.x 和 3.2.x
- Spring Cloud 2023.0.6 是 2023.0.x 的最终开源版本
- Spring Cloud 2023.0.6 官方发布说明明确提到它基于 Spring Boot 3.3.13

因此这套注解练习文档固定采用这组版本：

- Spring Boot 3.3.13
- Spring Cloud 2023.0.6

这样做的作用：

- Spring Boot 和 Spring Cloud 版本关系更清晰
- 你照着文档抄代码时，踩兼容坑的概率更低

## 3 个练习专题分别讲什么

### 1. Spring Boot 核心注解

重点注解：

- `@SpringBootApplication`
- `@Configuration`
- `@Bean`
- `@ConfigurationProperties`
- `@ConfigurationPropertiesScan`
- `@Component`
- `@Service`
- `@Repository`
- `@Value`

目标：

- 理解 Spring Boot 应用怎么启动
- 理解 Bean 是怎么被注册到容器里的
- 理解配置属性怎么绑定到对象

### 2. Spring Boot Web 常见注解

重点注解：

- `@RestController`
- `@RequestMapping`
- `@GetMapping`
- `@PostMapping`
- `@PutMapping`
- `@DeleteMapping`
- `@RequestParam`
- `@PathVariable`
- `@RequestBody`
- `@Valid`
- `@RestControllerAdvice`
- `@ExceptionHandler`

目标：

- 理解一个接口从 URL 到方法是怎么映射的
- 理解参数怎么从路径、查询串、请求体绑定进来
- 理解全局异常处理怎么做

### 3. Spring Cloud 常见注解

重点注解：

- `@EnableDiscoveryClient`
- `@EnableFeignClients`
- `@FeignClient`
- `@LoadBalanced`
- `@RefreshScope`

目标：

- 理解服务注册发现的基本思路
- 理解 Feign 声明式远程调用怎么写
- 理解 `@LoadBalanced` 为什么能用服务名发请求
- 理解 `@RefreshScope` 适合解决什么问题

## 为什么 Spring Cloud 部分保留 `@EnableDiscoveryClient`

根据 Spring Cloud Commons 官方文档：

- `@EnableDiscoveryClient` 这个注解仍然存在
- 但在当前版本里，它已经“不再是必须的”
- 只要类路径里有 `DiscoveryClient` 的实现，应用通常就会自动注册

这套文档里仍然保留它，原因是：

- 你这次学的是“常见注解”
- 这个注解在大量历史项目和面试题里仍然经常出现
- 保留下来更利于你建立完整认知

文档里会明确告诉你：它现在通常是“可选但常见”的注解。

## 为什么 Spring Cloud 部分选择 Consul

这套文档没有选 Eureka Server，也没有选 Nacos，主要是为了让环境更轻一点。

选择 Consul 的原因：

- Consul 官方提供 Docker 镜像
- 本地启动快
- 不需要再额外写一个注册中心服务端工程
- Spring Cloud Consul 属于 Spring Cloud 体系里的常见实现

## 文档交付物

将创建以下 4 个文档：

- `docs/plans/2026-03-12-spring-annotations-practice-suite-design.md`
- `docs/plans/2026-03-12-springboot-core-annotations-demo.md`
- `docs/plans/2026-03-12-springboot-web-annotations-demo.md`
- `docs/plans/2026-03-12-springcloud-common-annotations-demo.md`

## 每份实现文档的统一结构

每份文档都会包含：

- 这个主题在真实开发里为什么重要
- 最终目录结构
- 需要创建哪些文件
- 完整代码
- 每段代码下方的作用说明
- 启动方式
- 测试方式
- 常见报错排查

## 推荐学习顺序

1. `springboot-core-annotations-demo`
2. `springboot-web-annotations-demo`
3. `springcloud-common-annotations-demo`

原因：

- 先学会容器和 Bean 的基础概念
- 再学控制器和接口参数绑定
- 最后再学微服务调用和服务发现
