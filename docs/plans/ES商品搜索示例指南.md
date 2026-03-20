# 商品搜索 Elasticsearch 项目实战文档

## 1. 项目目标

你要新建一个 Spring Boot 项目，项目名叫 `es-product-search-demo`。

这个项目模拟的是生产环境里最常见的 Elasticsearch 场景之一：商品搜索。

用户会输入关键词，然后再结合下面这些条件搜索商品：

- 分类
- 品牌
- 价格区间
- 是否上架
- 排序方式
- 分页参数

这个项目会帮助你练会这些核心能力：

- 全文检索
- 精确过滤
- 范围查询
- 排序
- 分页
- 高亮显示

## 2. 最终目录结构

先看最终结构，这样你后面创建文件时不容易迷路：

```text
es-product-search-demo
├─ docker
│  └─ docker-compose.yml
├─ src
│  ├─ main
│  │  ├─ java/com/example/productsearch
│  │  │  ├─ config
│  │  │  │  └─ ElasticsearchConfig.java
│  │  │  ├─ controller
│  │  │  │  └─ ProductController.java
│  │  │  ├─ document
│  │  │  │  └─ ProductDocument.java
│  │  │  ├─ dto
│  │  │  │  ├─ ApiResponse.java
│  │  │  │  ├─ ProductSearchRequest.java
│  │  │  │  ├─ ProductSearchResponse.java
│  │  │  │  └─ ProductView.java
│  │  │  ├─ repository
│  │  │  │  └─ ProductRepository.java
│  │  │  ├─ service
│  │  │  │  ├─ ProductService.java
│  │  │  │  └─ impl
│  │  │  │     └─ ProductServiceImpl.java
│  │  │  └─ EsProductSearchDemoApplication.java
│  │  └─ resources
│  │     └─ application.yml
│  └─ test
│     └─ java/com/example/productsearch
│        └─ ProductSearchApplicationTests.java
├─ pom.xml
└─ README.md
```

## 3. 第一步：创建 Spring Boot 工程

新建一个 Maven 工程，推荐参数如下：

- `GroupId`：`com.example`
- `ArtifactId`：`es-product-search-demo`
- `Package name`：`com.example.productsearch`
- `Java`：`17`

依赖选择：

- Spring Web
- Spring Data Elasticsearch
- Validation
- Lombok
- Spring Boot Test

## 4. 第二步：创建 `pom.xml`

创建文件 [pom.xml](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\pom.xml)。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 父工程，帮我们统一管理大部分依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.6</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>es-product-search-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>es-product-search-demo</name>
    <description>商品搜索 Elasticsearch 练习项目</description>

    <properties>
        <!-- 使用 Java 17，这是 Spring Boot 3 常见且稳定的选择 -->
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- 提供 Web 接口能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 提供 Spring Data Elasticsearch 能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
        </dependency>

        <!-- 用于请求参数校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- 用于简化 getter、setter、构造器等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 单元测试和启动测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- 让我们可以直接使用 mvn spring-boot:run 启动项目 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 这段配置在做什么

- `spring-boot-starter-web`：让你能写 Controller，对外暴露 HTTP 接口
- `spring-boot-starter-data-elasticsearch`：这是和 ES 打交道的核心依赖
- `validation`：让你能校验分页参数之类的输入
- `lombok`：减少 Java 样板代码

## 5. 第三步：用 Docker 启动 Elasticsearch 和 Kibana

创建文件 [docker/docker-compose.yml](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\docker\docker-compose.yml)。

```yaml
version: "3.8"

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.15.3
    container_name: es-product-search
    environment:
      # 使用单机模式，适合本地学习和调试
      discovery.type: single-node
      # 关闭安全认证，降低初学阶段配置复杂度
      xpack.security.enabled: "false"
      # 限制 JVM 内存，避免本机资源占用过大
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"
    ports:
      # 把宿主机 9200 端口映射到容器 9200 端口
      - "9200:9200"
    volumes:
      # 把 ES 数据挂载到 Docker volume，避免容器重启后数据完全丢失
      - es_product_data:/usr/share/elasticsearch/data

  kibana:
    image: docker.elastic.co/kibana/kibana:8.15.3
    container_name: kibana-product-search
    environment:
      # 告诉 Kibana 去连接哪个 Elasticsearch 地址
      ELASTICSEARCH_HOSTS: '["http://elasticsearch:9200"]'
    depends_on:
      - elasticsearch
    ports:
      # 访问 http://localhost:5601 就能打开 Kibana
      - "5601:5601"

volumes:
  es_product_data:
```

### 启动命令

进入 `es-product-search-demo` 目录后执行：

```bash
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.yml ps
```

### 如何判断启动成功

你可以直接访问：

- `http://localhost:9200`

如果返回一段 JSON，说明 Elasticsearch 启动成功了。

## 6. 第四步：创建 `application.yml`

创建文件 [application.yml](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\src\main\resources\application.yml)。

```yaml
server:
  port: 8080

spring:
  application:
    name: es-product-search-demo
  elasticsearch:
    uris: http://localhost:9200

logging:
  level:
    root: info
    org.springframework.data.elasticsearch.client.WIRE: trace
```

### 配置解释

- `server.port`：当前 Spring Boot 项目的启动端口
- `spring.elasticsearch.uris`：ES 服务地址
- `WIRE: trace`：打印 ES 请求细节，方便你学习和排错

## 7. 第五步：创建启动类

创建文件 [EsProductSearchDemoApplication.java](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\src\main\java\com\example\productsearch\EsProductSearchDemoApplication.java)。

```java
package com.example.productsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 商品搜索项目启动类。
 * 这是整个 Spring Boot 项目的入口。
 */
@SpringBootApplication
public class EsProductSearchDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsProductSearchDemoApplication.class, args);
    }
}
```

## 8. 第六步：创建索引实体类

创建文件 [ProductDocument.java](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\src\main\java\com\example\productsearch\document\ProductDocument.java)。

```java
package com.example.productsearch.document;

import java.math.BigDecimal;
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
 * ProductDocument 表示 Elasticsearch 中的一条商品文档。
 * 一个 Java 对象，最终会对应 ES 里的一条 JSON 数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products")
public class ProductDocument {

    @Id
    private String id;

    // 商品名称，通常是用户搜索时最重要的命中字段
    @Field(type = FieldType.Text)
    private String name;

    // 商品副标题，也经常参与搜索
    @Field(type = FieldType.Text)
    private String subTitle;

    // 商品详情描述，适合做全文检索
    @Field(type = FieldType.Text)
    private String description;

    // 分类是精确值，适合使用 Keyword 类型
    @Field(type = FieldType.Keyword)
    private String category;

    // 品牌通常用于精确过滤，所以也是 Keyword
    @Field(type = FieldType.Keyword)
    private String brand;

    // 价格需要做区间过滤和排序
    @Field(type = FieldType.Double)
    private BigDecimal price;

    // 库存数量，用于展示商品当前库存情况
    @Field(type = FieldType.Integer)
    private Integer stock;

    // 是否上架，用于过滤商品状态
    @Field(type = FieldType.Boolean)
    private Boolean onSale;

    // 商品标签，用于展示或简单过滤
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    // 创建时间，用于按时间排序
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createTime;
}
```

### 为什么字段类型要这样设计

- `Text`：适合全文搜索，比如商品名、商品描述
- `Keyword`：适合精确过滤，比如品牌、分类
- `Double`：适合价格区间查询和排序
- `Date`：适合时间排序

## 9. 第七步：创建 Repository

创建文件 [ProductRepository.java](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\src\main\java\com\example\productsearch\repository\ProductRepository.java)。

```java
package com.example.productsearch.repository;

import com.example.productsearch.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * ProductRepository 提供最基础的 ES CRUD 能力。
 * 比如根据 id 查询、保存文档、批量保存等。
 */
public interface ProductRepository extends ElasticsearchRepository<ProductDocument, String> {
}
```

## 10. 第八步：创建统一返回对象

创建文件 [ApiResponse.java](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\src\main\java\com\example\productsearch\dto\ApiResponse.java)。

```java
package com.example.productsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回结构。
 * 这样前端或者调用方看到的 JSON 风格会比较统一。
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

## 11. 第九步：创建搜索请求对象

创建文件 [ProductSearchRequest.java](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\src\main\java\com\example\productsearch\dto\ProductSearchRequest.java)。

```java
package com.example.productsearch.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * ProductSearchRequest 用来接收用户传入的搜索参数。
 */
@Data
public class ProductSearchRequest {

    // 搜索关键词，会作用到 name、subTitle、description 这些字段上
    private String keyword;

    // 商品分类精确过滤
    private String category;

    // 品牌精确过滤
    private String brand;

    // 最低价格
    private BigDecimal minPrice;

    // 最高价格
    private BigDecimal maxPrice;

    // 是否上架
    private Boolean onSale;

    // 页码从 0 开始
    @Min(0)
    private Integer page = 0;

    // 每页条数
    @Min(1)
    private Integer size = 10;

    // 排序字段，练习项目里支持 price 和 createTime
    private String sortBy = "createTime";

    // 排序方向，支持 asc 或 desc
    private String sortDirection = "desc";
}
```

## 12. 第十步：创建响应对象

先创建 [ProductView.java](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\src\main\java\com\example\productsearch\dto\ProductView.java)。

```java
package com.example.productsearch.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * ProductView 是返回给调用方看的商品对象。
 * 为什么不直接返回 ProductDocument？
 * 因为后面我们可能会把高亮后的名称放进来，和原始文档做一点区分会更清晰。
 */
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
    private List<String> tags;
    private LocalDateTime createTime;
}
```

再创建 [ProductSearchResponse.java](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\src\main\java\com\example\productsearch\dto\ProductSearchResponse.java)。

```java
package com.example.productsearch.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * 商品搜索分页返回对象。
 */
@Data
@Builder
public class ProductSearchResponse {

    private long total;
    private int page;
    private int size;
    private List<ProductView> items;
}
```

## 13. 第十一步：创建 Service 接口

创建文件 [ProductService.java](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\src\main\java\com\example\productsearch\service\ProductService.java)。

```java
package com.example.productsearch.service;

import com.example.productsearch.document.ProductDocument;
import com.example.productsearch.dto.ProductSearchRequest;
import com.example.productsearch.dto.ProductSearchResponse;

/**
 * 商品搜索业务接口。
 */
public interface ProductService {

    // 初始化索引
    void initIndex();

    // 导入演示数据
    void loadDemoData();

    // 按条件搜索商品
    ProductSearchResponse search(ProductSearchRequest request);

    // 根据 id 查询单个商品
    ProductDocument getById(String id);
}
```

## 14. 第十二步：创建配置类

创建文件 [ElasticsearchConfig.java](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\src\main\java\com\example\productsearch\config\ElasticsearchConfig.java)。

```java
package com.example.productsearch.config;

import org.springframework.context.annotation.Configuration;

/**
 * 这里先保留一个配置类。
 * 当前项目主要依赖 Spring Boot 的自动配置，后续如果你想扩展自定义 ES 配置，
 * 可以直接从这里开始加。
 */
@Configuration
public class ElasticsearchConfig {
}
```

## 15. 第十三步：创建 Service 实现类

创建文件 [ProductServiceImpl.java](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\src\main\java\com\example\productsearch\service\impl\ProductServiceImpl.java)。

```java
package com.example.productsearch.service.impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import com.example.productsearch.document.ProductDocument;
import com.example.productsearch.dto.ProductSearchRequest;
import com.example.productsearch.dto.ProductSearchResponse;
import com.example.productsearch.dto.ProductView;
import com.example.productsearch.repository.ProductRepository;
import com.example.productsearch.service.ProductService;
import java.math.BigDecimal;
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
 * ProductServiceImpl 是真正执行 ES 查询逻辑的地方。
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void initIndex() {
        // 获取当前文档对应的索引操作对象
        IndexOperations indexOperations = elasticsearchOperations.indexOps(ProductDocument.class);

        // 如果索引已经存在，先删掉，保证你每次练习都能得到干净结果
        if (indexOperations.exists()) {
            indexOperations.delete();
        }

        // 创建索引
        indexOperations.create();

        // 根据 ProductDocument 上的注解自动生成 mapping
        indexOperations.putMapping(indexOperations.createMapping(ProductDocument.class));
    }

    @Override
    public void loadDemoData() {
        List<ProductDocument> demoProducts = List.of(
            ProductDocument.builder()
                .id("1")
                .name("Apple iPhone 15 Pro")
                .subTitle("高端旗舰手机")
                .description("钛金属机身，强劲芯片，专业影像系统。")
                .category("phone")
                .brand("Apple")
                .price(new BigDecimal("7999"))
                .stock(120)
                .onSale(true)
                .tags(List.of("smartphone", "ios", "flagship"))
                .createTime(LocalDateTime.now().minusDays(10))
                .build(),
            ProductDocument.builder()
                .id("2")
                .name("Huawei MateBook X Pro")
                .subTitle("轻薄办公笔记本")
                .description("高分辨率屏幕，机身轻薄，适合移动办公。")
                .category("laptop")
                .brand("Huawei")
                .price(new BigDecimal("9999"))
                .stock(35)
                .onSale(true)
                .tags(List.of("laptop", "office", "ultrabook"))
                .createTime(LocalDateTime.now().minusDays(7))
                .build(),
            ProductDocument.builder()
                .id("3")
                .name("Xiaomi Smart Band 9")
                .subTitle("轻量运动手环")
                .description("支持运动记录、睡眠监测和心率检测。")
                .category("wearable")
                .brand("Xiaomi")
                .price(new BigDecimal("299"))
                .stock(500)
                .onSale(true)
                .tags(List.of("wearable", "health", "fitness"))
                .createTime(LocalDateTime.now().minusDays(2))
                .build()
        );

        // 批量写入演示数据
        productRepository.saveAll(demoProducts);
    }

    @Override
    public ProductSearchResponse search(ProductSearchRequest request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 如果用户传了关键词，就对商品名称、副标题、描述做多字段搜索
        if (StringUtils.hasText(request.getKeyword())) {
            boolBuilder.must(must -> must.multiMatch(multiMatch -> multiMatch
                .query(request.getKeyword())
                .fields("name", "subTitle", "description")
            ));
        }

        // 分类精确过滤
        if (StringUtils.hasText(request.getCategory())) {
            boolBuilder.filter(filter -> filter.term(term -> term
                .field("category")
                .value(request.getCategory())
            ));
        }

        // 品牌精确过滤
        if (StringUtils.hasText(request.getBrand())) {
            boolBuilder.filter(filter -> filter.term(term -> term
                .field("brand")
                .value(request.getBrand())
            ));
        }

        // 是否上架过滤
        if (request.getOnSale() != null) {
            boolBuilder.filter(filter -> filter.term(term -> term
                .field("onSale")
                .value(request.getOnSale())
            ));
        }

        // 最低价格过滤
        if (request.getMinPrice() != null) {
            boolBuilder.filter(filter -> filter.range(range -> range
                .number(number -> number
                    .field("price")
                    .gte(JsonData.of(request.getMinPrice()))
                )
            ));
        }

        // 最高价格过滤
        if (request.getMaxPrice() != null) {
            boolBuilder.filter(filter -> filter.range(range -> range
                .number(number -> number
                    .field("price")
                    .lte(JsonData.of(request.getMaxPrice()))
                )
            ));
        }

        // 把 BoolQuery 转成真正的查询对象
        Query query = boolBuilder.build()._toQuery();

        NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(query)
            // 设置分页
            .withPageable(PageRequest.of(request.getPage(), request.getSize()))
            // 设置排序
            .withSort(sort -> sort.field(field -> field
                .field(request.getSortBy())
                .order("asc".equalsIgnoreCase(request.getSortDirection()) ? SortOrder.Asc : SortOrder.Desc)
            ))
            // 给 name 字段加高亮
            .withHighlightQuery(new HighlightQuery(
                List.of(new HighlightField("name")),
                HighlightParameters.builder()
                    .withPreTags("<em>")
                    .withPostTags("</em>")
                    .build()
            ))
            .build();

        SearchHits<ProductDocument> searchHits =
            elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        List<ProductView> items = new ArrayList<>();

        for (SearchHit<ProductDocument> hit : searchHits) {
            ProductDocument product = hit.getContent();

            // 默认先用原始商品名
            String highlightedName = product.getName();

            // 如果 ES 返回了高亮字段，就优先使用高亮后的名称
            Map<String, List<String>> highlightFields = hit.getHighlightFields();
            if (!CollectionUtils.isEmpty(highlightFields.get("name"))) {
                highlightedName = highlightFields.get("name").get(0);
            }

            items.add(ProductView.builder()
                .id(product.getId())
                .name(highlightedName)
                .subTitle(product.getSubTitle())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .price(product.getPrice())
                .stock(product.getStock())
                .onSale(product.getOnSale())
                .tags(product.getTags())
                .createTime(product.getCreateTime())
                .build());
        }

        return ProductSearchResponse.builder()
            .total(searchHits.getTotalHits())
            .page(request.getPage())
            .size(request.getSize())
            .items(items)
            .build();
    }

    @Override
    public ProductDocument getById(String id) {
        return productRepository.findById(id).orElse(null);
    }
}
```

### 这段核心代码你要重点理解什么

1. `BoolQuery` 是组合查询的核心
2. `must` 用来做关键词搜索
3. `filter` 用来做分类、品牌、价格区间、上架状态过滤
4. `withSort` 用来做排序
5. `withPageable` 用来做分页
6. `withHighlightQuery` 用来做高亮

## 16. 第十四步：创建 Controller

创建文件 [ProductController.java](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\src\main\java\com\example\productsearch\controller\ProductController.java)。

```java
package com.example.productsearch.controller;

import com.example.productsearch.dto.ApiResponse;
import com.example.productsearch.dto.ProductSearchRequest;
import com.example.productsearch.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品搜索模块对外暴露的 HTTP 接口。
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/init")
    public ApiResponse<String> initIndex() {
        productService.initIndex();
        return ApiResponse.success("商品索引初始化成功");
    }

    @PostMapping("/load-demo-data")
    public ApiResponse<String> loadDemoData() {
        productService.loadDemoData();
        return ApiResponse.success("商品演示数据导入成功");
    }

    @GetMapping("/search")
    public ApiResponse<?> search(@Valid @ModelAttribute ProductSearchRequest request) {
        return ApiResponse.success(productService.search(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(productService.getById(id));
    }
}
```

## 17. 第十五步：创建测试类

创建文件 [ProductSearchApplicationTests.java](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\src\test\java\com\example\productsearch\ProductSearchApplicationTests.java)。

```java
package com.example.productsearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 最基础的启动测试。
 * 它的作用是验证 Spring 容器能不能正常启动。
 */
@SpringBootTest
class ProductSearchApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

## 18. 第十六步：创建 `README.md`

创建文件 [README.md](D:\Gitee\demo\mysql-redis-mq-demo\es-product-search-demo\README.md)。

````md
# es-product-search-demo

## 启动 Elasticsearch

```bash
docker compose -f docker/docker-compose.yml up -d
```

## 启动项目

```bash
mvn spring-boot:run
```

## 初始化索引

`POST http://localhost:8080/api/products/init`

## 导入演示数据

`POST http://localhost:8080/api/products/load-demo-data`

## 搜索示例

`GET http://localhost:8080/api/products/search?keyword=phone&page=0&size=10`
````

## 19. 第十七步：启动项目

在 `es-product-search-demo` 目录下执行：

```bash
mvn clean package
mvn spring-boot:run
```

如果启动成功，控制台会显示项目运行在 `8080` 端口。

## 20. 第十八步：初始化索引

调用：

```http
POST http://localhost:8080/api/products/init
```

预期返回：

```json
{
  "code": 200,
  "message": "success",
  "data": "商品索引初始化成功"
}
```

## 21. 第十九步：导入演示数据

调用：

```http
POST http://localhost:8080/api/products/load-demo-data
```

## 22. 第二十步：验证搜索接口

示例请求：

```http
GET http://localhost:8080/api/products/search?keyword=phone&category=phone&page=0&size=10
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
        "id": "1",
        "name": "Apple i<em>Phone</em> 15 Pro",
        "subTitle": "高端旗舰手机",
        "description": "钛金属机身，强劲芯片，专业影像系统。",
        "category": "phone",
        "brand": "Apple",
        "price": 7999,
        "stock": 120,
        "onSale": true,
        "tags": ["smartphone", "ios", "flagship"],
        "createTime": "2026-03-01T10:00:00"
      }
    ]
  }
}
```

## 23. 常见问题排查

### 问题 1：启动时报 `Connection refused`

常见原因：

- ES 容器没有启动
- `application.yml` 里的端口写错了

排查方式：

```bash
docker compose -f docker/docker-compose.yml ps
```

然后再访问：

- `http://localhost:9200`

### 问题 2：搜索结果为空

常见原因：

- 没有先初始化索引
- 没有导入演示数据
- 关键字和当前数据不匹配

正确顺序：

1. 先调 `/api/products/init`
2. 再调 `/api/products/load-demo-data`
3. 再调搜索接口

### 问题 3：没有看到高亮

常见原因：

- 当前示例只对 `name` 字段做了高亮
- 搜索词可能命中了别的字段，而不是商品名

## 24. 你学会了什么

做完这个项目后，你应该已经理解了：

- Spring Boot 怎么连接 Elasticsearch
- ES 索引实体类应该怎么设计
- 搜索参数是怎么一步步变成 BoolQuery 的
- 过滤、排序、分页是怎么组合起来的
- 搜索高亮结果是怎么返回给前端的
