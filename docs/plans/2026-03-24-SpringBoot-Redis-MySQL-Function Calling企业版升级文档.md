# Spring Boot + Redis + MySQL + Function Calling 企业版升级文档

**目标：** 从零创建一个更接近企业真实项目的 AI Function Calling 示例系统。这个系统不再只用内存模拟业务，而是采用 `Spring Boot 3 + OpenAI Responses API + MySQL + Redis` 实现“智能订单客服助手”。用户可以通过自然语言查询订单、查询物流、创建售后工单；模型负责判断调用哪个工具，你的后端负责查 MySQL、读写 Redis、记录工具日志，并把真实业务结果回传给模型。

**适合谁：**

- 已经看过基础版 Function Calling 示例
- 想把 AI 工具调用真正接入业务系统
- 想知道 Redis 和 MySQL 在 AI 项目里分别承担什么角色

**技术栈：** `Java 17`、`Spring Boot 3.2.5`、`Spring Web`、`Spring Validation`、`Spring Data Redis`、`MyBatis`、`MySQL 8`、`Redis 7`、`OpenAI Responses API`、`RestClient`

---

## 一、这份“企业版升级文档”和基础版的区别是什么

基础版的目标是让你先理解 function calling 的最小闭环。  
而这份企业版的重点，是把这个闭环真正接到业务系统上。

这份文档会多出 4 个企业开发里很常见的能力：

1. **MySQL 保存真实业务数据**
   订单、物流、售后工单、工具调用日志都落库。

2. **Redis 承担缓存和上下文记忆**
   Redis 不只是缓存订单详情，还用来缓存物流结果、保存最近对话上下文、做简单的重复提交保护。

3. **工具执行要带用户上下文**
   模型只负责说“我要查订单 ORD-1001”，真正查询时后端会用当前用户 ID 限制数据范围，避免越权。

4. **工具调用有审计日志**
   企业项目里通常都要知道：模型调了什么工具、传了什么参数、结果是什么、成功还是失败。

---

## 二、这个项目在真实公司里对应什么场景

这份文档模拟的是一个很常见的企业后台场景：

- 用户在 App 或网页客服窗口里提问
- 问题如果只是普通咨询，模型直接回答
- 如果用户问订单、物流、售后问题，模型触发 function calling
- 后端再去查真实数据库或缓存
- 最后再由模型把结构化结果转换成用户看得懂的话

这类架构非常适合：

1. 电商客服助手
2. 商城订单机器人
3. 内部运营查询助手
4. 售后工单机器人
5. CRM 智能坐席助手

---

## 三、企业版整体架构

```text
用户 -> Spring Boot 接口 -> AI 助手服务
                        -> OpenAI Responses API
                        -> 模型返回 function_call
                        -> 工具分发器
                        -> MySQL 查询订单/物流/工单
                        -> Redis 缓存订单/物流/会话上下文
                        -> 工具执行结果回传模型
                        -> 最终自然语言答复返回用户
```

这段代码的作用：

- 用一条主线把整个系统串起来
- 这里最关键的点是：模型不直接碰数据库，只能通过你的工具层间接访问业务数据

---

## 四、Redis 和 MySQL 在这个项目里分别做什么

为了避免你以后把 Redis 和 MySQL 用混，我先把职责写清楚：

### 4.1 MySQL 负责什么

- 保存订单主数据
- 保存物流数据
- 保存售后工单
- 保存工具调用审计日志

也就是说：**MySQL 才是最终真实数据源。**

### 4.2 Redis 负责什么

- 缓存热点订单详情
- 缓存物流查询结果
- 保存用户最近几轮对话上下文
- 做简单的重复提交保护

也就是说：**Redis 是加速层和会话层，不是最终真相。**

---

## 五、项目目录结构

```text
ai-order-assistant-enterprise-demo
├── docker-compose.yml
├── init.sql
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.example.aienterprise
│   │   │       ├── AiEnterpriseApplication.java
│   │   │       ├── common
│   │   │       │   ├── Result.java
│   │   │       │   └── RedisKeyConstants.java
│   │   │       ├── config
│   │   │       │   └── OpenAiProperties.java
│   │   │       ├── controller
│   │   │       │   └── AiAssistantController.java
│   │   │       ├── dto
│   │   │       │   ├── AssistantChatRequest.java
│   │   │       │   └── AssistantChatResponse.java
│   │   │       ├── entity
│   │   │       │   ├── OrderInfo.java
│   │   │       │   ├── LogisticsInfo.java
│   │   │       │   ├── AfterSaleTicket.java
│   │   │       │   └── ToolCallLog.java
│   │   │       ├── mapper
│   │   │       │   ├── OrderInfoMapper.java
│   │   │       │   ├── LogisticsInfoMapper.java
│   │   │       │   ├── AfterSaleTicketMapper.java
│   │   │       │   └── ToolCallLogMapper.java
│   │   │       ├── service
│   │   │       │   ├── AiAssistantService.java
│   │   │       │   ├── OpenAiResponsesService.java
│   │   │       │   ├── ConversationMemoryService.java
│   │   │       │   ├── OrderToolService.java
│   │   │       │   ├── LogisticsToolService.java
│   │   │       │   ├── TicketToolService.java
│   │   │       │   └── ToolDispatcherService.java
│   │   │       └── util
│   │   │           └── ToolSchemaFactory.java
│   │   └── resources
│   │       └── application.yml
```

这段代码的作用：

- 这是你后面照着一步一步创建文件的完整目录
- 这个结构已经很接近真实企业 Java 项目的基本分层

---

## 六、先启动 MySQL 和 Redis

### 6.1 创建 `docker-compose.yml`

```yaml
version: "3.9"

services:
  mysql:
    image: mysql:8.0
    container_name: ai-enterprise-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root1234
      MYSQL_DATABASE: ai_enterprise_demo
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql

  redis:
    image: redis:7.2
    container_name: ai-enterprise-redis
    ports:
      - "6379:6379"
    command: [ "redis-server", "--appendonly", "yes" ]
```

这段代码的作用：

- 启动 `MySQL 8` 和 `Redis 7`
- MySQL 用来保存业务数据
- Redis 用来做缓存和会话上下文

### 6.2 启动命令

```powershell
docker compose up -d
```

这段代码的作用：

- 在后台启动 MySQL 和 Redis 容器

---

## 七、创建数据库脚本 `init.sql`

```sql
CREATE DATABASE IF NOT EXISTS ai_enterprise_demo DEFAULT CHARACTER SET utf8mb4;
USE ai_enterprise_demo;

CREATE TABLE IF NOT EXISTS t_order_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    goods_name VARCHAR(100) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no)
);

CREATE TABLE IF NOT EXISTS t_logistics_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    company VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    latest_trace VARCHAR(255) NOT NULL,
    estimated_arrival VARCHAR(64) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_after_sale_ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_no VARCHAR(64) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ticket_no (ticket_no)
);

CREATE TABLE IF NOT EXISTS t_tool_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    tool_name VARCHAR(100) NOT NULL,
    tool_args TEXT NOT NULL,
    tool_result TEXT,
    success_flag TINYINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO t_order_info (order_no, user_id, status, amount, goods_name)
VALUES ('ORD-1001', 10001, 'PAID', 199.00, '无线耳机'),
       ('ORD-1002', 10001, 'SHIPPED', 299.00, '机械键盘'),
       ('ORD-2001', 20001, 'PAID', 59.90, '手机支架');

INSERT INTO t_logistics_info (order_no, company, status, latest_trace, estimated_arrival)
VALUES ('ORD-1001', '顺丰', '运输中', '快件已到达杭州转运中心', '明天 18:00 前'),
       ('ORD-1002', '京东物流', '已签收', '包裹已由前台代收', '已送达'),
       ('ORD-2001', '圆通', '已揽收', '快件已由商家发出', '后天 20:00 前');
```

这段代码的作用：

- 创建这个项目所需的 4 张核心业务表
- 并插入几条演示数据，方便你马上测试 function calling

---

## 八、创建 `pom.xml`

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
    <artifactId>ai-order-assistant-enterprise-demo</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>3.0.3</version>
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

这段代码的作用：

- 在基础版文档的依赖上，增加了 `Redis`、`MyBatis`、`MySQL`
- 这意味着从这里开始，这个项目已经进入“真正能接业务数据”的阶段

---

## 九、创建配置文件 `application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: ai-order-assistant-enterprise-demo
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/ai_enterprise_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root1234
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      database: 0

mybatis:
  configuration:
    map-underscore-to-camel-case: true

openai:
  base-url: https://api.openai.com/v1
  api-key: ${OPENAI_API_KEY:}
  model: gpt-5-mini
```

这段代码的作用：

- 把项目连接到 MySQL、Redis、OpenAI API
- 你后面所有的订单查询、缓存、上下文保存、工具调用都依赖这里的配置

---

## 十、设置 OpenAI API Key

```powershell
$env:OPENAI_API_KEY="你的OpenAI_API_Key"
```

这段代码的作用：

- 给当前终端会话注入 OpenAI API Key
- 避免把密钥直接写进代码或提交进 Git

---

## 十一、创建启动类 `AiEnterpriseApplication.java`

```java
package com.example.aienterprise;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.aienterprise.mapper")
@SpringBootApplication
public class AiEnterpriseApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiEnterpriseApplication.class, args);
    }
}
```

这段代码的作用：

- 启动 Spring Boot 项目
- 同时让 MyBatis 能够自动扫描到 `mapper` 包里的数据库访问接口

---

## 十二、创建公共返回对象 `common/Result.java`

```java
package com.example.aienterprise.common;

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

这段代码的作用：

- 统一接口返回格式
- 这样前端拿到数据时就会更稳定，不会每个接口风格都不一样

---

## 十三、创建 Redis Key 常量类 `common/RedisKeyConstants.java`

```java
package com.example.aienterprise.common;

public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    public static final String ORDER_DETAIL_KEY = "ai:order:detail:";
    public static final String LOGISTICS_KEY = "ai:logistics:";
    public static final String CHAT_MEMORY_KEY = "ai:chat:memory:";
    public static final String AFTER_SALE_LOCK_KEY = "ai:ticket:lock:";
}
```

这段代码的作用：

- 统一管理 Redis Key 前缀
- 避免你后面在各个类里手写字符串，导致命名混乱

---

## 十四、创建 OpenAI 配置类 `config/OpenAiProperties.java`

```java
package com.example.aienterprise.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

    private String baseUrl;
    private String apiKey;
    private String model;
}
```

这段代码的作用：

- 读取 `application.yml` 里的 OpenAI 配置
- 避免在业务代码里写死模型名、地址和密钥读取逻辑

---

## 十五、创建 DTO

### 15.1 创建 `dto/AssistantChatRequest.java`

```java
package com.example.aienterprise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssistantChatRequest {

    // 企业项目里一般都会带当前登录用户 ID。
    @NotNull(message = "userId 不能为空")
    private Long userId;

    // 用户自然语言输入。
    @NotBlank(message = "message 不能为空")
    private String message;
}
```

这段代码的作用：

- 请求体里除了消息本身，还必须带用户 ID
- 这样工具执行时才能限制“只能查自己的订单”，避免越权

### 15.2 创建 `dto/AssistantChatResponse.java`

```java
package com.example.aienterprise.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssistantChatResponse {

    private String answer;
    private List<String> calledTools;
    private String responseId;
}
```

这段代码的作用：

- 返回最终给用户展示的自然语言答案
- 同时把本轮到底调用了哪些工具也返回出去，便于排查和学习

---

## 十六、创建实体类

### 16.1 创建 `entity/OrderInfo.java`

```java
package com.example.aienterprise.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderInfo {

    private Long id;
    private String orderNo;
    private Long userId;
    private String status;
    private BigDecimal amount;
    private String goodsName;
    private LocalDateTime createTime;
}
```

这段代码的作用：

- 对应订单表 `t_order_info`
- 保存订单详情工具查询出来的核心字段

### 16.2 创建 `entity/LogisticsInfo.java`

```java
package com.example.aienterprise.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogisticsInfo {

    private Long id;
    private String orderNo;
    private String company;
    private String status;
    private String latestTrace;
    private String estimatedArrival;
    private LocalDateTime createTime;
}
```

这段代码的作用：

- 对应物流表 `t_logistics_info`
- 保存物流工具要返回给用户的关键信息

### 16.3 创建 `entity/AfterSaleTicket.java`

```java
package com.example.aienterprise.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AfterSaleTicket {

    private Long id;
    private String ticketNo;
    private String orderNo;
    private Long userId;
    private String reason;
    private String contactPhone;
    private String status;
    private LocalDateTime createTime;
}
```

这段代码的作用：

- 对应售后工单表 `t_after_sale_ticket`
- 用来保存模型触发创建工单后的真实业务数据

### 16.4 创建 `entity/ToolCallLog.java`

```java
package com.example.aienterprise.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ToolCallLog {

    private Long id;
    private Long userId;
    private String toolName;
    private String toolArgs;
    private String toolResult;
    private Integer successFlag;
    private LocalDateTime createTime;
}
```

这段代码的作用：

- 对应工具调用日志表 `t_tool_call_log`
- 企业项目里，工具调用日志非常重要，因为这关系到审计、排错和问题复盘

---

## 十七、创建 Mapper 接口

### 17.1 创建 `mapper/OrderInfoMapper.java`

```java
package com.example.aienterprise.mapper;

import com.example.aienterprise.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderInfoMapper {

    @Select("""
            select id, order_no, user_id, status, amount, goods_name, create_time
            from t_order_info
            where order_no = #{orderNo} and user_id = #{userId}
            limit 1
            """)
    OrderInfo findByOrderNoAndUserId(@Param("orderNo") String orderNo, @Param("userId") Long userId);
}
```

这段代码的作用：

- 根据订单号和用户 ID 查询订单
- 这里特意把 `user_id` 带进条件，是为了防止用户查到别人的订单

### 17.2 创建 `mapper/LogisticsInfoMapper.java`

```java
package com.example.aienterprise.mapper;

import com.example.aienterprise.entity.LogisticsInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LogisticsInfoMapper {

    @Select("""
            select id, order_no, company, status, latest_trace, estimated_arrival, create_time
            from t_logistics_info
            where order_no = #{orderNo}
            order by id desc
            limit 1
            """)
    LogisticsInfo findLatestByOrderNo(@Param("orderNo") String orderNo);
}
```

这段代码的作用：

- 查询指定订单的最新物流信息
- 物流表这里按 `id desc` 取最后一条，模拟“最新物流轨迹”的常见写法

### 17.3 创建 `mapper/AfterSaleTicketMapper.java`

```java
package com.example.aienterprise.mapper;

import com.example.aienterprise.entity.AfterSaleTicket;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AfterSaleTicketMapper {

    @Insert("""
            insert into t_after_sale_ticket(ticket_no, order_no, user_id, reason, contact_phone, status)
            values(#{ticketNo}, #{orderNo}, #{userId}, #{reason}, #{contactPhone}, #{status})
            """)
    int insert(AfterSaleTicket ticket);

    @Select("""
            select count(1)
            from t_after_sale_ticket
            where order_no = #{orderNo}
              and user_id = #{userId}
              and status in ('CREATED', 'PROCESSING')
            """)
    int countOpenTicket(@Param("orderNo") String orderNo, @Param("userId") Long userId);
}
```

这段代码的作用：

- 一个方法负责写入售后工单
- 一个方法负责判断用户对这个订单是否已经有未关闭工单，避免重复创建

### 17.4 创建 `mapper/ToolCallLogMapper.java`

```java
package com.example.aienterprise.mapper;

import com.example.aienterprise.entity.ToolCallLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ToolCallLogMapper {

    @Insert("""
            insert into t_tool_call_log(user_id, tool_name, tool_args, tool_result, success_flag)
            values(#{userId}, #{toolName}, #{toolArgs}, #{toolResult}, #{successFlag})
            """)
    int insert(ToolCallLog log);
}
```

这段代码的作用：

- 把每次工具调用都记录下来
- 后面无论你要排错还是做审计，这张表都会很有价值

---

## 十八、创建工具 Schema 工厂 `util/ToolSchemaFactory.java`

```java
package com.example.aienterprise.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ToolSchemaFactory {

    private final ObjectMapper objectMapper;

    public List<JsonNode> buildTools() {
        return List.of(
                buildQueryOrderTool(),
                buildQueryLogisticsTool(),
                buildCreateAfterSaleTool()
        );
    }

    private JsonNode buildQueryOrderTool() {
        return objectMapper.createObjectNode()
                .put("type", "function")
                .put("name", "query_order_detail")
                .put("description", "查询当前用户自己的订单详情，例如订单状态、金额、商品名称。")
                .put("strict", true)
                .set("parameters", objectMapper.createObjectNode()
                        .put("type", "object")
                        .set("properties", objectMapper.createObjectNode()
                                .set("order_no", objectMapper.createObjectNode()
                                        .put("type", "string")
                                        .put("description", "订单号，例如 ORD-1001")))
                        .put("additionalProperties", false)
                        .set("required", objectMapper.createArrayNode().add("order_no")));
    }

    private JsonNode buildQueryLogisticsTool() {
        return objectMapper.createObjectNode()
                .put("type", "function")
                .put("name", "query_logistics_status")
                .put("description", "查询当前用户指定订单的物流状态、物流公司、最新轨迹。")
                .put("strict", true)
                .set("parameters", objectMapper.createObjectNode()
                        .put("type", "object")
                        .set("properties", objectMapper.createObjectNode()
                                .set("order_no", objectMapper.createObjectNode()
                                        .put("type", "string")
                                        .put("description", "订单号，例如 ORD-1001")))
                        .put("additionalProperties", false)
                        .set("required", objectMapper.createArrayNode().add("order_no")));
    }

    private JsonNode buildCreateAfterSaleTool() {
        return objectMapper.createObjectNode()
                .put("type", "function")
                .put("name", "create_after_sale_ticket")
                .put("description", "为当前用户的指定订单创建售后工单。")
                .put("strict", true)
                .set("parameters", objectMapper.createObjectNode()
                        .put("type", "object")
                        .set("properties", objectMapper.createObjectNode()
                                .set("order_no", objectMapper.createObjectNode()
                                        .put("type", "string")
                                        .put("description", "订单号，例如 ORD-1001"))
                                .set("reason", objectMapper.createObjectNode()
                                        .put("type", "string")
                                        .put("description", "售后原因，例如耳机有杂音"))
                                .set("contact_phone", objectMapper.createObjectNode()
                                        .put("type", "string")
                                        .put("description", "联系电话，例如 13800000000")))
                        .put("additionalProperties", false)
                        .set("required", objectMapper.createArrayNode()
                                .add("order_no")
                                .add("reason")
                                .add("contact_phone")));
    }
}
```

这段代码的作用：

- 告诉模型：当前系统到底提供哪些工具、每个工具需要什么参数
- 这里我把工具参数设计得比较严格，是为了减少模型传错参数的概率

---

## 十九、创建对话上下文服务 `service/ConversationMemoryService.java`

```java
package com.example.aienterprise.service;

import com.example.aienterprise.common.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationMemoryService {

    private final StringRedisTemplate stringRedisTemplate;

    public void saveMessage(Long userId, String role, String content) {
        String key = RedisKeyConstants.CHAT_MEMORY_KEY + userId;

        // 这里为了简化文档，直接把消息存成 "角色:内容" 的字符串。
        String value = role + ":" + content;

        stringRedisTemplate.opsForList().rightPush(key, value);
        // 只保留最近 10 条消息，避免上下文无限膨胀。
        stringRedisTemplate.opsForList().trim(key, -10, -1);
        // 设置过期时间，防止长期无人访问的聊天上下文一直占内存。
        stringRedisTemplate.expire(key, Duration.ofHours(12));
    }

    public String buildRecentContext(Long userId) {
        String key = RedisKeyConstants.CHAT_MEMORY_KEY + userId;

        List<String> recentMessages = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (recentMessages == null || recentMessages.isEmpty()) {
            return "";
        }

        // 把最近几轮对话拼成文本，作为模型输入上下文的一部分。
        return recentMessages.stream().collect(Collectors.joining("\n"));
    }
}
```

这段代码的作用：

- 企业项目里，AI 助手一般不会只看当前一句话
- 这里通过 Redis 保存最近对话，让模型在下一轮请求时能带上上下文

---

## 二十、创建订单工具服务 `service/OrderToolService.java`

```java
package com.example.aienterprise.service;

import com.example.aienterprise.common.RedisKeyConstants;
import com.example.aienterprise.entity.OrderInfo;
import com.example.aienterprise.mapper.OrderInfoMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderToolService {

    private final OrderInfoMapper orderInfoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, Object> queryOrderDetail(Long userId, String orderNo) {
        String cacheKey = RedisKeyConstants.ORDER_DETAIL_KEY + orderNo;

        // 先查 Redis，热点订单优先走缓存。
        String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedJson != null && !cachedJson.isBlank()) {
            try {
                return objectMapper.readValue(cachedJson, Map.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("解析订单缓存失败", e);
            }
        }

        // 缓存没有，再查 MySQL。
        OrderInfo orderInfo = orderInfoMapper.findByOrderNoAndUserId(orderNo, userId);
        if (orderInfo == null) {
            throw new IllegalArgumentException("订单不存在，或者该订单不属于当前用户");
        }

        Map<String, Object> result = Map.of(
                "order_no", orderInfo.getOrderNo(),
                "status", orderInfo.getStatus(),
                "amount", orderInfo.getAmount(),
                "goods_name", orderInfo.getGoodsName(),
                "user_id", orderInfo.getUserId()
        );

        try {
            // 回写 Redis，后面同一个订单的高频查询会更快。
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(result),
                    Duration.ofMinutes(10)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("写入订单缓存失败", e);
        }

        return result;
    }
}
```

这段代码的作用：

- 演示了企业开发里最常见的缓存思路：先查 Redis，再查 MySQL，再回写 Redis
- 同时这里用 `userId + orderNo` 做数据库限制，防止用户越权查看别人的订单

---

## 二十一、创建物流工具服务 `service/LogisticsToolService.java`

```java
package com.example.aienterprise.service;

import com.example.aienterprise.common.RedisKeyConstants;
import com.example.aienterprise.entity.LogisticsInfo;
import com.example.aienterprise.mapper.LogisticsInfoMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LogisticsToolService {

    private final LogisticsInfoMapper logisticsInfoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, Object> queryLogisticsStatus(String orderNo) {
        String cacheKey = RedisKeyConstants.LOGISTICS_KEY + orderNo;

        String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedJson != null && !cachedJson.isBlank()) {
            try {
                return objectMapper.readValue(cachedJson, Map.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("解析物流缓存失败", e);
            }
        }

        LogisticsInfo logisticsInfo = logisticsInfoMapper.findLatestByOrderNo(orderNo);
        if (logisticsInfo == null) {
            throw new IllegalArgumentException("物流信息不存在");
        }

        Map<String, Object> result = Map.of(
                "order_no", logisticsInfo.getOrderNo(),
                "company", logisticsInfo.getCompany(),
                "status", logisticsInfo.getStatus(),
                "latest_trace", logisticsInfo.getLatestTrace(),
                "estimated_arrival", logisticsInfo.getEstimatedArrival()
        );

        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(result),
                    Duration.ofMinutes(5)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("写入物流缓存失败", e);
        }

        return result;
    }
}
```

这段代码的作用：

- 物流属于典型的“查得多、改得少”数据，所以很适合做 Redis 缓存
- 这里 TTL 设得比订单更短，是因为物流变化通常比订单状态更频繁

---

## 二十二、创建售后工单工具服务 `service/TicketToolService.java`

```java
package com.example.aienterprise.service;

import com.example.aienterprise.common.RedisKeyConstants;
import com.example.aienterprise.entity.AfterSaleTicket;
import com.example.aienterprise.entity.OrderInfo;
import com.example.aienterprise.mapper.AfterSaleTicketMapper;
import com.example.aienterprise.mapper.OrderInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketToolService {

    private final AfterSaleTicketMapper afterSaleTicketMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public Map<String, Object> createAfterSaleTicket(Long userId, String orderNo, String reason, String contactPhone) {
        String lockKey = RedisKeyConstants.AFTER_SALE_LOCK_KEY + userId + ":" + orderNo;
        Boolean lockSuccess = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
        if (Boolean.FALSE.equals(lockSuccess)) {
            throw new IllegalStateException("请不要重复提交售后申请");
        }

        try {
            OrderInfo orderInfo = orderInfoMapper.findByOrderNoAndUserId(orderNo, userId);
            if (orderInfo == null) {
                throw new IllegalArgumentException("订单不存在，或者该订单不属于当前用户");
            }

            int openTicketCount = afterSaleTicketMapper.countOpenTicket(orderNo, userId);
            if (openTicketCount > 0) {
                throw new IllegalStateException("当前订单已经存在未关闭的售后工单");
            }

            AfterSaleTicket ticket = new AfterSaleTicket();
            ticket.setTicketNo("TICKET-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
            ticket.setOrderNo(orderNo);
            ticket.setUserId(userId);
            ticket.setReason(reason);
            ticket.setContactPhone(contactPhone);
            ticket.setStatus("CREATED");

            afterSaleTicketMapper.insert(ticket);

            return Map.of(
                    "ticket_no", ticket.getTicketNo(),
                    "order_no", ticket.getOrderNo(),
                    "status", ticket.getStatus(),
                    "reason", ticket.getReason(),
                    "contact_phone", ticket.getContactPhone()
            );
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }
}
```

这段代码的作用：

- 演示了一个很典型的企业动作：工具不只是查询，也可以落业务数据
- 这里同时用了 MySQL 校验、Redis 防重复提交、工单入库

---

## 二十三、创建工具分发器 `service/ToolDispatcherService.java`

```java
package com.example.aienterprise.service;

import com.example.aienterprise.entity.ToolCallLog;
import com.example.aienterprise.mapper.ToolCallLogMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ToolDispatcherService {

    private final OrderToolService orderToolService;
    private final LogisticsToolService logisticsToolService;
    private final TicketToolService ticketToolService;
    private final ToolCallLogMapper toolCallLogMapper;
    private final ObjectMapper objectMapper;

    public String dispatch(Long userId, String toolName, String argumentsJson) {
        ToolCallLog log = new ToolCallLog();
        log.setUserId(userId);
        log.setToolName(toolName);
        log.setToolArgs(argumentsJson);

        try {
            JsonNode argsNode = objectMapper.readTree(argumentsJson);

            String result = switch (toolName) {
                case "query_order_detail" -> objectMapper.writeValueAsString(
                        orderToolService.queryOrderDetail(userId, argsNode.get("order_no").asText())
                );
                case "query_logistics_status" -> objectMapper.writeValueAsString(
                        logisticsToolService.queryLogisticsStatus(argsNode.get("order_no").asText())
                );
                case "create_after_sale_ticket" -> objectMapper.writeValueAsString(
                        ticketToolService.createAfterSaleTicket(
                                userId,
                                argsNode.get("order_no").asText(),
                                argsNode.get("reason").asText(),
                                argsNode.get("contact_phone").asText()
                        )
                );
                default -> throw new IllegalArgumentException("未知工具：" + toolName);
            };

            log.setToolResult(result);
            log.setSuccessFlag(1);
            toolCallLogMapper.insert(log);
            return result;
        } catch (Exception e) {
            log.setToolResult(e.getMessage());
            log.setSuccessFlag(0);
            toolCallLogMapper.insert(log);
            throw new RuntimeException("执行工具失败：" + toolName, e);
        }
    }
}
```

这段代码的作用：

- 这是整个系统的工具执行中枢
- 它不仅负责把模型请求路由到本地业务方法，还会把每次调用结果记录到 MySQL

---

## 二十四、创建 OpenAI 调用服务 `service/OpenAiResponsesService.java`

```java
package com.example.aienterprise.service;

import com.example.aienterprise.config.OpenAiProperties;
import com.example.aienterprise.util.ToolSchemaFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenAiResponsesService {

    private final OpenAiProperties openAiProperties;
    private final ToolSchemaFactory toolSchemaFactory;
    private final ObjectMapper objectMapper;

    public JsonNode createFirstResponse(String composedInput) {
        JsonNode requestBody = objectMapper.createObjectNode()
                .put("model", openAiProperties.getModel())
                .put("instructions", "你是企业电商客服助手。凡是订单、物流、售后问题，都必须先调用工具拿到真实业务数据，再给用户自然语言答复。不要凭空编造订单结果。")
                .put("input", composedInput)
                .set("tools", objectMapper.valueToTree(toolSchemaFactory.buildTools()));

        return buildClient().post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode createSecondResponse(String previousResponseId, List<JsonNode> toolOutputs) {
        JsonNode requestBody = objectMapper.createObjectNode()
                .put("model", openAiProperties.getModel())
                .put("previous_response_id", previousResponseId)
                .set("input", objectMapper.valueToTree(toolOutputs));

        return buildClient().post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);
    }

    private RestClient buildClient() {
        return RestClient.builder()
                .baseUrl(openAiProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + openAiProperties.getApiKey())
                .build();
    }
}
```

这段代码的作用：

- 第一轮请求负责“让模型决定要不要调工具”
- 第二轮请求负责“把工具结果回传给模型，拿到最终自然语言答复”

---

## 二十五、创建 AI 助手主服务 `service/AiAssistantService.java`

```java
package com.example.aienterprise.service;

import com.example.aienterprise.dto.AssistantChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final OpenAiResponsesService openAiResponsesService;
    private final ToolDispatcherService toolDispatcherService;
    private final ConversationMemoryService conversationMemoryService;
    private final ObjectMapper objectMapper;

    public AssistantChatResponse chat(Long userId, String userMessage) {
        String recentContext = conversationMemoryService.buildRecentContext(userId);

        String composedInput = """
                【最近对话上下文】
                %s

                【当前用户问题】
                %s
                """.formatted(recentContext, userMessage);

        JsonNode firstResponse = openAiResponsesService.createFirstResponse(composedInput);

        List<String> calledTools = new ArrayList<>();
        List<JsonNode> toolOutputs = buildToolOutputs(userId, firstResponse, calledTools);

        String finalAnswer;
        String responseId;

        if (toolOutputs.isEmpty()) {
            finalAnswer = extractTextAnswer(firstResponse);
            responseId = firstResponse.path("id").asText();
        } else {
            JsonNode secondResponse = openAiResponsesService.createSecondResponse(
                    firstResponse.path("id").asText(),
                    toolOutputs
            );
            finalAnswer = extractTextAnswer(secondResponse);
            responseId = secondResponse.path("id").asText();
        }

        conversationMemoryService.saveMessage(userId, "user", userMessage);
        conversationMemoryService.saveMessage(userId, "assistant", finalAnswer);

        return new AssistantChatResponse(finalAnswer, calledTools, responseId);
    }

    private List<JsonNode> buildToolOutputs(Long userId, JsonNode responseNode, List<String> calledTools) {
        List<JsonNode> toolOutputs = new ArrayList<>();

        for (JsonNode item : responseNode.path("output")) {
            if (!"function_call".equals(item.path("type").asText())) {
                continue;
            }

            String toolName = item.path("name").asText();
            String argumentsJson = item.path("arguments").asText("{}");
            String callId = item.path("call_id").asText();

            String toolResult = toolDispatcherService.dispatch(userId, toolName, argumentsJson);

            calledTools.add(toolName);

            ObjectNode toolOutput = objectMapper.createObjectNode();
            toolOutput.put("type", "function_call_output");
            toolOutput.put("call_id", callId);
            toolOutput.put("output", toolResult);
            toolOutputs.add(toolOutput);
        }

        return toolOutputs;
    }

    private String extractTextAnswer(JsonNode responseNode) {
        String outputText = responseNode.path("output_text").asText();
        if (outputText != null && !outputText.isBlank()) {
            return outputText;
        }

        StringBuilder answerBuilder = new StringBuilder();
        for (JsonNode outputItem : responseNode.path("output")) {
            if (!"message".equals(outputItem.path("type").asText())) {
                continue;
            }
            for (JsonNode contentItem : outputItem.path("content")) {
                if (!"output_text".equals(contentItem.path("type").asText())) {
                    continue;
                }
                answerBuilder.append(contentItem.path("text").asText());
            }
        }

        if (answerBuilder.isEmpty()) {
            return "模型本次没有返回可直接展示的文本，请检查工具输出和响应结构。";
        }
        return answerBuilder.toString();
    }
}
```

这段代码的作用：

- 这是整个企业版项目的主闭环
- 它把 Redis 上下文、模型调用、工具执行、工具结果回传、最终答复生成全部串起来了

---

## 二十六、创建控制器 `controller/AiAssistantController.java`

```java
package com.example.aienterprise.controller;

import com.example.aienterprise.common.Result;
import com.example.aienterprise.dto.AssistantChatRequest;
import com.example.aienterprise.dto.AssistantChatResponse;
import com.example.aienterprise.service.AiAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/assistant")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/chat")
    public Result<AssistantChatResponse> chat(@Valid @RequestBody AssistantChatRequest request) {
        AssistantChatResponse response = aiAssistantService.chat(request.getUserId(), request.getMessage());
        return Result.success(response);
    }
}
```

这段代码的作用：

- 对外暴露统一聊天接口
- 前端只要传 `userId + message`，后端就会自动完成 function calling 全闭环

---

## 二十七、到这里你已经真正实现了什么

到这里，这个项目已经不是“AI 玩具 Demo”了，而是一个具备企业味道的 AI 后端骨架：

1. 订单和物流查的是 MySQL 真实数据
2. Redis 承担了缓存和上下文记忆
3. 售后工单能真正落库
4. 每次工具调用都会记录日志
5. 用户只能查自己的订单，避免越权
6. 模型通过标准 function calling 和后端工具协作

---

## 二十八、启动项目

```powershell
mvn spring-boot:run
```

这段代码的作用：

- 启动 Spring Boot 服务
- 默认监听 `8080` 端口

---

## 二十九、开始测试

### 29.1 普通问题，不触发工具

```powershell
curl.exe -X POST "http://localhost:8080/ai/assistant/chat" ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\":10001,\"message\":\"你好，你能做什么？\"}"
```

这段代码的作用：

- 测试一个普通闲聊问题
- 这类问题一般不需要查订单，不需要查物流，也不需要建工单

### 29.2 查询订单详情

```powershell
curl.exe -X POST "http://localhost:8080/ai/assistant/chat" ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\":10001,\"message\":\"帮我查一下订单 ORD-1001 的详情\"}"
```

这段代码的作用：

- 测试模型是否会触发 `query_order_detail`
- 正确情况下，工具执行层会先查 Redis，没有命中再查 MySQL

预期结果：

- `calledTools` 包含 `query_order_detail`
- `answer` 里会包含订单状态、金额、商品名称

### 29.3 查询物流

```powershell
curl.exe -X POST "http://localhost:8080/ai/assistant/chat" ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\":10001,\"message\":\"ORD-1001 现在物流到哪里了？\"}"
```

这段代码的作用：

- 测试模型是否会调用 `query_logistics_status`
- 同时也可以观察 Redis 物流缓存是否生效

### 29.4 创建售后工单

```powershell
curl.exe -X POST "http://localhost:8080/ai/assistant/chat" ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\":10001,\"message\":\"订单 ORD-1001 的耳机有杂音，帮我创建售后工单，我的手机号是 13800000000\"}"
```

这段代码的作用：

- 测试模型是否会提取订单号、售后原因、联系电话
- 然后调用 `create_after_sale_ticket`

预期结果：

- `calledTools` 包含 `create_after_sale_ticket`
- MySQL 表 `t_after_sale_ticket` 会新增一条工单数据

### 29.5 测试越权保护

```powershell
curl.exe -X POST "http://localhost:8080/ai/assistant/chat" ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\":10001,\"message\":\"帮我查一下订单 ORD-2001 的详情\"}"
```

这段代码的作用：

- 这里 `ORD-2001` 实际属于 `userId=20001`
- 当前请求用户是 `10001`
- 也就是说，这个测试是故意模拟“用户查别人的订单”

预期结果：

- 工具执行层报错：订单不存在，或者该订单不属于当前用户
- 这正是企业项目必须具备的数据隔离能力

---

## 三十、你应该如何验证 Redis 真的参与了系统

你可以直接进入 Redis 查看这些 key：

```powershell
docker exec -it ai-enterprise-redis redis-cli
```

这段代码的作用：

- 进入 Redis 容器命令行

然后执行：

```text
keys ai:*
```

这段代码的作用：

- 查看这个项目写入 Redis 的 key

你通常会看到：

- `ai:order:detail:ORD-1001`
- `ai:logistics:ORD-1001`
- `ai:chat:memory:10001`

这就说明：

- 订单缓存生效了
- 物流缓存生效了
- 对话上下文也被保存了

---

## 三十一、你应该如何验证 MySQL 真的参与了系统

进入 MySQL：

```powershell
docker exec -it ai-enterprise-mysql mysql -uroot -proot1234 ai_enterprise_demo
```

这段代码的作用：

- 进入 MySQL 容器并连接到业务数据库

查看工单：

```sql
select * from t_after_sale_ticket;
```

这段代码的作用：

- 查看模型触发创建售后工单后，业务数据是否真的入库

查看工具日志：

```sql
select * from t_tool_call_log order by id desc;
```

这段代码的作用：

- 查看最近的工具调用记录
- 你能知道模型调了哪个工具、参数是什么、结果是否成功

---

## 三十二、这个企业版项目最值得你吸收的 6 个设计点

### 32.1 模型不直接访问数据库

这意味着：

- 安全边界清晰
- 权限逻辑仍然掌握在后端手里
- 模型只是“决定调什么”，不是“直接操作系统”

### 32.2 用户上下文不交给模型自己猜

这份文档里，`userId` 是接口传入的。  
然后工具执行层会用 `userId` 限制 SQL 查询范围。

这意味着：

- 就算模型理解错了订单归属
- 真正查库时也不会越权

### 32.3 Redis 不只是缓存，还承担会话记忆

很多人一提 Redis 只想到缓存。  
但在 AI 项目里，Redis 很适合保存：

- 最近对话
- 工具结果缓存
- 临时去重 key

### 32.4 工具调用一定要有审计日志

如果生产环境出了问题，你至少要能回答这些问题：

- 模型到底调用了什么工具
- 当时传了什么参数
- 工具执行结果是什么
- 失败原因是什么

### 32.5 工具参数设计要尽量简单、明确

例如：

- `order_no`
- `reason`
- `contact_phone`

这比把一个大对象直接丢给模型更稳定。

### 32.6 要接受“模型负责决策，后端负责兜底”

这是 AI 工程最核心的心态之一：

- 模型可以做智能判断
- 但安全、权限、真实数据、幂等、审计，仍然必须由后端负责

---

## 三十三、常见报错排查

### 33.1 API Key 为空

检查：

```powershell
echo $env:OPENAI_API_KEY
```

这段代码的作用：

- 检查当前终端里是否真的设置了 OpenAI API Key

### 33.2 MySQL 启动了但查不到表

原因通常有两个：

1. `init.sql` 没有正确挂载到容器
2. 容器不是第一次启动，初始化脚本不会再次自动执行

解决办法：

- `docker compose down -v`
- 再重新 `docker compose up -d`

### 33.3 Redis 里没有上下文 key

原因可能有：

1. 你请求根本没走到保存消息逻辑
2. Redis 配置错了
3. 你用了错误的 `userId`

建议先执行：

```text
keys ai:chat:memory:*
```

这段代码的作用：

- 检查聊天上下文 key 是否存在

### 33.4 模型没有调用工具，直接乱答

常见原因：

1. 工具描述不够明确
2. 系统指令没强调“订单、物流、售后必须先调工具”
3. 用户问题太模糊

改进方向：

- 优化 `description`
- 优化 `parameters`
- 优化 `instructions`

### 33.5 第二轮请求后没有最终文本

最常见的问题：

1. 忘了传 `previous_response_id`
2. 忘了传 `call_id`
3. `function_call_output` 的 `output` 格式不对

这里必须记住：

- `function_call` 和 `function_call_output` 是一一对应的
- 关联靠的是 `call_id`

### 33.6 售后工单重复创建

如果你发现重复创建，一般有这些可能：

1. Redis 防重复 key 没生效
2. 你把 finally 里的 `delete` 去掉了又没做更强幂等
3. 数据库层没有做更严格的唯一约束

企业里通常还会继续加：

- 业务幂等号
- 分布式锁
- 唯一索引

---

## 三十四、这份企业版文档学完后，你会真正多会什么

相对基础版，你多学到的不是“会多写几个类”，而是：

1. 会把 function calling 接到真实业务数据库
2. 会用 Redis 给 AI 场景做缓存和上下文记忆
3. 会做用户数据隔离，不让用户越权
4. 会给工具调用做审计日志
5. 会让 AI 功能更像真实系统，而不是玩具 Demo

---

## 三十五、下一步怎么继续升级

如果你把这份文档跑通了，下一步我建议你继续升级这些能力：

1. 给订单和物流缓存加失效策略和主动失效机制
2. 给工具调用加超时控制和熔断
3. 给售后工单工具加事务和真正的业务状态流转
4. 引入登录态，让 `userId` 从登录上下文里获取，而不是前端直接传
5. 接流式输出，把最终答案改成流式返回
6. 引入更多工具，例如查询退款、优惠券、收货地址

---

## 三十六、官方参考资料

- [OpenAI Models](https://developers.openai.com/api/docs/models)
- [GPT-5 mini Model](https://developers.openai.com/api/docs/models/gpt-5-mini)
- [Migrate to the Responses API](https://developers.openai.com/api/docs/guides/migrate-to-responses)
- [Function calling](https://developers.openai.com/api/docs/guides/function-calling)
- [Create a model response](https://developers.openai.com/api/reference/resources/responses/methods/create)
- [Spring Framework RestClient](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/reference/)
- [MyBatis Spring Boot Starter](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
- [MySQL Official Image](https://hub.docker.com/_/mysql)
- [Redis Official Image](https://hub.docker.com/_/redis)

---

## 三十七、最后总结

如果说基础版文档教会你的是：

- 模型怎么调工具

那么这份企业版文档教会你的就是：

- 工具怎么真正接业务
- Redis 在 AI 项目里怎么用
- MySQL 在 AI 项目里怎么承接真实数据
- 为什么 AI 项目里仍然要强调权限、缓存、日志和数据边界

这才是你以后在真实公司项目里更容易遇到的 Function Calling 形态。
