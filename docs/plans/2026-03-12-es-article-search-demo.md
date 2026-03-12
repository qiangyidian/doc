# es-article-search-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot + Elasticsearch 项目，完成“文章搜索”练习模块，让初学者掌握 ES 的多字段搜索、标签过滤、状态过滤和搜索结果高亮。

**Architecture:** 项目主链路是 `Controller -> Service -> ElasticsearchRepository / ElasticsearchOperations -> Elasticsearch`。文章写入使用 `Repository`，文章搜索使用 `ElasticsearchOperations` 执行多字段全文检索和高亮查询。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Web, Spring Validation, Spring Data Elasticsearch, Elasticsearch 8.10.4, Docker Compose, Maven, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 Elasticsearch

文章搜索通常出现在博客平台、内容管理系统、帮助中心和内部知识库中。

这类场景的搜索通常不是只查一个标题字段，而是会同时涉及：

- 标题
- 摘要
- 正文
- 标签
- 作者
- 发布状态

如果只依赖数据库 `LIKE` 查询，通常很难同时做到：

- 查询性能可接受
- 搜索结果支持高亮
- 多字段搜索体验自然

所以这类内容搜索场景非常适合使用 Elasticsearch。

---

## 二、最终目录结构

```text
es-article-search-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/esarticlesearch
│   │   │   ├── EsArticleSearchApplication.java
│   │   │   ├── common
│   │   │   │   └── Result.java
│   │   │   ├── controller
│   │   │   │   └── ArticleController.java
│   │   │   ├── document
│   │   │   │   └── ArticleDocument.java
│   │   │   ├── dto
│   │   │   │   ├── ArticleCreateRequest.java
│   │   │   │   ├── ArticleSearchRequest.java
│   │   │   │   ├── ArticleSearchResponse.java
│   │   │   │   └── ArticleView.java
│   │   │   ├── repository
│   │   │   │   └── ArticleRepository.java
│   │   │   └── service
│   │   │       ├── ArticleService.java
│   │   │       └── impl
│   │   │           └── ArticleServiceImpl.java
│   │   └── resources
│   │       └── application.yml
│   └── test
│       └── java/com/example/esarticlesearch
│           └── EsArticleSearchApplicationTests.java
```

这段目录结构的作用：

- 让你提前知道要创建哪些文件
- 把“文章文档”“搜索请求”“搜索结果”和“业务逻辑”拆开，后面照着创建不容易乱

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/esarticlesearch/EsArticleSearchApplication.java`
- Create: `src/main/resources/application.yml`
- Test: `src/test/java/com/example/esarticlesearch/EsArticleSearchApplicationTests.java`

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
    <artifactId>es-article-search-demo</artifactId>
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

- 提供 Web、参数校验和 Spring Data Elasticsearch 基础能力
- 依赖组合和商品搜索项目保持一致，方便你横向对比学习

**Step 2: 创建 `docker-compose.yml`**

```yaml
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.10.4
    container_name: es-article-search
    restart: always
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
    ports:
      - "9202:9200"
```

这段代码的作用：

- 启动当前文章搜索项目专属的 ES 容器
- 单节点模式更适合本地练习，配置也最简单

**Step 3: 创建启动类 `EsArticleSearchApplication.java`**

```java
package com.example.esarticlesearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EsArticleSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsArticleSearchApplication.class, args);
    }
}
```

这段代码的作用：

- 这是文章搜索项目的启动入口
- 所有 Controller、Service、Repository 都会从这里被 Spring Boot 扫描到

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8142

spring:
  application:
    name: es-article-search-demo
  elasticsearch:
    uris: http://localhost:9202

logging:
  level:
    org.springframework.data.elasticsearch.client.elc: info
```

这段代码的作用：

- 把应用端口固定到 `8142`
- 让 Spring Data Elasticsearch 连接当前项目自己的 ES 端口 `9202`

**Step 5: 创建测试类 `EsArticleSearchApplicationTests.java`**

```java
package com.example.esarticlesearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EsArticleSearchApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

这段代码的作用：

- 这是最基础的上下文加载测试
- 它的目标不是测业务，而是先确认 Spring Boot 配置没有直接启动失败

---

### Task 2: 创建公共返回对象、文章文档模型和 DTO

**Files:**
- Create: `src/main/java/com/example/esarticlesearch/common/Result.java`
- Create: `src/main/java/com/example/esarticlesearch/document/ArticleDocument.java`
- Create: `src/main/java/com/example/esarticlesearch/dto/ArticleCreateRequest.java`
- Create: `src/main/java/com/example/esarticlesearch/dto/ArticleSearchRequest.java`
- Create: `src/main/java/com/example/esarticlesearch/dto/ArticleSearchResponse.java`
- Create: `src/main/java/com/example/esarticlesearch/dto/ArticleView.java`
- Create: `src/main/java/com/example/esarticlesearch/repository/ArticleRepository.java`

**Step 1: 创建 `Result.java`**

```java
package com.example.esarticlesearch.common;

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

- 统一文章模块的接口返回结构
- 和其他练习项目保持一致，便于你形成固定开发习惯

**Step 2: 创建 `ArticleDocument.java`**

```java
package com.example.esarticlesearch.document;

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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "article_index")
public class ArticleDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String summary;

    @Field(type = FieldType.Text)
    private String content;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String author;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private LocalDateTime publishTime;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private LocalDateTime updateTime;

    @Field(type = FieldType.Integer)
    private Integer viewCount;
}
```

这段代码的作用：

- `title`、`summary`、`content` 是全文搜索字段
- `category`、`author`、`tags`、`status` 更适合做过滤条件
- 这是内容搜索非常典型的字段建模方式

**Step 3: 创建 `ArticleCreateRequest.java`**

```java
package com.example.esarticlesearch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class ArticleCreateRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String summary;

    @NotBlank(message = "正文不能为空")
    private String content;

    @NotBlank(message = "分类不能为空")
    private String category;

    @NotBlank(message = "作者不能为空")
    private String author;

    @NotEmpty(message = "标签不能为空")
    private List<String> tags;

    @NotBlank(message = "状态不能为空")
    private String status;
}
```

这段代码的作用：

- 定义新增文章接口的请求体
- 标签和状态都是后续检索时很重要的字段，所以这里直接要求必填

**Step 4: 创建 `ArticleSearchRequest.java`**

```java
package com.example.esarticlesearch.dto;

import lombok.Data;

@Data
public class ArticleSearchRequest {

    private String keyword;
    private String category;
    private String tag;
    private String author;
    private String status;
    private String sortField = "publishTime";
    private String sortDirection = "desc";
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
```

这段代码的作用：

- 收敛文章搜索里最常用的查询条件
- 默认按发布时间倒序展示，更符合内容平台搜索习惯

**Step 5: 创建 `ArticleView.java`**

```java
package com.example.esarticlesearch.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

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

这段代码的作用：

- 这是返回给前端的文章展示对象
- `title` 和 `content` 会在搜索命中时被高亮内容替换

**Step 6: 创建 `ArticleSearchResponse.java`**

```java
package com.example.esarticlesearch.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArticleSearchResponse {

    private long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<ArticleView> items;
}
```

这段代码的作用：

- 统一封装文章分页查询结果
- 便于前端直接渲染“总条数 + 当前页数据”

**Step 7: 创建 `ArticleRepository.java`**

```java
package com.example.esarticlesearch.repository;

import com.example.esarticlesearch.document.ArticleDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ArticleRepository extends ElasticsearchRepository<ArticleDocument, String> {
}
```

这段代码的作用：

- 提供基础的文章文档保存和查询能力
- 复杂检索继续通过 `ElasticsearchOperations` 完成

---

### Task 3: 创建 Service，实现多字段搜索和高亮

**Files:**
- Create: `src/main/java/com/example/esarticlesearch/service/ArticleService.java`
- Create: `src/main/java/com/example/esarticlesearch/service/impl/ArticleServiceImpl.java`

**Step 1: 创建 `ArticleService.java`**

```java
package com.example.esarticlesearch.service;

import com.example.esarticlesearch.document.ArticleDocument;
import com.example.esarticlesearch.dto.ArticleCreateRequest;
import com.example.esarticlesearch.dto.ArticleSearchRequest;
import com.example.esarticlesearch.dto.ArticleSearchResponse;

public interface ArticleService {

    void resetAndLoadDemoData();

    ArticleDocument save(ArticleCreateRequest request);

    ArticleSearchResponse search(ArticleSearchRequest request);

    ArticleDocument getById(String id);
}
```

这段代码的作用：

- 先把文章模块的核心能力抽象出来
- Controller 只依赖接口，不直接依赖实现细节

**Step 2: 创建 `ArticleServiceImpl.java`**

```java
package com.example.esarticlesearch.service.impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.esarticlesearch.document.ArticleDocument;
import com.example.esarticlesearch.dto.ArticleCreateRequest;
import com.example.esarticlesearch.dto.ArticleSearchRequest;
import com.example.esarticlesearch.dto.ArticleSearchResponse;
import com.example.esarticlesearch.dto.ArticleView;
import com.example.esarticlesearch.repository.ArticleRepository;
import com.example.esarticlesearch.service.ArticleService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("publishTime", "updateTime", "viewCount");

    private final ArticleRepository articleRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void resetAndLoadDemoData() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(ArticleDocument.class);
        if (indexOperations.exists()) {
            indexOperations.delete();
        }
        indexOperations.create();
        indexOperations.putMapping(indexOperations.createMapping(ArticleDocument.class));

        List<ArticleDocument> articles = List.of(
            ArticleDocument.builder()
                .id(UUID.randomUUID().toString())
                .title("Spring Boot Elasticsearch Guide")
                .summary("A beginner guide for integrating Spring Boot with Elasticsearch")
                .content("This article introduces index creation, document mapping, query building and highlight usage.")
                .category("backend")
                .author("Alice")
                .tags(List.of("spring", "elasticsearch", "search"))
                .status("PUBLISHED")
                .publishTime(LocalDateTime.now().minusDays(6))
                .updateTime(LocalDateTime.now().minusDays(2))
                .viewCount(1200)
                .build(),
            ArticleDocument.builder()
                .id(UUID.randomUUID().toString())
                .title("How to Design Search Features")
                .summary("Understand the difference between full text search and exact filtering")
                .content("A good search feature starts from field modeling, then query DSL, and then result rendering.")
                .category("architecture")
                .author("Bob")
                .tags(List.of("search", "architecture"))
                .status("PUBLISHED")
                .publishTime(LocalDateTime.now().minusDays(4))
                .updateTime(LocalDateTime.now().minusDays(1))
                .viewCount(860)
                .build(),
            ArticleDocument.builder()
                .id(UUID.randomUUID().toString())
                .title("Content Review Draft")
                .summary("This is an internal draft article")
                .content("The article is still under editing workflow and should not be exposed to end users.")
                .category("process")
                .author("Alice")
                .tags(List.of("workflow", "draft"))
                .status("DRAFT")
                .publishTime(LocalDateTime.now().minusDays(1))
                .updateTime(LocalDateTime.now())
                .viewCount(16)
                .build()
        );

        articleRepository.saveAll(articles);
    }

    @Override
    public ArticleDocument save(ArticleCreateRequest request) {
        ArticleDocument document = ArticleDocument.builder()
            .id(UUID.randomUUID().toString())
            .title(request.getTitle())
            .summary(request.getSummary())
            .content(request.getContent())
            .category(request.getCategory())
            .author(request.getAuthor())
            .tags(request.getTags())
            .status(request.getStatus())
            .publishTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .viewCount(0)
            .build();

        return articleRepository.save(document);
    }

    @Override
    public ArticleSearchResponse search(ArticleSearchRequest request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        boolean hasCondition = false;

        if (StringUtils.hasText(request.getKeyword())) {
            boolBuilder.must(must -> must.multiMatch(multi -> multi
                .query(request.getKeyword())
                .fields("title", "summary", "content")
            ));
            hasCondition = true;
        }

        if (StringUtils.hasText(request.getCategory())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("category").value(request.getCategory())));
            hasCondition = true;
        }

        if (StringUtils.hasText(request.getTag())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("tags").value(request.getTag())));
            hasCondition = true;
        }

        if (StringUtils.hasText(request.getAuthor())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("author").value(request.getAuthor())));
            hasCondition = true;
        }

        if (StringUtils.hasText(request.getStatus())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("status").value(request.getStatus())));
            hasCondition = true;
        }

        String sortField = ALLOWED_SORT_FIELDS.contains(request.getSortField()) ? request.getSortField() : "publishTime";
        SortOrder sortOrder = "asc".equalsIgnoreCase(request.getSortDirection()) ? SortOrder.Asc : SortOrder.Desc;
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();

        Query query = hasCondition
            ? boolBuilder.build()._toQuery()
            : Query.of(q -> q.matchAll(m -> m));

        NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(query)
            .withPageable(PageRequest.of(pageNum - 1, pageSize))
            .withSort(sort -> sort.field(field -> field.field(sortField).order(sortOrder)))
            .withHighlightQuery(new HighlightQuery(
                List.of(new HighlightField("title"), new HighlightField("content")),
                HighlightParameters.builder()
                    .withPreTags("<em>")
                    .withPostTags("</em>")
                    .build()
            ))
            .build();

        SearchHits<ArticleDocument> hits = elasticsearchOperations.search(nativeQuery, ArticleDocument.class);
        List<ArticleView> items = new ArrayList<>();

        for (SearchHit<ArticleDocument> hit : hits) {
            ArticleDocument article = hit.getContent();
            Map<String, List<String>> highlightFields = hit.getHighlightFields();

            String title = article.getTitle();
            if (!CollectionUtils.isEmpty(highlightFields.get("title"))) {
                title = highlightFields.get("title").get(0);
            }

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
            .pageNum(pageNum)
            .pageSize(pageSize)
            .items(items)
            .build();
    }

    @Override
    public ArticleDocument getById(String id) {
        return articleRepository.findById(id).orElse(null);
    }
}
```

这段代码的作用：

- `multiMatch` 让一个关键字同时搜索标题、摘要和正文
- `term` 过滤负责分类、标签、作者、状态这些结构化条件
- `HighlightQuery` 告诉 ES 把命中的 `title` 和 `content` 高亮返回
- 最终返回给前端的不是原始文档，而是带高亮片段的 `ArticleView`

---

### Task 4: 创建 Controller，对外暴露接口

**Files:**
- Create: `src/main/java/com/example/esarticlesearch/controller/ArticleController.java`

**Step 1: 创建 `ArticleController.java`**

```java
package com.example.esarticlesearch.controller;

import com.example.esarticlesearch.common.Result;
import com.example.esarticlesearch.document.ArticleDocument;
import com.example.esarticlesearch.dto.ArticleCreateRequest;
import com.example.esarticlesearch.dto.ArticleSearchRequest;
import com.example.esarticlesearch.dto.ArticleSearchResponse;
import com.example.esarticlesearch.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping("/init-demo")
    public Result<String> initDemo() {
        articleService.resetAndLoadDemoData();
        return Result.success("文章索引已重建并写入演示数据");
    }

    @PostMapping
    public Result<ArticleDocument> create(@Valid @RequestBody ArticleCreateRequest request) {
        return Result.success(articleService.save(request));
    }

    @PostMapping("/search")
    public Result<ArticleSearchResponse> search(@RequestBody ArticleSearchRequest request) {
        return Result.success(articleService.search(request));
    }

    @GetMapping("/{id}")
    public Result<ArticleDocument> getById(@PathVariable String id) {
        return Result.success(articleService.getById(id));
    }
}
```

这段代码的作用：

- 暴露初始化、写入、搜索、详情 4 个接口
- 这已经足够你把文章写入 ES 和从 ES 搜出来的主链路完整跑通

---

## 四、启动顺序

**Step 1: 启动 Elasticsearch**

```bash
docker compose up -d
```

这段命令的作用：

- 启动文章搜索项目专属的 ES 容器
- 当前项目只占用 `9202` 端口，不会影响其他项目

**Step 2: 启动 Spring Boot**

```bash
mvn spring-boot:run
```

这段命令的作用：

- 启动文章搜索应用
- 启动后应用会监听 `8142` 端口

---

## 五、接口测试顺序

**Step 1: 初始化演示数据**

```bash
curl -X POST http://localhost:8142/articles/init-demo
```

这段命令的作用：

- 重建 `article_index`
- 写入 3 条演示文章

**Step 2: 搜索 spring 相关文章**

```bash
curl -X POST http://localhost:8142/articles/search ^
  -H "Content-Type: application/json" ^
  -d "{\"keyword\":\"spring\",\"status\":\"PUBLISHED\",\"pageNum\":1,\"pageSize\":10}"
```

这段命令的作用：

- 演示关键字搜索 + 已发布状态过滤
- 如果命中标题或正文，高亮片段会出现在返回结果里

**Step 3: 搜索带 `search` 标签的文章**

```bash
curl -X POST http://localhost:8142/articles/search ^
  -H "Content-Type: application/json" ^
  -d "{\"tag\":\"search\",\"sortField\":\"viewCount\",\"sortDirection\":\"desc\"}"
```

这段命令的作用：

- 演示标签过滤和按阅读量排序
- 这很接近内容平台“搜索结果按热度排序”的常见需求

---

## 六、常见报错排查

### 1. 搜索结果没有高亮

原因：

- 你查询的关键字没有命中 `title` 或 `content`
- 或者你看的不是搜索接口返回，而是详情接口返回

### 2. 标签过滤没有结果

原因：

- `tags` 是精确过滤字段
- 你传入的值必须和文档里的标签值完全一致，比如 `search`

### 3. ES 启动很慢

排查命令：

```bash
docker compose logs -f elasticsearch
```

这段命令的作用：

- 查看 ES 容器启动日志
- 判断是不是内存不足，或者容器还在初始化

---

## 七、你在这个项目里学到了什么

做完这个项目后，你应该已经掌握：

- 内容搜索里“全文字段”和“过滤字段”的划分方式
- 如何使用 `multiMatch` 做多字段搜索
- 如何用 `HighlightQuery` 返回高亮结果
- 为什么文章、帮助中心、知识库这类系统非常适合 Elasticsearch
