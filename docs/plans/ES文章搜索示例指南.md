# 文章搜索 Elasticsearch 项目实战文档

## 1. 项目目标

你要新建一个 Spring Boot 项目，项目名叫 `es-article-search-demo`。

这个项目模拟的是生产环境中的文章搜索场景，比如：

- 博客系统
- 帮助中心
- CMS 内容平台
- 内部知识库

用户会输入关键词搜索文章，然后再按这些条件继续过滤：

- 分类
- 标签
- 作者
- 状态
- 排序字段
- 分页参数

这个项目的学习重点是：

- 多字段全文检索
- 标签过滤
- 作者过滤
- 发布时间排序
- 分页
- 标题和正文高亮

## 2. 最终目录结构

```text
es-article-search-demo
├─ docker
│  └─ docker-compose.yml
├─ src
│  ├─ main
│  │  ├─ java/com/example/articlesearch
│  │  │  ├─ config
│  │  │  │  └─ ElasticsearchConfig.java
│  │  │  ├─ controller
│  │  │  │  └─ ArticleController.java
│  │  │  ├─ document
│  │  │  │  └─ ArticleDocument.java
│  │  │  ├─ dto
│  │  │  │  ├─ ApiResponse.java
│  │  │  │  ├─ ArticleSearchRequest.java
│  │  │  │  ├─ ArticleSearchResponse.java
│  │  │  │  └─ ArticleView.java
│  │  │  ├─ repository
│  │  │  │  └─ ArticleRepository.java
│  │  │  ├─ service
│  │  │  │  ├─ ArticleService.java
│  │  │  │  └─ impl
│  │  │  │     └─ ArticleServiceImpl.java
│  │  │  └─ EsArticleSearchDemoApplication.java
│  │  └─ resources
│  │     └─ application.yml
│  └─ test
│     └─ java/com/example/articlesearch
│        └─ ArticleSearchApplicationTests.java
├─ pom.xml
└─ README.md
```

## 3. 第一步：创建 Spring Boot 工程

推荐参数如下：

- `GroupId`：`com.example`
- `ArtifactId`：`es-article-search-demo`
- `Package name`：`com.example.articlesearch`
- `Java`：`17`

依赖选择：

- Spring Web
- Spring Data Elasticsearch
- Validation
- Lombok
- Spring Boot Test

## 4. 第二步：创建 `pom.xml`

创建文件 [pom.xml](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\pom.xml)。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 父工程，用来统一依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.6</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>es-article-search-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>es-article-search-demo</name>

    <properties>
        <!-- 当前项目统一使用 Java 17 -->
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- 提供 REST 接口开发能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 提供 Elasticsearch 操作能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
        </dependency>

        <!-- 参数校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- 简化 Java 样板代码 -->
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

创建文件 [docker/docker-compose.yml](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\docker\docker-compose.yml)。

```yaml
version: "3.8"

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.15.3
    container_name: es-article-search
    environment:
      # 单机模式，适合本地练习
      discovery.type: single-node
      # 关闭认证，简化初学配置
      xpack.security.enabled: "false"
      # 限制 ES 的 JVM 内存
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"
    ports:
      # 文章项目使用 9201，避免和商品项目冲突
      - "9201:9200"
    volumes:
      - es_article_data:/usr/share/elasticsearch/data

  kibana:
    image: docker.elastic.co/kibana/kibana:8.15.3
    container_name: kibana-article-search
    environment:
      ELASTICSEARCH_HOSTS: '["http://elasticsearch:9200"]'
    depends_on:
      - elasticsearch
    ports:
      # 文章项目的 Kibana 端口使用 5602
      - "5602:5601"

volumes:
  es_article_data:
```

启动命令：

```bash
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.yml ps
```

## 6. 第四步：创建 `application.yml`

创建文件 [application.yml](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\src\main\resources\application.yml)。

```yaml
server:
  port: 8081

spring:
  application:
    name: es-article-search-demo
  elasticsearch:
    uris: http://localhost:9201

logging:
  level:
    root: info
    org.springframework.data.elasticsearch.client.WIRE: trace
```

## 7. 第五步：创建启动类

创建文件 [EsArticleSearchDemoApplication.java](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\src\main\java\com\example\articlesearch\EsArticleSearchDemoApplication.java)。

```java
package com.example.articlesearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 文章搜索项目启动类。
 */
@SpringBootApplication
public class EsArticleSearchDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsArticleSearchDemoApplication.class, args);
    }
}
```

## 8. 第六步：创建索引实体类

创建文件 [ArticleDocument.java](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\src\main\java\com\example\articlesearch\document\ArticleDocument.java)。

```java
package com.example.articlesearch.document;

import java.time.LocalDateTime;
import java.util.List;
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
 * ArticleDocument 表示 Elasticsearch 中的一条文章文档。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "articles")
public class ArticleDocument {

    @Id
    private String id;

    // 文章标题，是最重要的搜索字段之一
    @Field(type = FieldType.Text)
    private String title;

    // 摘要可以帮助补充搜索命中范围
    @Field(type = FieldType.Text)
    private String summary;

    // 正文是全文搜索的核心字段
    @Field(type = FieldType.Text)
    private String content;

    // 分类用于精确过滤
    @Field(type = FieldType.Keyword)
    private String category;

    // 作者用于精确过滤
    @Field(type = FieldType.Keyword)
    private String author;

    // 标签列表用于精确过滤
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    // 状态字段用于区分已发布和草稿
    @Field(type = FieldType.Keyword)
    private String status;

    // 发布时间通常会参与排序
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime publishTime;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updateTime;

    // 浏览量字段可以作为另一种排序条件
    @Field(type = FieldType.Integer)
    private Integer viewCount;
}
```

## 9. 第七步：创建 Repository

创建文件 [ArticleRepository.java](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\src\main\java\com\example\articlesearch\repository\ArticleRepository.java)。

```java
package com.example.articlesearch.repository;

import com.example.articlesearch.document.ArticleDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 文章索引的基础 Repository。
 */
public interface ArticleRepository extends ElasticsearchRepository<ArticleDocument, String> {
}
```

## 10. 第八步：创建 DTO

创建 [ApiResponse.java](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\src\main\java\com\example\articlesearch\dto\ApiResponse.java)。

```java
package com.example.articlesearch.dto;

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

创建 [ArticleSearchRequest.java](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\src\main\java\com\example\articlesearch\dto\ArticleSearchRequest.java)。

```java
package com.example.articlesearch.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 接收文章搜索请求参数。
 */
@Data
public class ArticleSearchRequest {

    // 搜索关键词，会匹配标题、摘要、正文
    private String keyword;

    // 分类过滤
    private String category;

    // 标签过滤
    private String tag;

    // 作者过滤
    private String author;

    // 状态过滤，比如 PUBLISHED
    private String status;

    @Min(0)
    private Integer page = 0;

    @Min(1)
    private Integer size = 10;

    // 当前示例支持按发布时间或浏览量排序
    private String sortBy = "publishTime";

    private String sortDirection = "desc";
}
```

创建 [ArticleView.java](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\src\main\java\com\example\articlesearch\dto\ArticleView.java)。

```java
package com.example.articlesearch.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 返回给前端的文章对象。
 */
@Data
@Builder
public class ArticleView {

    private String id;
    private String title;
    private String summary;
    private String content;
    private String category;
    private String author;
    private List<String> tags;
    private String status;
    private LocalDateTime publishTime;
    private LocalDateTime updateTime;
    private Integer viewCount;
}
```

创建 [ArticleSearchResponse.java](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\src\main\java\com\example\articlesearch\dto\ArticleSearchResponse.java)。

```java
package com.example.articlesearch.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 文章搜索分页返回对象。
 */
@Data
@Builder
public class ArticleSearchResponse {

    private long total;
    private int page;
    private int size;
    private List<ArticleView> items;
}
```

## 11. 第九步：创建 Service 接口

创建文件 [ArticleService.java](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\src\main\java\com\example\articlesearch\service\ArticleService.java)。

```java
package com.example.articlesearch.service;

import com.example.articlesearch.document.ArticleDocument;
import com.example.articlesearch.dto.ArticleSearchRequest;
import com.example.articlesearch.dto.ArticleSearchResponse;

/**
 * 文章搜索业务接口。
 */
public interface ArticleService {

    void initIndex();

    void loadDemoData();

    ArticleSearchResponse search(ArticleSearchRequest request);

    ArticleDocument getById(String id);
}
```

## 12. 第十步：创建配置类

创建文件 [ElasticsearchConfig.java](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\src\main\java\com\example\articlesearch\config\ElasticsearchConfig.java)。

```java
package com.example.articlesearch.config;

import org.springframework.context.annotation.Configuration;

/**
 * 预留的 Elasticsearch 配置类。
 * 当前版本主要使用 Spring Boot 自动配置。
 */
@Configuration
public class ElasticsearchConfig {
}
```

## 13. 第十一步：创建 Service 实现类

创建文件 [ArticleServiceImpl.java](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\src\main\java\com\example\articlesearch\service\impl\ArticleServiceImpl.java)。

```java
package com.example.articlesearch.service.impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.example.articlesearch.document.ArticleDocument;
import com.example.articlesearch.dto.ArticleSearchRequest;
import com.example.articlesearch.dto.ArticleSearchResponse;
import com.example.articlesearch.dto.ArticleView;
import com.example.articlesearch.repository.ArticleRepository;
import com.example.articlesearch.service.ArticleService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightField;
import org.springframework.data.elasticsearch.core.query.HighlightParameters;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 文章搜索业务实现类。
 */
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void initIndex() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(ArticleDocument.class);
        if (indexOperations.exists()) {
            indexOperations.delete();
        }
        indexOperations.create();
        indexOperations.putMapping(indexOperations.createMapping(ArticleDocument.class));
    }

    @Override
    public void loadDemoData() {
        List<ArticleDocument> documents = List.of(
            ArticleDocument.builder()
                .id("1")
                .title("Spring Boot Elasticsearch 入门指南")
                .summary("这是一篇讲解 Spring Boot 集成 Elasticsearch 的实践文章。")
                .content("本文会介绍索引创建、文档映射、查询写法以及高亮显示。")
                .category("backend")
                .author("Alice")
                .tags(List.of("spring-boot", "elasticsearch", "tutorial"))
                .status("PUBLISHED")
                .publishTime(LocalDateTime.now().minusDays(6))
                .updateTime(LocalDateTime.now().minusDays(2))
                .viewCount(1200)
                .build(),
            ArticleDocument.builder()
                .id("2")
                .title("如何设计搜索功能")
                .summary("理解全文检索和精确过滤之间的区别。")
                .content("好的搜索设计从字段建模开始，然后才是查询写法和结果展示。")
                .category("architecture")
                .author("Bob")
                .tags(List.of("search", "architecture"))
                .status("PUBLISHED")
                .publishTime(LocalDateTime.now().minusDays(4))
                .updateTime(LocalDateTime.now().minusDays(1))
                .viewCount(880)
                .build(),
            ArticleDocument.builder()
                .id("3")
                .title("内容审核流程草稿")
                .summary("这是一篇还没有发布的内部草稿。")
                .content("当前文章仍处于编辑流转阶段，还不应该被外部用户看到。")
                .category("process")
                .author("Alice")
                .tags(List.of("workflow", "draft"))
                .status("DRAFT")
                .publishTime(LocalDateTime.now().minusDays(1))
                .updateTime(LocalDateTime.now())
                .viewCount(15)
                .build()
        );

        // 批量写入演示数据
        articleRepository.saveAll(documents);
    }

    @Override
    public ArticleSearchResponse search(ArticleSearchRequest request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 如果有关键字，就同时搜索标题、摘要、正文
        if (StringUtils.hasText(request.getKeyword())) {
            boolBuilder.must(must -> must.multiMatch(multiMatch -> multiMatch
                .query(request.getKeyword())
                .fields("title", "summary", "content")
            ));
        }

        // 分类过滤
        if (StringUtils.hasText(request.getCategory())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("category").value(request.getCategory())));
        }

        // 标签过滤
        if (StringUtils.hasText(request.getTag())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("tags").value(request.getTag())));
        }

        // 作者过滤
        if (StringUtils.hasText(request.getAuthor())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("author").value(request.getAuthor())));
        }

        // 状态过滤，比如只看已发布文章
        if (StringUtils.hasText(request.getStatus())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("status").value(request.getStatus())));
        }

        NativeQuery query = NativeQuery.builder()
            .withQuery(boolBuilder.build()._toQuery())
            .withPageable(PageRequest.of(request.getPage(), request.getSize()))
            .withSort(sort -> sort.field(field -> field
                .field(request.getSortBy())
                .order("asc".equalsIgnoreCase(request.getSortDirection()) ? SortOrder.Asc : SortOrder.Desc)
            ))
            // 对 title 和 content 两个字段做高亮
            .withHighlightQuery(new HighlightQuery(
                List.of(new HighlightField("title"), new HighlightField("content")),
                HighlightParameters.builder()
                    .withPreTags("<em>")
                    .withPostTags("</em>")
                    .build()
            ))
            .build();

        SearchHits<ArticleDocument> hits = elasticsearchOperations.search(query, ArticleDocument.class);
        List<ArticleView> items = new ArrayList<>();

        for (SearchHit<ArticleDocument> hit : hits) {
            ArticleDocument article = hit.getContent();
            Map<String, List<String>> highlightFields = hit.getHighlightFields();

            // 默认先用原始标题
            String title = article.getTitle();
            if (!CollectionUtils.isEmpty(highlightFields.get("title"))) {
                title = highlightFields.get("title").get(0);
            }

            // 默认先用原始正文
            String content = article.getContent();
            if (!CollectionUtils.isEmpty(highlightFields.get("content"))) {
                content = highlightFields.get("content").get(0);
            }

            items.add(ArticleView.builder()
                .id(article.getId())
                .title(title)
                .summary(article.getSummary())
                .content(content)
                .category(article.getCategory())
                .author(article.getAuthor())
                .tags(article.getTags())
                .status(article.getStatus())
                .publishTime(article.getPublishTime())
                .updateTime(article.getUpdateTime())
                .viewCount(article.getViewCount())
                .build());
        }

        return ArticleSearchResponse.builder()
            .total(hits.getTotalHits())
            .page(request.getPage())
            .size(request.getSize())
            .items(items)
            .build();
    }

    @Override
    public ArticleDocument getById(String id) {
        return articleRepository.findById(id).orElse(null);
    }
}
```

### 你要重点理解的地方

1. 文章搜索通常不是只搜一个字段，而是同时搜多个字段
2. `tags`、`author`、`status` 更适合精确过滤
3. 高亮不一定只能做在标题，也可以做在正文里

## 14. 第十二步：创建 Controller

创建文件 [ArticleController.java](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\src\main\java\com\example\articlesearch\controller\ArticleController.java)。

```java
package com.example.articlesearch.controller;

import com.example.articlesearch.dto.ApiResponse;
import com.example.articlesearch.dto.ArticleSearchRequest;
import com.example.articlesearch.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文章搜索模块对外暴露的 HTTP 接口。
 */
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping("/init")
    public ApiResponse<String> initIndex() {
        articleService.initIndex();
        return ApiResponse.success("文章索引初始化成功");
    }

    @PostMapping("/load-demo-data")
    public ApiResponse<String> loadDemoData() {
        articleService.loadDemoData();
        return ApiResponse.success("文章演示数据导入成功");
    }

    @GetMapping("/search")
    public ApiResponse<?> search(@Valid @ModelAttribute ArticleSearchRequest request) {
        return ApiResponse.success(articleService.search(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(articleService.getById(id));
    }
}
```

## 15. 第十三步：创建测试类

创建文件 [ArticleSearchApplicationTests.java](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\src\test\java\com\example\articlesearch\ArticleSearchApplicationTests.java)。

```java
package com.example.articlesearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 最基础的项目启动测试。
 */
@SpringBootTest
class ArticleSearchApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

## 16. 第十四步：创建 `README.md`

创建文件 [README.md](D:\Gitee\demo\mysql-redis-mq-demo\es-article-search-demo\README.md)。

````md
# es-article-search-demo

## 启动 Elasticsearch

```bash
docker compose -f docker/docker-compose.yml up -d
```

## 启动项目

```bash
mvn spring-boot:run
```

## 初始化索引

`POST http://localhost:8081/api/articles/init`

## 导入演示数据

`POST http://localhost:8081/api/articles/load-demo-data`
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
POST http://localhost:8081/api/articles/init
POST http://localhost:8081/api/articles/load-demo-data
```

## 19. 第十七步：验证搜索接口

示例请求：

```http
GET http://localhost:8081/api/articles/search?keyword=搜索&status=PUBLISHED&page=0&size=10
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
        "title": "如何设计<em>搜索</em>功能",
        "summary": "理解全文检索和精确过滤之间的区别。",
        "content": "好的<em>搜索</em>设计从字段建模开始，然后才是查询写法和结果展示。",
        "category": "architecture",
        "author": "Bob",
        "tags": ["search", "architecture"],
        "status": "PUBLISHED",
        "publishTime": "2026-03-07T10:00:00",
        "updateTime": "2026-03-10T10:00:00",
        "viewCount": 880
      }
    ]
  }
}
```

## 20. 常见问题排查

### 问题 1：标签过滤不生效

常见原因：

- 传入的标签值和 ES 中保存的标签值不一致

排查方式：

- 先通过 `/api/articles/{id}` 看看实际存进去的标签值是什么

### 问题 2：把草稿文章也搜出来了

常见原因：

- 你没有传 `status=PUBLISHED`

### 问题 3：正文没有高亮

常见原因：

- 关键词没有真正命中 `content` 字段
- 你搜到的词可能只命中了标题

## 21. 你学会了什么

做完这个项目后，你应该已经理解了：

- 多字段全文检索怎么写
- 标签、作者、状态这些过滤条件怎么组合
- 标题和正文高亮是怎么返回的
