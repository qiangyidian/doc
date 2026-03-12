# es-product-search-demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 从零创建一个独立的 Spring Boot + Elasticsearch 项目，完成“商品搜索”练习模块，让初学者掌握 ES 的全文检索、精确过滤、范围查询、排序和分页。

**Architecture:** 项目主链路是 `Controller -> Service -> ElasticsearchRepository / ElasticsearchOperations -> Elasticsearch`。`Repository` 负责保存文档，`ElasticsearchOperations` 负责执行更灵活的组合查询。

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Web, Spring Validation, Spring Data Elasticsearch, Elasticsearch 8.10.4, Docker Compose, Maven, Lombok, JUnit 5

---

## 一、这个项目在生产环境里为什么会用 Elasticsearch

商品搜索是 Elasticsearch 最典型的使用场景之一。

一个真实的商品搜索请求，通常不只是“输个关键字然后查一下”，而是会同时包含：

- 关键字搜索
- 分类过滤
- 品牌过滤
- 价格区间过滤
- 是否上架过滤
- 按价格或时间排序
- 分页

如果你把这些条件全部压在关系型数据库的 `LIKE` 查询上，随着数据量变大，查询性能和搜索体验都会越来越差。  
Elasticsearch 更适合做这类“搜索型需求”。

---

## 二、最终目录结构

```text
es-product-search-demo
├── docker-compose.yml
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/example/esproductsearch
│   │   │   ├── EsProductSearchApplication.java
│   │   │   ├── common
│   │   │   │   └── Result.java
│   │   │   ├── controller
│   │   │   │   └── ProductController.java
│   │   │   ├── document
│   │   │   │   └── ProductDocument.java
│   │   │   ├── dto
│   │   │   │   ├── ProductCreateRequest.java
│   │   │   │   ├── ProductSearchRequest.java
│   │   │   │   ├── ProductSearchResponse.java
│   │   │   │   └── ProductView.java
│   │   │   ├── repository
│   │   │   │   └── ProductRepository.java
│   │   │   └── service
│   │   │       ├── ProductService.java
│   │   │       └── impl
│   │   │           └── ProductServiceImpl.java
│   │   └── resources
│   │       └── application.yml
│   └── test
│       └── java/com/example/esproductsearch
│           └── EsProductSearchApplicationTests.java
```

这段目录结构的作用：

- 让你先看清整个项目到底有哪些文件
- 把“文档模型”“查询入参”“控制器”“业务层”拆开，后面照着创建不容易乱
- 让你理解 ES 项目里最重要的是 `document`、`repository` 和 `service`

---

## 三、开始前先记住 2 个约束

1. 这个练习项目默认**不安装中文分词插件**
2. 演示搜索建议优先用英文关键字，比如 `iphone`、`laptop`、`monitor`

这段说明的作用：

- 避免你刚开始就被分词插件卡住
- 先把 ES 的核心查询流程跑通，再考虑中文搜索增强

---

### Task 1: 创建项目骨架和 Docker 环境

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/esproductsearch/EsProductSearchApplication.java`
- Create: `src/main/resources/application.yml`
- Test: `src/test/java/com/example/esproductsearch/EsProductSearchApplicationTests.java`

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
    <artifactId>es-product-search-demo</artifactId>
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

- `spring-boot-starter-data-elasticsearch` 提供 Spring Data ES 能力
- `spring-boot-starter-validation` 负责接口参数校验
- `spring-boot-starter-web` 负责对外提供 HTTP 接口

**Step 2: 创建 `docker-compose.yml`**

```yaml
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.10.4
    container_name: es-product-search
    restart: always
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
    ports:
      - "9201:9200"
```

这段代码的作用：

- 启动一个单机版 Elasticsearch 容器
- `discovery.type=single-node` 表示本地练习用单节点模式
- `xpack.security.enabled=false` 是为了让初学者先跳过认证配置

**Step 3: 创建启动类 `EsProductSearchApplication.java`**

```java
package com.example.esproductsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EsProductSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsProductSearchApplication.class, args);
    }
}
```

这段代码的作用：

- 这是 Spring Boot 项目的标准启动入口
- 后面运行 `mvn spring-boot:run` 时，就是从这里启动整个应用

**Step 4: 创建 `application.yml`**

```yaml
server:
  port: 8141

spring:
  application:
    name: es-product-search-demo
  elasticsearch:
    uris: http://localhost:9201

logging:
  level:
    org.springframework.data.elasticsearch.client.elc: info
```

这段代码的作用：

- 把应用端口固定到 `8141`
- 把 Spring Data Elasticsearch 指向本地 Docker 启动的 ES 地址
- 打开 ES 客户端日志，方便你排查请求有没有真正发出去

**Step 5: 创建测试类 `EsProductSearchApplicationTests.java`**

```java
package com.example.esproductsearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EsProductSearchApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

这段代码的作用：

- 这是最基础的上下文启动测试
- 它能帮你确认 Spring 容器最起码能启动起来

---

### Task 2: 创建公共返回对象、ES 文档模型和查询 DTO

**Files:**
- Create: `src/main/java/com/example/esproductsearch/common/Result.java`
- Create: `src/main/java/com/example/esproductsearch/document/ProductDocument.java`
- Create: `src/main/java/com/example/esproductsearch/dto/ProductCreateRequest.java`
- Create: `src/main/java/com/example/esproductsearch/dto/ProductSearchRequest.java`
- Create: `src/main/java/com/example/esproductsearch/dto/ProductSearchResponse.java`
- Create: `src/main/java/com/example/esproductsearch/dto/ProductView.java`
- Create: `src/main/java/com/example/esproductsearch/repository/ProductRepository.java`

**Step 1: 创建 `Result.java`**

```java
package com.example.esproductsearch.common;

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

- 统一接口返回格式，避免每个接口都各自返回不同结构
- 后面所有 Controller 都可以直接复用这个返回对象

**Step 2: 创建 `ProductDocument.java`**

```java
package com.example.esproductsearch.document;

import java.math.BigDecimal;
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
@Document(indexName = "product_index")
public class ProductDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String subTitle;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String brand;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Boolean)
    private Boolean onSale;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private LocalDateTime createTime;
}
```

这段代码的作用：

- 用 `@Document` 声明这个类对应 ES 里的 `product_index`
- `Text` 字段用于全文检索，`Keyword` 字段用于精确过滤
- 这个文档类就是 ES 索引里一条商品记录的 Java 映射

**Step 3: 创建 `ProductCreateRequest.java`**

```java
package com.example.esproductsearch.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductCreateRequest {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String subTitle;

    private String description;

    @NotBlank(message = "分类不能为空")
    private String category;

    @NotBlank(message = "品牌不能为空")
    private String brand;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "价格必须大于 0")
    private java.math.BigDecimal price;

    @NotNull(message = "库存不能为空")
    private Integer stock;

    @NotNull(message = "上架状态不能为空")
    private Boolean onSale;
}
```

这段代码的作用：

- 定义“新增商品文档”接口的入参
- 用校验注解拦截明显错误的数据，避免脏数据写进 ES

**Step 4: 创建 `ProductSearchRequest.java`**

```java
package com.example.esproductsearch.dto;

import lombok.Data;

@Data
public class ProductSearchRequest {

    private String keyword;
    private String category;
    private String brand;
    private java.math.BigDecimal minPrice;
    private java.math.BigDecimal maxPrice;
    private Boolean onSale;
    private String sortField = "createTime";
    private String sortDirection = "desc";
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
```

这段代码的作用：

- 把商品搜索里最常见的查询条件都收敛到一个请求对象里
- 默认给排序和分页参数兜底，减少空值判断

**Step 5: 创建 `ProductView.java`**

```java
package com.example.esproductsearch.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductView {

    private String id;
    private String name;
    private String subTitle;
    private String description;
    private String category;
    private String brand;
    private BigDecimal price;
    private Integer stock;
    private Boolean onSale;
    private LocalDateTime createTime;
}
```

这段代码的作用：

- 这是返回给前端看的商品视图对象
- 它和索引里的完整文档可以不完全一样，便于以后做返回裁剪

**Step 6: 创建 `ProductSearchResponse.java`**

```java
package com.example.esproductsearch.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductSearchResponse {

    private long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<ProductView> items;
}
```

这段代码的作用：

- 把分页查询结果统一封装起来
- `total` 表示总命中数，`items` 表示当前页数据

**Step 7: 创建 `ProductRepository.java`**

```java
package com.example.esproductsearch.repository;

import com.example.esproductsearch.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductRepository extends ElasticsearchRepository<ProductDocument, String> {
}
```

这段代码的作用：

- 让 Spring Data 自动帮你生成最基础的 `save`、`findById`、`saveAll` 能力
- 这类简单写入操作直接用 Repository 就够了

---

### Task 3: 创建 Service 并实现 ES 查询逻辑

**Files:**
- Create: `src/main/java/com/example/esproductsearch/service/ProductService.java`
- Create: `src/main/java/com/example/esproductsearch/service/impl/ProductServiceImpl.java`

**Step 1: 创建 `ProductService.java`**

```java
package com.example.esproductsearch.service;

import com.example.esproductsearch.document.ProductDocument;
import com.example.esproductsearch.dto.ProductCreateRequest;
import com.example.esproductsearch.dto.ProductSearchRequest;
import com.example.esproductsearch.dto.ProductSearchResponse;

public interface ProductService {

    void resetAndLoadDemoData();

    ProductDocument save(ProductCreateRequest request);

    ProductSearchResponse search(ProductSearchRequest request);

    ProductDocument getById(String id);
}
```

这段代码的作用：

- 先把业务能力定义清楚，再让实现类去落地
- 你可以很清楚看到这个项目只做 4 件事：初始化、保存、搜索、详情查询

**Step 2: 创建 `ProductServiceImpl.java`**

```java
package com.example.esproductsearch.service.impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.example.esproductsearch.document.ProductDocument;
import com.example.esproductsearch.dto.ProductCreateRequest;
import com.example.esproductsearch.dto.ProductSearchRequest;
import com.example.esproductsearch.dto.ProductSearchResponse;
import com.example.esproductsearch.dto.ProductView;
import com.example.esproductsearch.repository.ProductRepository;
import com.example.esproductsearch.service.ProductService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
public class ProductServiceImpl implements ProductService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("price", "createTime");

    private final ProductRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void resetAndLoadDemoData() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(ProductDocument.class);
        if (indexOperations.exists()) {
            indexOperations.delete();
        }
        indexOperations.create();
        indexOperations.putMapping(indexOperations.createMapping(ProductDocument.class));

        List<ProductDocument> demoProducts = List.of(
            ProductDocument.builder()
                .id(UUID.randomUUID().toString())
                .name("iPhone 15 Pro")
                .subTitle("Flagship smartphone")
                .description("Titanium body and pro camera system")
                .category("phone")
                .brand("Apple")
                .price(new java.math.BigDecimal("7999"))
                .stock(120)
                .onSale(true)
                .createTime(LocalDateTime.now().minusDays(10))
                .build(),
            ProductDocument.builder()
                .id(UUID.randomUUID().toString())
                .name("MateBook X Pro")
                .subTitle("Thin office laptop")
                .description("Portable laptop for mobile office work")
                .category("laptop")
                .brand("Huawei")
                .price(new java.math.BigDecimal("9999"))
                .stock(36)
                .onSale(true)
                .createTime(LocalDateTime.now().minusDays(7))
                .build(),
            ProductDocument.builder()
                .id(UUID.randomUUID().toString())
                .name("Mechanical Keyboard K87")
                .subTitle("RGB gaming keyboard")
                .description("Hot swap mechanical keyboard with RGB backlight")
                .category("keyboard")
                .brand("Keycool")
                .price(new java.math.BigDecimal("499"))
                .stock(260)
                .onSale(true)
                .createTime(LocalDateTime.now().minusDays(2))
                .build()
        );

        productRepository.saveAll(demoProducts);
    }

    @Override
    public ProductDocument save(ProductCreateRequest request) {
        ProductDocument document = ProductDocument.builder()
            .id(UUID.randomUUID().toString())
            .name(request.getName())
            .subTitle(request.getSubTitle())
            .description(request.getDescription())
            .category(request.getCategory())
            .brand(request.getBrand())
            .price(request.getPrice())
            .stock(request.getStock())
            .onSale(request.getOnSale())
            .createTime(LocalDateTime.now())
            .build();

        return productRepository.save(document);
    }

    @Override
    public ProductSearchResponse search(ProductSearchRequest request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        boolean hasCondition = false;

        if (StringUtils.hasText(request.getKeyword())) {
            boolBuilder.must(must -> must.multiMatch(multi -> multi
                .query(request.getKeyword())
                .fields("name", "subTitle", "description")
            ));
            hasCondition = true;
        }

        if (StringUtils.hasText(request.getCategory())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("category").value(request.getCategory())));
            hasCondition = true;
        }

        if (StringUtils.hasText(request.getBrand())) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("brand").value(request.getBrand())));
            hasCondition = true;
        }

        if (request.getOnSale() != null) {
            boolBuilder.filter(filter -> filter.term(term -> term.field("onSale").value(request.getOnSale())));
            hasCondition = true;
        }

        if (request.getMinPrice() != null) {
            boolBuilder.filter(filter -> filter.range(range -> range.number(number -> number
                .field("price")
                .gte(JsonData.of(request.getMinPrice()))
            )));
            hasCondition = true;
        }

        if (request.getMaxPrice() != null) {
            boolBuilder.filter(filter -> filter.range(range -> range.number(number -> number
                .field("price")
                .lte(JsonData.of(request.getMaxPrice()))
            )));
            hasCondition = true;
        }

        String sortField = ALLOWED_SORT_FIELDS.contains(request.getSortField()) ? request.getSortField() : "createTime";
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
            .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(nativeQuery, ProductDocument.class);
        List<ProductView> items = new ArrayList<>();

        for (SearchHit<ProductDocument> hit : hits) {
            ProductDocument product = hit.getContent();
            items.add(ProductView.builder()
                .id(product.getId())
                .name(product.getName())
                .subTitle(product.getSubTitle())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .price(product.getPrice())
                .stock(product.getStock())
                .onSale(product.getOnSale())
                .createTime(product.getCreateTime())
                .build());
        }

        return ProductSearchResponse.builder()
            .total(hits.getTotalHits())
            .pageNum(pageNum)
            .pageSize(pageSize)
            .items(items)
            .build();
    }

    @Override
    public ProductDocument getById(String id) {
        return productRepository.findById(id).orElse(null);
    }
}
```

这段代码的作用：

- `resetAndLoadDemoData` 负责重建索引并写入演示商品，方便你反复练习
- `save` 负责把一个新商品文档写入 ES
- `search` 是核心：把关键字、分类、品牌、价格区间、上架状态组合成一个布尔查询
- `ElasticsearchOperations` 负责执行复杂查询，`Repository` 负责简单写入和按 id 查询

---

### Task 4: 创建 Controller，对外暴露接口

**Files:**
- Create: `src/main/java/com/example/esproductsearch/controller/ProductController.java`

**Step 1: 创建 `ProductController.java`**

```java
package com.example.esproductsearch.controller;

import com.example.esproductsearch.common.Result;
import com.example.esproductsearch.document.ProductDocument;
import com.example.esproductsearch.dto.ProductCreateRequest;
import com.example.esproductsearch.dto.ProductSearchRequest;
import com.example.esproductsearch.dto.ProductSearchResponse;
import com.example.esproductsearch.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/init-demo")
    public Result<String> initDemo() {
        productService.resetAndLoadDemoData();
        return Result.success("商品索引已重建并写入演示数据");
    }

    @PostMapping
    public Result<ProductDocument> create(@Valid @RequestBody ProductCreateRequest request) {
        return Result.success(productService.save(request));
    }

    @PostMapping("/search")
    public Result<ProductSearchResponse> search(@RequestBody ProductSearchRequest request) {
        return Result.success(productService.search(request));
    }

    @GetMapping("/{id}")
    public Result<ProductDocument> getById(@PathVariable String id) {
        return Result.success(productService.getById(id));
    }
}
```

这段代码的作用：

- 暴露 4 个最核心接口：初始化演示数据、创建商品、搜索商品、查看详情
- 这样你能把“写入文档”和“查询文档”两条主链路都完整练到

---

## 四、启动顺序

**Step 1: 启动 Elasticsearch**

```bash
docker compose up -d
```

这段命令的作用：

- 在当前项目目录里启动本项目专属的 ES 容器
- 它不会依赖其他练习项目的容器

**Step 2: 启动 Spring Boot**

```bash
mvn spring-boot:run
```

这段命令的作用：

- 启动当前商品搜索应用
- 启动后应用会监听 `8141` 端口

---

## 五、接口测试顺序

**Step 1: 初始化演示数据**

```bash
curl -X POST http://localhost:8141/products/init-demo
```

这段命令的作用：

- 重建 `product_index`
- 写入 3 条演示商品

**Step 2: 搜索 laptop 类商品**

```bash
curl -X POST http://localhost:8141/products/search ^
  -H "Content-Type: application/json" ^
  -d "{\"keyword\":\"laptop\",\"pageNum\":1,\"pageSize\":10}"
```

这段命令的作用：

- 演示最基础的关键字搜索
- 你会看到 `MateBook X Pro` 被搜索出来

**Step 3: 搜索 phone 分类且价格低于 9000 的商品**

```bash
curl -X POST http://localhost:8141/products/search ^
  -H "Content-Type: application/json" ^
  -d "{\"category\":\"phone\",\"maxPrice\":9000,\"sortField\":\"price\",\"sortDirection\":\"asc\"}"
```

这段命令的作用：

- 演示分类过滤 + 价格范围过滤 + 排序的组合查询

---

## 六、常见报错排查

### 1. `Connection refused: localhost/127.0.0.1:9201`

原因：

- Elasticsearch 容器没有启动成功

排查命令：

```bash
docker compose logs -f elasticsearch
```

这段命令的作用：

- 查看 ES 容器启动日志
- 能快速判断是不是内存不足或者容器还没完全启动

### 2. 搜索结果为空

原因：

- 你还没有先调用 `/products/init-demo`
- 或者你用了中文关键词，但当前文档没有安装中文分词插件

### 3. 排序报错

原因：

- 你传了不在白名单里的排序字段

说明：

- 当前示例只允许按 `price` 或 `createTime` 排序
- 这是为了避免新手把 `Text` 字段直接拿去排序而报错

---

## 七、你在这个项目里学到了什么

做完这个项目后，你应该已经掌握：

- ES 索引和文档的基本概念
- `Text` 和 `Keyword` 字段的区别
- 如何使用 Spring Data Elasticsearch 写入文档
- 如何使用 `BoolQuery` 组合关键词搜索和精确过滤
- 商品搜索为什么非常适合落在 Elasticsearch 上
