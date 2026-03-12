# es-log-search-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot + Elasticsearch 项目，完成“日志检索”练习模块，让初学者掌握 ES 在日志搜索场景下的关键字检索、精确过滤、时间范围查询和倒序分页。

**Architecture:** 项目主链路是 `Controller -> Service -> ElasticsearchRepository / ElasticsearchOperations -> Elasticsearch`。日志写入使用 `Repository`，日志搜索使用 `ElasticsearchOperations` 执行组合查询。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Web, Spring Validation, Spring Data Elasticsearch, Elasticsearch 8.10.4, Docker Compose, Maven, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 Elasticsearch

日志检索是 Elasticsearch 最典型的生产落地场景之一。

排查一个线上问题时，工程师通常不会只做“全文搜索”这么简单，而是会组合这些条件：

- 按 `message` 关键字搜索
- 按服务名过滤
- 按日志级别过滤
- 按 `traceId` 精确定位
- 按时间范围过滤
- 默认按时间倒序查看最新日志

这种查询模式和数据库天然不太匹配，但和 Elasticsearch 非常匹配。

---

## 二、最终目录结构

```text
es-log-search-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/eslogsearch
│   │   │   ├── EsLogSearchApplication.java
│   │   │   ├── common
│   │   │   │   └── Result.java
│   │   │   ├── controller
│   │   │   │   └── LogController.java
│   │   │   ├── document
│   │   │   │   └── LogDocument.java
│   │   │   ├── dto
│   │   │   │   ├── LogIngestRequest.java
│   │   │   │   ├── LogSearchRequest.java
│   │   │   │   ├── LogSearchResponse.java
│   │   │   │   └── LogView.java
│   │   │   ├── repository
│   │   │   │   └── LogRepository.java
│   │   │   └── service
│   │   │       ├── LogService.java
│   │   │       └── impl
│   │   │           └── LogServiceImpl.java
│   │   └── resources
│   │       └── application.yml
│   └── test
│       └── java/com/example/eslogsearch
│           └── EsLogSearchApplicationTests.java
```

这段目录结构的作用：

- 让你一眼看出日志项目重点围绕 `LogDocument`、`LogSearchRequest` 和 `LogServiceImpl`
- 和前两个 ES 练习项目保持相同结构，方便你横向比较

---

## 三、日志场景里最关键的是字段设计

日志搜索里，不同字段的职责非常清楚：

- `message`：全文字段
- `serviceName`：精确过滤字段
- `level`：精确过滤字段
- `traceId`：精确过滤字段
- `createTime`：时间范围过滤和排序字段

这段说明的作用：

- 让你理解为什么日志搜索不是“所有字段都用全文检索”
- 正确的字段建模是日志场景能否好用的核心

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/eslogsearch/EsLogSearchApplication.java`
- Create: `src/main/resources/application.yml`
- Test: `src/test/java/com/example/eslogsearch/EsLogSearchApplicationTests.java`

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
    <artifactId>es-log-search-demo</artifactId>
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
            <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
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

- 继续沿用同一套依赖组合
- 让你把注意力集中在“日志查询 DSL”和“字段职责”上

**Step 2: 创建 `docker-compose.yml`**

```yaml
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.10.4
    container_name: es-log-search
    restart: always
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
    ports:
      - "9203:9200"
```

这段代码的作用：

- 启动日志检索项目自己的 ES 容器
- 单节点模式更适合本地学习和重复练习

**Step 3: 创建启动类 `EsLogSearchApplication.java`**

```java
package com.example.eslogsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EsLogSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsLogSearchApplication.class, args);
    }
}
```

这段代码的作用：

- 这是日志检索项目的启动入口
- 所有组件都会从这个启动类开始被 Spring Boot 扫描

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8143

spring:
  application:
    name: es-log-search-demo
  elasticsearch:
    uris: http://localhost:9203

logging:
  level:
    org.springframework.data.elasticsearch.client.elc: info
```

这段代码的作用：

- 把应用固定到 `8143`
- 把 ES 连接地址固定到当前项目自己的 `9203`

**Step 5: 创建测试类 `EsLogSearchApplicationTests.java`**

```java
package com.example.eslogsearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EsLogSearchApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

这段代码的作用：

- 用最小测试确认当前项目的基础配置能启动起来
- 先保证环境没问题，再写业务逻辑更稳

---

### Task 2: 创建公共返回对象、日志文档模型和 DTO

**Files:**
- Create: `src/main/java/com/example/eslogsearch/common/Result.java`
- Create: `src/main/java/com/example/eslogsearch/document/LogDocument.java`
- Create: `src/main/java/com/example/eslogsearch/dto/LogIngestRequest.java`
- Create: `src/main/java/com/example/eslogsearch/dto/LogSearchRequest.java`
- Create: `src/main/java/com/example/eslogsearch/dto/LogSearchResponse.java`
- Create: `src/main/java/com/example/eslogsearch/dto/LogView.java`
- Create: `src/main/java/com/example/eslogsearch/repository/LogRepository.java`

**Step 1: 创建 `Result.java`**

```java
package com.example.eslogsearch.common;

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

这段代码的作用：

- 统一接口返回格式
- 让这 3 个 ES 练习项目看起来像一套统一训练体系

**Step 2: 创建 `LogDocument.java`**

```java
package com.example.eslogsearch.document;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "log_index")
public class LogDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String serviceName;

    @Field(type = FieldType.Keyword)
    private String hostName;

    @Field(type = FieldType.Keyword)
    private String level;

    @Field(type = FieldType.Keyword)
    private String traceId;

    @Field(type = FieldType.Text)
    private String message;

    @Field(type = FieldType.Keyword)
    private String loggerName;

    @Field(type = FieldType.Keyword)
    private String threadName;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private LocalDateTime createTime;
}
```

这段代码的作用：

- `message` 用来做全文检索
- 其他字段基本都是结构化过滤字段
- 这就是日志场景里最典型的 ES 文档建模方式

**Step 3: 创建 `LogIngestRequest.java`**

```java
package com.example.eslogsearch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogIngestRequest {

    @NotBlank(message = "服务名不能为空")
    private String serviceName;

    @NotBlank(message = "主机名不能为空")
    private String hostName;

    @NotBlank(message = "日志级别不能为空")
    private String level;

    @NotBlank(message = "traceId 不能为空")
    private String traceId;

    @NotBlank(message = "日志内容不能为空")
    private String message;

    @NotBlank(message = "loggerName 不能为空")
    private String loggerName;

    @NotBlank(message = "threadName 不能为空")
    private String threadName;
}
```

这段代码的作用：

- 定义写入一条日志时需要提供的字段
- 这些字段也是后面检索时最常见的过滤条件来源

**Step 4: 创建 `LogSearchRequest.java`**

```java
package com.example.eslogsearch.dto;

import lombok.Data;

@Data
public class LogSearchRequest {

    private String keyword;
    private String serviceName;
    private String level;
    private String traceId;
    private String startTime;
    private String endTime;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
```

这段代码的作用：

- 收敛日志搜索里最核心的查询条件
- 时间范围这里用字符串接收，便于你直接传 `2026-03-12T10:00:00` 这种格式

**Step 5: 创建 `LogView.java`**

```java
package com.example.eslogsearch.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogView {

    private String id;
    private String serviceName;
    private String hostName;
    private String level;
    private String traceId;
    private String message;
    private String loggerName;
    private String threadName;
    private LocalDateTime createTime;
}
```

这段代码的作用：

- 这是接口返回给前端的日志数据结构
- 和原始日志文档结构基本一致，便于理解

**Step 6: 创建 `LogSearchResponse.java`**

```java
package com.example.eslogsearch.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogSearchResponse {

    private long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<LogView> items;
}
```

这段代码的作用：

- 统一封装日志分页查询结果
- 日志量通常很大，所以分页结构是必须的

**Step 7: 创建 `LogRepository.java`**

```java
package com.example.eslogsearch.repository;

import com.example.eslogsearch.document.LogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface LogRepository extends ElasticsearchRepository<LogDocument, String> {
}
```

这段代码的作用：

- 提供最基础的日志写入和按 id 查询能力
- 复杂检索继续通过 `ElasticsearchOperations` 完成

---

### Task 3: 创建 Service，实现日志组合查询

**Files:**
- Create: `src/main/java/com/example/eslogsearch/service/LogService.java`
- Create: `src/main/java/com/example/eslogsearch/service/impl/LogServiceImpl.java`

**Step 1: 创建 `LogService.java`**

```java
package com.example.eslogsearch.service;

import com.example.eslogsearch.document.LogDocument;
import com.example.eslogsearch.dto.LogIngestRequest;
import com.example.eslogsearch.dto.LogSearchRequest;
import com.example.eslogsearch.dto.LogSearchResponse;

public interface LogService {

    void resetAndLoadDemoData();

    LogDocument save(LogIngestRequest request);

    LogSearchResponse search(LogSearchRequest request);

    LogDocument getById(String id);
}
```

这段代码的作用：

- 先把日志模块的核心能力抽象出来
- Controller 只依赖接口，不直接接触 ES 查询细节

**Step 2: 创建 `LogServiceImpl.java`**

```java
package com.example.eslogsearch.service.impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.example.eslogsearch.document.LogDocument;
import com.example.eslogsearch.dto.LogIngestRequest;
import com.example.eslogsearch.dto.LogSearchRequest;
import com.example.eslogsearch.dto.LogSearchResponse;
import com.example.eslogsearch.dto.LogView;
import com.example.eslogsearch.repository.LogRepository;
import com.example.eslogsearch.service.LogService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final LogRepository logRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void resetAndLoadDemoData() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(LogDocument.class);
        if (indexOperations.exists()) {
            indexOperations.delete();
        }
        indexOperations.create();
        indexOperations.putMapping(indexOperations.createMapping(LogDocument.class));

        List<LogDocument> logs = List.of(
            LogDocument.builder()
                .id(UUID.randomUUID().toString())
                .serviceName("order-service")
                .hostName("node-01")
                .level("INFO")
                .traceId("trace-1001")
                .message("create order request handled successfully")
                .loggerName("OrderController")
                .threadName("http-nio-8143-exec-1")
                .createTime(LocalDateTime.now().minusMinutes(30))
                .build(),
            LogDocument.builder()
                .id(UUID.randomUUID().toString())
                .serviceName("payment-service")
                .hostName("node-02")
                .level("ERROR")
                .traceId("trace-1002")
                .message("payment callback signature verification failed")
                .loggerName("PaymentCallbackHandler")
                .threadName("http-nio-8143-exec-2")
                .createTime(LocalDateTime.now().minusMinutes(12))
                .build(),
            LogDocument.builder()
                .id(UUID.randomUUID().toString())
                .serviceName("inventory-service")
                .hostName("node-03")
                .level("WARN")
                .traceId("trace-1003")
                .message("stock deduction cost is higher than expected threshold")
                .loggerName("InventoryService")
                .threadName("inventory-worker-1")
                .createTime(LocalDateTime.now().minusMinutes(5))
                .build()
        );

        logRepository.saveAll(logs);
    }

    @Override
    public LogDocument save(LogIngestRequest request) {
        LogDocument document = LogDocument.builder()
            .id(UUID.randomUUID().toString())
            .serviceName(request.getServiceName())
            .hostName(request.getHostName())
            .level(request.getLevel())
            .traceId(request.getTraceId())
            .message(request.getMessage())
            .loggerName(request.getLoggerName())
            .threadName(request.getThreadName())
            .createTime(LocalDateTime.now())
            .build();

        return logRepository.save(document);
    }

    @Override
    public LogSearchResponse search(LogSearchRequest request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        boolean hasCondition = false;

        if (StringUtils.hasText(request.getKeyword())) {
            boolBuilder.must(must -> must.match(match -> match.field("message").query(request.getKeyword())));
            hasCondition = true;
        }

        if (StringUtils.hasText(request.getServiceName())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("serviceName").value(request.getServiceName())));
            hasCondition = true;
        }

        if (StringUtils.hasText(request.getLevel())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("level").value(request.getLevel())));
            hasCondition = true;
        }

        if (StringUtils.hasText(request.getTraceId())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("traceId").value(request.getTraceId())));
            hasCondition = true;
        }

        if (StringUtils.hasText(request.getStartTime())) {
            boolBuilder.filter(filter -> filter.range(range -> range.date(date -> date
                .field("createTime")
                .gte(JsonData.of(request.getStartTime()))
            )));
            hasCondition = true;
        }

        if (StringUtils.hasText(request.getEndTime())) {
            boolBuilder.filter(filter -> filter.range(range -> range.date(date -> date
                .field("createTime")
                .lte(JsonData.of(request.getEndTime()))
            )));
            hasCondition = true;
        }

        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();

        Query query = hasCondition
            ? boolBuilder.build()._toQuery()
            : Query.of(q -> q.matchAll(m -> m));

        NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(query)
            .withPageable(PageRequest.of(pageNum - 1, pageSize))
            .withSort(sort -> sort.field(field -> field.field("createTime").order(SortOrder.Desc)))
            .build();

        SearchHits<LogDocument> hits = elasticsearchOperations.search(nativeQuery, LogDocument.class);
        List<LogView> items = new ArrayList<>();

        for (SearchHit<LogDocument> hit : hits) {
            LogDocument log = hit.getContent();
            items.add(LogView.builder()
                .id(log.getId())
                .serviceName(log.getServiceName())
                .hostName(log.getHostName())
                .level(log.getLevel())
                .traceId(log.getTraceId())
                .message(log.getMessage())
                .loggerName(log.getLoggerName())
                .threadName(log.getThreadName())
                .createTime(log.getCreateTime())
                .build());
        }

        return LogSearchResponse.builder()
            .total(hits.getTotalHits())
            .pageNum(pageNum)
            .pageSize(pageSize)
            .items(items)
            .build();
    }

    @Override
    public LogDocument getById(String id) {
        return logRepository.findById(id).orElse(null);
    }
}
```

这段代码的作用：

- `message` 负责关键字全文检索
- `serviceName`、`level`、`traceId` 负责精确过滤
- `startTime` 和 `endTime` 负责时间范围过滤
- `createTime desc` 是日志检索最常见的默认排序方式

---

### Task 4: 创建 Controller，对外暴露接口

**Files:**
- Create: `src/main/java/com/example/eslogsearch/controller/LogController.java`

**Step 1: 创建 `LogController.java`**

```java
package com.example.eslogsearch.controller;

import com.example.eslogsearch.common.Result;
import com.example.eslogsearch.document.LogDocument;
import com.example.eslogsearch.dto.LogIngestRequest;
import com.example.eslogsearch.dto.LogSearchRequest;
import com.example.eslogsearch.dto.LogSearchResponse;
import com.example.eslogsearch.service.LogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @PostMapping("/init-demo")
    public Result<String> initDemo() {
        logService.resetAndLoadDemoData();
        return Result.success("日志索引已重建并写入演示数据");
    }

    @PostMapping
    public Result<LogDocument> create(@Valid @RequestBody LogIngestRequest request) {
        return Result.success(logService.save(request));
    }

    @PostMapping("/search")
    public Result<LogSearchResponse> search(@RequestBody LogSearchRequest request) {
        return Result.success(logService.search(request));
    }

    @GetMapping("/{id}")
    public Result<LogDocument> getById(@PathVariable String id) {
        return Result.success(logService.getById(id));
    }
}
```

这段代码的作用：

- 暴露初始化日志、写入日志、搜索日志、查看详情 4 个接口
- 这已经足够你模拟一个最小可运行的日志检索服务

---

## 四、启动顺序

**Step 1: 启动 Elasticsearch**

```bash
docker compose up -d
```

这段命令的作用：

- 启动日志项目专属的 ES 容器
- 当前项目只占用 `9203` 端口

**Step 2: 启动 Spring Boot**

```bash
mvn spring-boot:run
```

这段命令的作用：

- 启动日志检索应用
- 启动后应用会监听 `8143` 端口

---

## 五、接口测试顺序

**Step 1: 初始化演示日志**

```bash
curl -X POST http://localhost:8143/logs/init-demo
```

这段命令的作用：

- 重建 `log_index`
- 写入 3 条演示日志

**Step 2: 搜索 ERROR 级别日志**

```bash
curl -X POST http://localhost:8143/logs/search ^
  -H "Content-Type: application/json" ^
  -d "{\"level\":\"ERROR\",\"pageNum\":1,\"pageSize\":10}"
```

这段命令的作用：

- 演示日志级别精确过滤
- 返回结果会按时间倒序排列

**Step 3: 按关键字和服务名组合搜索**

```bash
curl -X POST http://localhost:8143/logs/search ^
  -H "Content-Type: application/json" ^
  -d "{\"keyword\":\"payment\",\"serviceName\":\"payment-service\"}"
```

这段命令的作用：

- 演示“全文搜索 + 精确过滤”的组合检索
- 这就是线上排查问题时最常见的用法

**Step 4: 按时间范围搜索**

```bash
curl -X POST http://localhost:8143/logs/search ^
  -H "Content-Type: application/json" ^
  -d "{\"startTime\":\"2026-03-12T09:00:00\",\"endTime\":\"2026-03-12T23:59:59\"}"
```

这段命令的作用：

- 演示时间范围过滤
- 这对线上问题定位非常重要

---

## 六、常见报错排查

### 1. 时间范围查询没有结果

原因：

- 你传入的时间格式不正确

建议：

- 按 `2026-03-12T09:00:00` 这种 ISO 风格时间传参

### 2. 关键字明明在 message 里却搜不到

原因：

- 你当前使用的是默认分词和英文示例数据
- 如果你传中文关键字，而数据本身是英文内容，自然搜不到

### 3. ES 容器一直启动不起来

排查命令：

```bash
docker compose logs -f elasticsearch
```

这段命令的作用：

- 查看 ES 容器启动日志
- 这是排查 ES 内存、端口占用和初始化问题最直接的方式

---

## 七、你在这个项目里学到了什么

做完这个项目后，你应该已经掌握：

- 日志检索的核心字段怎么建模
- 如何组合关键字、服务名、级别、traceId、时间范围查询
- 为什么日志系统默认按时间倒序分页
- 为什么日志搜索是 Elasticsearch 在生产里最经典的落地方式之一
