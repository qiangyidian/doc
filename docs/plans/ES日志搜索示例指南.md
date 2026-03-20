# 日志检索 Elasticsearch 项目实战文档

## 1. 项目目标

你要新建一个 Spring Boot 项目，项目名叫 `es-log-search-demo`。

这个项目模拟的是生产环境中的日志检索场景。开发人员或运维人员通常会这样查日志：

- 输入关键字搜索日志正文
- 按服务名过滤
- 按日志级别过滤
- 按 traceId 过滤
- 按时间范围过滤
- 按时间倒序查看最新日志

这个项目的重点是：

- 日志正文关键字搜索
- 精确过滤
- 时间范围查询
- 按时间倒序分页

## 2. 最终目录结构

```text
es-log-search-demo
├─ docker
│  └─ docker-compose.yml
├─ src
│  ├─ main
│  │  ├─ java/com/example/logsearch
│  │  │  ├─ config
│  │  │  │  └─ ElasticsearchConfig.java
│  │  │  ├─ controller
│  │  │  │  └─ LogController.java
│  │  │  ├─ document
│  │  │  │  └─ LogDocument.java
│  │  │  ├─ dto
│  │  │  │  ├─ ApiResponse.java
│  │  │  │  ├─ LogSearchRequest.java
│  │  │  │  ├─ LogSearchResponse.java
│  │  │  │  └─ LogView.java
│  │  │  ├─ repository
│  │  │  │  └─ LogRepository.java
│  │  │  ├─ service
│  │  │  │  ├─ LogService.java
│  │  │  │  └─ impl
│  │  │  │     └─ LogServiceImpl.java
│  │  │  └─ EsLogSearchDemoApplication.java
│  │  └─ resources
│  │     └─ application.yml
│  └─ test
│     └─ java/com/example/logsearch
│        └─ LogSearchApplicationTests.java
├─ pom.xml
└─ README.md
```

## 3. 第一步：创建 Spring Boot 工程

推荐参数如下：

- `GroupId`：`com.example`
- `ArtifactId`：`es-log-search-demo`
- `Package name`：`com.example.logsearch`
- `Java`：`17`

依赖选择：

- Spring Web
- Spring Data Elasticsearch
- Validation
- Lombok
- Spring Boot Test

## 4. 第二步：创建 `pom.xml`

创建文件 [pom.xml](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\pom.xml)。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 父工程 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.6</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>es-log-search-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>es-log-search-demo</name>

    <properties>
        <!-- 统一使用 Java 17 -->
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Web 接口依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Elasticsearch 依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
        </dependency>

        <!-- 参数校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- 简化样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖 -->
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

## 5. 第三步：用 Docker 启动中间件

创建文件 [docker/docker-compose.yml](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\docker\docker-compose.yml)。

```yaml
version: "3.8"

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.15.3
    container_name: es-log-search
    environment:
      # 单机模式，适合本地练习
      discovery.type: single-node
      # 关闭安全认证，降低入门门槛
      xpack.security.enabled: "false"
      # 限制内存占用
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"
    ports:
      # 日志项目使用 9202 端口，避免和其他项目冲突
      - "9202:9200"
    volumes:
      - es_log_data:/usr/share/elasticsearch/data

  kibana:
    image: docker.elastic.co/kibana/kibana:8.15.3
    container_name: kibana-log-search
    environment:
      ELASTICSEARCH_HOSTS: '["http://elasticsearch:9200"]'
    depends_on:
      - elasticsearch
    ports:
      - "5603:5601"

volumes:
  es_log_data:
```

启动命令：

```bash
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.yml ps
```

## 6. 第四步：创建 `application.yml`

创建文件 [application.yml](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\src\main\resources\application.yml)。

```yaml
server:
  port: 8082

spring:
  application:
    name: es-log-search-demo
  elasticsearch:
    uris: http://localhost:9202

logging:
  level:
    root: info
    org.springframework.data.elasticsearch.client.WIRE: trace
```

## 7. 第五步：创建启动类

创建文件 [EsLogSearchDemoApplication.java](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\src\main\java\com\example\logsearch\EsLogSearchDemoApplication.java)。

```java
package com.example.logsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 日志检索项目启动类。
 */
@SpringBootApplication
public class EsLogSearchDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsLogSearchDemoApplication.class, args);
    }
}
```

## 8. 第六步：创建索引实体类

创建文件 [LogDocument.java](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\src\main\java\com\example\logsearch\document\LogDocument.java)。

```java
package com.example.logsearch.document;

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

/**
 * LogDocument 表示 Elasticsearch 中的一条日志文档。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "logs")
public class LogDocument {

    @Id
    private String id;

    // 服务名，一般用于精确过滤
    @Field(type = FieldType.Keyword)
    private String serviceName;

    @Field(type = FieldType.Keyword)
    private String hostName;

    // 日志级别通常是固定值，比如 INFO、WARN、ERROR
    @Field(type = FieldType.Keyword)
    private String level;

    // traceId 也是精确值
    @Field(type = FieldType.Keyword)
    private String traceId;

    // message 是日志搜索的核心全文字段
    @Field(type = FieldType.Text)
    private String message;

    @Field(type = FieldType.Keyword)
    private String loggerName;

    @Field(type = FieldType.Keyword)
    private String threadName;

    // 产生时间，是日志检索里最关键的排序字段
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createTime;
}
```

## 9. 第七步：创建 Repository

创建文件 [LogRepository.java](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\src\main\java\com\example\logsearch\repository\LogRepository.java)。

```java
package com.example.logsearch.repository;

import com.example.logsearch.document.LogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 日志索引的基础 Repository。
 */
public interface LogRepository extends ElasticsearchRepository<LogDocument, String> {
}
```

## 10. 第八步：创建 DTO

创建 [ApiResponse.java](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\src\main\java\com\example\logsearch\dto\ApiResponse.java)。

```java
package com.example.logsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回结构。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }
}
```

创建 [LogSearchRequest.java](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\src\main\java\com\example\logsearch\dto\LogSearchRequest.java)。

```java
package com.example.logsearch.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 接收日志搜索条件。
 */
@Data
public class LogSearchRequest {

    // 日志正文关键字
    private String keyword;

    // 服务名过滤
    private String serviceName;

    // 日志级别过滤
    private String level;

    // traceId 过滤
    private String traceId;

    // 开始时间，建议使用 2026-03-11T09:00:00 这种格式
    private String startTime;

    // 结束时间
    private String endTime;

    @Min(0)
    private Integer page = 0;

    @Min(1)
    private Integer size = 10;
}
```

创建 [LogView.java](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\src\main\java\com\example\logsearch\dto\LogView.java)。

```java
package com.example.logsearch.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 返回给调用方的日志对象。
 */
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

创建 [LogSearchResponse.java](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\src\main\java\com\example\logsearch\dto\LogSearchResponse.java)。

```java
package com.example.logsearch.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 日志搜索分页返回对象。
 */
@Data
@Builder
public class LogSearchResponse {

    private long total;
    private int page;
    private int size;
    private List<LogView> items;
}
```

## 11. 第九步：创建 Service 接口

创建文件 [LogService.java](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\src\main\java\com\example\logsearch\service\LogService.java)。

```java
package com.example.logsearch.service;

import com.example.logsearch.document.LogDocument;
import com.example.logsearch.dto.LogSearchRequest;
import com.example.logsearch.dto.LogSearchResponse;

/**
 * 日志检索业务接口。
 */
public interface LogService {

    void initIndex();

    void loadDemoData();

    LogSearchResponse search(LogSearchRequest request);

    LogDocument getById(String id);
}
```

## 12. 第十步：创建配置类

创建文件 [ElasticsearchConfig.java](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\src\main\java\com\example\logsearch\config\ElasticsearchConfig.java)。

```java
package com.example.logsearch.config;

import org.springframework.context.annotation.Configuration;

/**
 * 预留的 Elasticsearch 配置类。
 */
@Configuration
public class ElasticsearchConfig {
}
```

## 13. 第十一步：创建 Service 实现类

创建文件 [LogServiceImpl.java](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\src\main\java\com\example\logsearch\service\impl\LogServiceImpl.java)。

```java
package com.example.logsearch.service.impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.json.JsonData;
import com.example.logsearch.document.LogDocument;
import com.example.logsearch.dto.LogSearchRequest;
import com.example.logsearch.dto.LogSearchResponse;
import com.example.logsearch.dto.LogView;
import com.example.logsearch.repository.LogRepository;
import com.example.logsearch.service.LogService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 日志检索业务实现类。
 */
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final LogRepository logRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void initIndex() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(LogDocument.class);
        if (indexOperations.exists()) {
            indexOperations.delete();
        }
        indexOperations.create();
        indexOperations.putMapping(indexOperations.createMapping(LogDocument.class));
    }

    @Override
    public void loadDemoData() {
        List<LogDocument> logs = List.of(
            LogDocument.builder()
                .id("1")
                .serviceName("order-service")
                .hostName("node-01")
                .level("INFO")
                .traceId("trace-1001")
                .message("创建订单请求处理成功。")
                .loggerName("OrderController")
                .threadName("http-nio-8080-exec-1")
                .createTime(LocalDateTime.now().minusMinutes(30))
                .build(),
            LogDocument.builder()
                .id("2")
                .serviceName("payment-service")
                .hostName("node-02")
                .level("ERROR")
                .traceId("trace-1002")
                .message("支付回调验签失败，签名不正确。")
                .loggerName("PaymentCallbackHandler")
                .threadName("http-nio-8081-exec-4")
                .createTime(LocalDateTime.now().minusMinutes(12))
                .build(),
            LogDocument.builder()
                .id("3")
                .serviceName("inventory-service")
                .hostName("node-03")
                .level("WARN")
                .traceId("trace-1003")
                .message("库存扣减耗时偏高，超过预期阈值。")
                .loggerName("InventoryService")
                .threadName("inventory-worker-1")
                .createTime(LocalDateTime.now().minusMinutes(5))
                .build()
        );

        // 批量写入演示日志
        logRepository.saveAll(logs);
    }

    @Override
    public LogSearchResponse search(LogSearchRequest request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 对 message 字段做全文检索
        if (StringUtils.hasText(request.getKeyword())) {
            boolBuilder.must(must -> must.match(match -> match
                .field("message")
                .query(request.getKeyword())
            ));
        }

        // 服务名精确过滤
        if (StringUtils.hasText(request.getServiceName())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("serviceName").value(request.getServiceName())));
        }

        // 级别精确过滤
        if (StringUtils.hasText(request.getLevel())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("level").value(request.getLevel())));
        }

        // traceId 精确过滤
        if (StringUtils.hasText(request.getTraceId())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("traceId").value(request.getTraceId())));
        }

        // 开始时间过滤
        if (StringUtils.hasText(request.getStartTime())) {
            boolBuilder.filter(filter -> filter.range(range -> range
                .date(date -> date.field("createTime").gte(JsonData.of(request.getStartTime())))
            ));
        }

        // 结束时间过滤
        if (StringUtils.hasText(request.getEndTime())) {
            boolBuilder.filter(filter -> filter.range(range -> range
                .date(date -> date.field("createTime").lte(JsonData.of(request.getEndTime())))
            ));
        }

        NativeQuery query = NativeQuery.builder()
            .withQuery(boolBuilder.build()._toQuery())
            .withPageable(PageRequest.of(request.getPage(), request.getSize()))
            // 日志检索里最常见的排序方式就是按时间倒序
            .withSort(sort -> sort.field(field -> field
                .field("createTime")
                .order(SortOrder.Desc)
            ))
            .build();

        SearchHits<LogDocument> hits = elasticsearchOperations.search(query, LogDocument.class);
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
            .page(request.getPage())
            .size(request.getSize())
            .items(items)
            .build();
    }

    @Override
    public LogDocument getById(String id) {
        return logRepository.findById(id).orElse(null);
    }
}
```

### 你要重点理解的地方

1. 日志检索和商品搜索不一样，它更强调精确过滤和时间范围
2. `message` 是全文字段，`serviceName`、`level`、`traceId` 是精确字段
3. 日志通常默认按 `createTime desc` 排序

## 14. 第十二步：创建 Controller

创建文件 [LogController.java](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\src\main\java\com\example\logsearch\controller\LogController.java)。

```java
package com.example.logsearch.controller;

import com.example.logsearch.dto.ApiResponse;
import com.example.logsearch.dto.LogSearchRequest;
import com.example.logsearch.service.LogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日志检索模块对外暴露的 HTTP 接口。
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @PostMapping("/init")
    public ApiResponse<String> initIndex() {
        logService.initIndex();
        return ApiResponse.success("日志索引初始化成功");
    }

    @PostMapping("/load-demo-data")
    public ApiResponse<String> loadDemoData() {
        logService.loadDemoData();
        return ApiResponse.success("日志演示数据导入成功");
    }

    @GetMapping("/search")
    public ApiResponse<?> search(@Valid @ModelAttribute LogSearchRequest request) {
        return ApiResponse.success(logService.search(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(logService.getById(id));
    }
}
```

## 15. 第十三步：创建测试类

创建文件 [LogSearchApplicationTests.java](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\src\test\java\com\example\logsearch\LogSearchApplicationTests.java)。

```java
package com.example.logsearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 最基础的启动测试。
 */
@SpringBootTest
class LogSearchApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

## 16. 第十四步：创建 `README.md`

创建文件 [README.md](D:\Gitee\demo\mysql-redis-mq-demo\es-log-search-demo\README.md)。

````md
# es-log-search-demo

## 启动 Elasticsearch

```bash
docker compose -f docker/docker-compose.yml up -d
```

## 启动项目

```bash
mvn spring-boot:run
```

## 初始化索引

`POST http://localhost:8082/api/logs/init`

## 导入演示数据

`POST http://localhost:8082/api/logs/load-demo-data`
````

## 17. 第十五步：启动项目

执行：

```bash
mvn clean package
mvn spring-boot:run
```

## 18. 第十六步：初始化索引并导入数据

调用：

```http
POST http://localhost:8082/api/logs/init
POST http://localhost:8082/api/logs/load-demo-data
```

## 19. 第十七步：验证搜索接口

示例请求：

```http
GET http://localhost:8082/api/logs/search?level=ERROR&page=0&size=10
GET http://localhost:8082/api/logs/search?serviceName=payment-service&keyword=验签&page=0&size=10
```

可能的返回结果：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1,
    "page": 0,
    "size": 10,
    "items": [
      {
        "id": "2",
        "serviceName": "payment-service",
        "hostName": "node-02",
        "level": "ERROR",
        "traceId": "trace-1002",
        "message": "支付回调验签失败，签名不正确。",
        "loggerName": "PaymentCallbackHandler",
        "threadName": "http-nio-8081-exec-4",
        "createTime": "2026-03-11T09:48:00"
      }
    ]
  }
}
```

## 20. 常见问题排查

### 问题 1：时间范围过滤后查不到数据

常见原因：

- 时间格式不对

建议格式：

- `2026-03-11T09:00:00`

### 问题 2：结果排序不对

常见原因：

- 你改动了 Service 里固定的 `createTime desc`

### 问题 3：关键字搜不到

常见原因：

- 你搜索的词并没有出现在 `message` 字段中

## 21. 你学会了什么

做完这个项目后，你应该已经理解了：

- 日志检索和商品、文章搜索的区别
- 时间范围过滤怎么写
- 为什么日志几乎总是按时间倒序看最新数据
