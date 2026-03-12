# Kafka 练习套件设计文档

**Date:** 2026-03-12

## 目标

提供 3 个完全独立的 Spring Boot + Kafka 练习项目：

1. `kafka-order-event-demo`：练习最基础的生产者和消费者
2. `kafka-log-dispatch-demo`：练习一个 Topic 被多个 Consumer Group 消费
3. `kafka-order-sequence-demo`：练习基于消息 Key 保证同一业务键的有序消费

目标读者是 Kafka 初学者，需要中文 Markdown 文档，并且可以照着文档一步一步创建项目、创建文件、复制代码、启动 Docker 和测试接口。

## 为什么选择这 3 个场景

这 3 个场景覆盖了 Kafka 最常见的 3 类生产理解点：

- 怎么发消息、怎么收消息
- Consumer Group 到底是什么
- 为什么 Kafka 里经常强调“消息 Key”和“分区内有序”

## 为什么仍然拆成 3 个独立项目

沿用你之前 MQ、Redis、MyBatis、Security 文档已经确认过的交付方式：

- 每个场景单独一个 Spring Boot 项目
- 每个项目单独一份 `docker-compose.yml`
- 每个项目单独一个数据库
- 每个项目都可以独立启动、独立练习、独立删除

## 统一技术路线

- Java 17
- Spring Boot 3.2.5
- Spring for Apache Kafka
- Spring Web
- Spring Data JPA
- MySQL 8.0
- Apache Kafka Docker 镜像
- Docker Compose
- Maven
- Lombok

## 3 个生产风格场景

### 1. 订单事件通知

生产意义：

- 订单创建成功后，经常需要异步通知别的模块
- Kafka 很适合做这种事件投递

练习目标：

- 创建订单
- 发送订单创建事件到 Kafka
- 消费者收到事件后写通知日志

### 2. 操作日志分发

生产意义：

- 一条业务日志往往要被多个下游系统消费
- 比如审计系统、告警系统、行为分析系统

练习目标：

- 发送操作日志事件到一个 Topic
- 两个不同 Consumer Group 分别消费同一条消息
- 直观看到“同一条消息每个组都能收到”

### 3. 订单状态流转有序消费

生产意义：

- 同一个订单的状态变化必须按顺序处理
- Kafka 里通常会用同一个业务键作为消息 Key，让消息进入同一个分区

练习目标：

- 发送“订单创建、支付、发货、完成”事件
- 使用 `orderNo` 作为消息 Key
- 消费者按顺序处理同一个订单的事件

## 范围

包含：

- 3 个独立 Spring Boot 项目
- 1 份总设计文档
- 3 份详细实现文档
- Docker 启动步骤
- 完整目录结构
- 每个文件的创建顺序
- 完整代码和中文注释
- 每段代码下方都写“这段代码的作用”
- curl 测试步骤
- 常见报错说明

不包含：

- Kafka Streams
- Schema Registry
- Avro / Protobuf
- 事务消息
- 死信队列
- 延迟消息
- 高阶监控和告警

## 端口规划

为了避免本机冲突，3 个项目分别使用不同端口：

- `kafka-order-event-demo`
  - 应用端口：`8121`
  - MySQL：`3319`
  - Kafka 外部端口：`9093`
- `kafka-log-dispatch-demo`
  - 应用端口：`8122`
  - MySQL：`3320`
  - Kafka 外部端口：`9094`
- `kafka-order-sequence-demo`
  - 应用端口：`8123`
  - MySQL：`3321`
  - Kafka 外部端口：`9095`

## 文档交付物

将创建以下 4 个文档：

- `docs/plans/2026-03-12-kafka-practice-suite-design.md`
- `docs/plans/2026-03-12-kafka-order-event-demo.md`
- `docs/plans/2026-03-12-kafka-log-dispatch-demo.md`
- `docs/plans/2026-03-12-kafka-order-sequence-demo.md`

## 推荐学习顺序

1. `kafka-order-event-demo`
2. `kafka-log-dispatch-demo`
3. `kafka-order-sequence-demo`

原因：

- 先掌握最基础的发消息和收消息
- 再理解 Consumer Group 的语义
- 最后理解 Key、Partition 和顺序消费
