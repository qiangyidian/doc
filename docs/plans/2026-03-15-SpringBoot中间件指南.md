# Spring Boot 常见中间件与本地启动方式指南

## 一、这一份文档解决什么问题

这份文档专门讲 Spring Boot 开发里最常见的一批中间件，以及它们在本地开发时通常怎么启动、怎么接入。

很多初学者一开始看到中间件会有两个困惑：

1. 这个中间件到底是干什么的
2. Spring Boot 项目里通常怎么连它

这份文档会把这两个问题拆开讲，并尽量给出最小可用示例。

---

## 二、MySQL

### 1. MySQL 在 Spring Boot 项目中的定位

MySQL 是最常见的关系型数据库。  
在 Spring Boot 项目里，它通常用来保存：

- 用户数据
- 订单数据
- 支付数据
- 配置数据

### 2. 本地 Docker Compose 示例

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: springboot-mysql
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: demo_db
    ports:
      - "3306:3306"
```

这段代码的作用：

- 用 Docker 启动一个本地 MySQL 8.0 容器
- `MYSQL_DATABASE` 会初始化一个数据库
- `3306:3306` 让本机 Spring Boot 可以直接访问容器中的 MySQL

### 3. Spring Boot 配置示例

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/demo_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
```

这段代码的作用：

- 告诉 Spring Boot 数据库连接地址、账号、密码和驱动类
- 后续无论你用 MyBatis、JPA 还是 JdbcTemplate，都会先依赖这一层数据源配置

---

## 三、Redis

### 1. Redis 在 Spring Boot 项目中的定位

Redis 最常见的场景有：

- 缓存
- 登录会话
- 分布式锁
- 计数器
- 热点数据加速访问

### 2. 本地 Docker Compose 示例

```yaml
services:
  redis:
    image: redis:7
    container_name: springboot-redis
    ports:
      - "6379:6379"
```

这段代码的作用：

- 启动一个本地 Redis 服务
- 默认把容器的 `6379` 端口映射到宿主机
- 适合本地开发和练习环境

### 3. Spring Boot 配置示例

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

这段代码的作用：

- 配置 Spring Boot 访问 Redis 的主机、端口和库号
- 后续项目里使用 `StringRedisTemplate` 或 `RedisTemplate` 时，会先读取这里的配置

### 4. 最小使用示例

```java
package com.example.demo.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void saveCode(String mobile, String code) {
        // 把手机验证码写入 Redis，key 设计成业务前缀 + 手机号。
        stringRedisTemplate.opsForValue().set("login:code:" + mobile, code);
    }
}
```

这段代码的作用：

- 演示 Spring Boot 中最常见的 Redis 使用入口之一
- `StringRedisTemplate` 适合处理字符串类型的 key/value
- 示例场景是把短信验证码放进 Redis

---

## 四、RabbitMQ

### 1. RabbitMQ 在 Spring Boot 项目中的定位

RabbitMQ 通常用于：

- 异步处理
- 业务解耦
- 削峰填谷

例如：

- 下单后异步发通知
- 支付成功后异步发积分
- 库存变更后异步同步其他系统

### 2. 本地 Docker Compose 示例

```yaml
services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: springboot-rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
```

这段代码的作用：

- 启动带管理后台的 RabbitMQ
- `5672` 是应用连接端口
- `15672` 是浏览器访问管理后台的端口

### 3. Spring Boot 配置示例

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

这段代码的作用：

- 配置 Spring Boot 访问 RabbitMQ 的连接信息
- 后续你写生产者、消费者时，会先依赖这里的连接配置

### 4. 最小消费者示例

```java
package com.example.demo.mq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderMessageConsumer {

    // @RabbitListener 用于监听指定队列。
    // 当队列里有消息时，这个方法会自动被 Spring 调用。
    @RabbitListener(queues = "order.notify.queue")
    public void consume(String message) {
        System.out.println("收到消息：" + message);
    }
}
```

这段代码的作用：

- 演示 RabbitMQ 消费者在 Spring Boot 中最常见的写法
- `@RabbitListener` 会把方法注册成消息监听器
- 这就是异步消费最基础的入口

---

## 五、Kafka

### 1. Kafka 在 Spring Boot 项目中的定位

Kafka 更适合：

- 高吞吐日志分发
- 事件流处理
- 大量消息传输
- 多消费组独立消费

如果你的业务更偏“事件总线”或“日志流”，Kafka 会更常见。

### 2. 本地 Docker Compose 示例

```yaml
services:
  kafka:
    image: apache/kafka:3.9.0
    container_name: springboot-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

这段代码的作用：

- 用单机模式启动一个本地 Kafka
- 适合学习和本地联调，不适合生产环境
- `ADVERTISED_LISTENERS` 很关键，决定客户端如何访问 Kafka

### 3. Spring Boot 配置示例

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: demo-group
      auto-offset-reset: earliest
```

这段代码的作用：

- `bootstrap-servers` 指定 Kafka 地址
- `group-id` 指定消费者组
- `auto-offset-reset: earliest` 适合本地学习时从头消费消息

---

## 六、Elasticsearch

### 1. Elasticsearch 在 Spring Boot 项目中的定位

Elasticsearch 最常见的两个方向是：

- 搜索
- 日志检索

例如：

- 商品搜索
- 文章全文搜索
- 运维日志查询

### 2. 本地 Docker Compose 示例

```yaml
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.10.4
    container_name: springboot-es
    environment:
      discovery.type: single-node
      xpack.security.enabled: "false"
    ports:
      - "9200:9200"
      - "9300:9300"
```

这段代码的作用：

- 以单节点模式启动 Elasticsearch
- 关闭安全功能，降低本地学习门槛
- `9200` 是最常见的 HTTP 访问端口

### 3. Spring Boot 配置示例

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
```

这段代码的作用：

- 告诉 Spring Boot 去哪里访问 Elasticsearch
- 后续无论你用 Spring Data Elasticsearch 还是官方 Java Client，都需要先有这个连接地址

---

## 七、Nginx

### 1. Nginx 在 Spring Boot 项目中的定位

Nginx 通常不直接嵌在 Spring Boot 项目里，而是作为前置反向代理层存在。  
最常见作用有：

- 反向代理
- 负载均衡
- 静态资源托管
- HTTPS 终止

### 2. 一个最小反向代理配置

```nginx
server {
    listen 80;
    server_name localhost;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

这段代码的作用：

- 所有访问 `80` 端口的请求都会被转发到 Spring Boot 的 `8080` 端口
- `proxy_set_header` 用来把原始请求信息继续带给后端服务
- 这是 Nginx 代理 Spring Boot 服务最常见的基础配置之一

---

## 八、一个整合式本地环境示例

如果你在本地做综合练习，可以只拉起你当前项目真正需要的服务。  
不要一开始把所有中间件都堆起来。

### 一个组合示例

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: demo_db
    ports:
      - "3306:3306"

  redis:
    image: redis:7
    ports:
      - "6379:6379"

  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"
```

这段代码的作用：

- 这是一个适合绝大多数 Spring Boot 入门项目的本地依赖组合
- 包含数据库、缓存和消息队列
- 你可以按实际项目需要删减，而不是一开始全开

### 启动命令

```powershell
docker compose up -d
```

这段代码的作用：

- 用后台模式启动当前 `docker-compose.yml` 里定义的全部服务
- 是本地拉起依赖环境最常见的一条命令

---

## 九、中间件选择的基本原则

不要一上来就问“项目是不是应该把这些中间件全用上”。  
更合理的思路应该是：

- 需要关系型数据时再用 MySQL
- 需要缓存和高频 KV 时再用 Redis
- 需要异步解耦时再用 RabbitMQ
- 需要高吞吐事件流时再用 Kafka
- 需要全文搜索时再用 Elasticsearch
- 需要统一代理层时再用 Nginx

工具是为业务服务的，不是堆栈越多越好。

---

## 十、初学者最常见的坑

### 1. 端口冲突

表现：

- 容器启动失败
- Spring Boot 连不上服务

### 2. 配置和容器不一致

表现：

- Docker 启动了 Redis，但 Spring Boot 配成了别的端口

### 3. 容器启动了，但服务没就绪

表现：

- 启动早期连接失败
- 多试几秒又恢复正常

### 4. 一口气上太多中间件

表现：

- 学习路径变乱
- 排查问题时不知道是哪一层出错

---

## 十一、这一份文档你应该记住什么

Spring Boot 开发中最常见的一批中间件，不是让你全部背下来，而是先建立“职责感”：

- MySQL 负责结构化数据
- Redis 负责缓存和高频 KV
- RabbitMQ / Kafka 负责消息
- Elasticsearch 负责搜索
- Nginx 负责前置代理

你只要先知道它们各自解决什么问题，再学会用 Docker 在本地把它们拉起来，后面进入具体项目开发时就会轻松很多。
