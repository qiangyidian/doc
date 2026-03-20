# MySQL Redis MQ Demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a minimal runnable Spring Boot 3 demo that reads product data through Redis cache-aside and updates product price by writing MySQL first, then deleting Redis asynchronously through RabbitMQ with retry-on-failure behavior.

**Architecture:** The project is a single Spring Boot application with REST controllers, a MyBatis-Plus mapper, a service layer, and RabbitMQ producer/consumer components. MySQL, Redis, and RabbitMQ run in Docker Compose; the application stores product data in MySQL, caches products in Redis, and uses RabbitMQ manual acknowledgments to retry cache deletion when the consumer fails.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Web, Spring Data Redis, Spring AMQP, RabbitMQ, MyBatis-Plus, MySQL 8, Lombok, JUnit 5, Mockito, Spring Boot Test

---

### Task 1: Bootstrap The Project Skeleton

**Files:**
- Create: `pom.xml`
- Create: `docker-compose.yml`
- Create: `src/main/java/com/example/demo/DemoApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `src/main/resources/data.sql`
- Test: `src/test/java/com/example/demo/DemoApplicationTests.java`

**Step 1: Write the failing test**

```java
package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.sql.init.mode=never"
})
class DemoApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=DemoApplicationTests test`

Expected: FAIL because `pom.xml` and the Spring Boot application classes do not exist yet.

**Step 3: Write minimal implementation**

Create the Maven project and minimal application files. Use this application class:

```java
package com.example.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.demo.mapper")
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

Create `application.yml` with MySQL, Redis, RabbitMQ, and MyBatis-Plus settings. Configure RabbitMQ with manual acknowledgment mode and disable listener retry. Add `schema.sql` and `data.sql` to create and seed the `product` table. Add `docker-compose.yml` for MySQL, Redis, and RabbitMQ.

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=DemoApplicationTests test`

Expected: PASS with `Tests run: 1, Failures: 0, Errors: 0`.

**Step 5: Commit**

```bash
git add pom.xml docker-compose.yml src/main/java/com/example/demo/DemoApplication.java src/main/resources/application.yml src/main/resources/schema.sql src/main/resources/data.sql src/test/java/com/example/demo/DemoApplicationTests.java
git commit -m "build: scaffold spring boot cache demo"
```

### Task 2: Add The Product Domain And Cache-Aside Read Path

**Files:**
- Create: `src/main/java/com/example/demo/entity/Product.java`
- Create: `src/main/java/com/example/demo/mapper/ProductMapper.java`
- Create: `src/main/java/com/example/demo/service/ProductService.java`
- Create: `src/main/java/com/example/demo/service/impl/ProductServiceImpl.java`
- Test: `src/test/java/com/example/demo/service/ProductServiceReadTest.java`

**Step 1: Write the failing test**

```java
package com.example.demo.service;

import com.example.demo.entity.Product;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.service.impl.ProductServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProductServiceReadTest {

    @Test
    void shouldQueryMysqlAndBackfillRedisWhenCacheMiss() throws Exception {
        ProductMapper productMapper = mock(ProductMapper.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("product:1")).thenReturn(null);

        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 15");
        product.setPrice(new BigDecimal("5999.00"));
        when(productMapper.selectById(1L)).thenReturn(product);

        ProductServiceImpl service = new ProductServiceImpl(
                productMapper,
                redisTemplate,
                new ObjectMapper(),
                null
        );

        Product result = service.getByIdWithCache(1L);

        assertThat(result.getId()).isEqualTo(1L);
        verify(valueOperations).set(eq("product:1"), contains("\"id\":1"), eq(30L), eq(TimeUnit.MINUTES));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=ProductServiceReadTest test`

Expected: FAIL because the entity, mapper, service interface, and service implementation do not exist.

**Step 3: Write minimal implementation**

Create the `Product` entity with `id`, `name`, `price`, and `updateTime`. Create `ProductMapper extends BaseMapper<Product>`. Create `ProductService` with `getByIdWithCache(Long id)` and `updatePrice(Long id, BigDecimal newPrice)`. Implement `ProductServiceImpl#getByIdWithCache` with Redis read, MySQL fallback, and Redis backfill:

```java
String cacheKey = PRODUCT_CACHE_KEY_PREFIX + id;
String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
if (cacheValue != null) {
    return objectMapper.readValue(cacheValue, Product.class);
}
Product product = productMapper.selectById(id);
if (product != null) {
    String json = objectMapper.writeValueAsString(product);
    stringRedisTemplate.opsForValue().set(cacheKey, json, 30, TimeUnit.MINUTES);
}
return product;
```

Keep `updatePrice` as a stub that throws `UnsupportedOperationException` for now so the class compiles without implementing the write path yet.

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=ProductServiceReadTest test`

Expected: PASS with `Tests run: 1, Failures: 0, Errors: 0`.

**Step 5: Commit**

```bash
git add src/main/java/com/example/demo/entity/Product.java src/main/java/com/example/demo/mapper/ProductMapper.java src/main/java/com/example/demo/service/ProductService.java src/main/java/com/example/demo/service/impl/ProductServiceImpl.java src/test/java/com/example/demo/service/ProductServiceReadTest.java
git commit -m "feat: add product cache aside read path"
```

### Task 3: Add MQ Message Publishing To The Update Flow

**Files:**
- Create: `src/main/java/com/example/demo/config/RabbitConfig.java`
- Create: `src/main/java/com/example/demo/mq/CacheDeleteMessage.java`
- Create: `src/main/java/com/example/demo/mq/CacheDeleteProducer.java`
- Modify: `src/main/java/com/example/demo/service/impl/ProductServiceImpl.java`
- Test: `src/test/java/com/example/demo/service/ProductServiceUpdateTest.java`

**Step 1: Write the failing test**

```java
package com.example.demo.service;

import com.example.demo.entity.Product;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.mq.CacheDeleteMessage;
import com.example.demo.mq.CacheDeleteProducer;
import com.example.demo.service.impl.ProductServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceUpdateTest {

    @Test
    void shouldUpdateMysqlAndSendCacheDeleteMessage() {
        ProductMapper productMapper = mock(ProductMapper.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        CacheDeleteProducer producer = mock(CacheDeleteProducer.class);

        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 15");
        product.setPrice(new BigDecimal("5999.00"));
        when(productMapper.selectById(1L)).thenReturn(product);

        ProductServiceImpl service = new ProductServiceImpl(
                productMapper,
                redisTemplate,
                new ObjectMapper(),
                producer
        );

        service.updatePrice(1L, new BigDecimal("6999.00"));

        verify(productMapper).updateById(product);
        verify(producer).sendDeleteMessage(argThat(message ->
                message.getBusinessId().equals(1L)
                        && message.getCacheKey().equals("product:1")
                        && message.getType().equals("PRODUCT_CACHE_DELETE")
        ));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=ProductServiceUpdateTest test`

Expected: FAIL because the MQ classes do not exist and `updatePrice` is not implemented.

**Step 3: Write minimal implementation**

Create RabbitMQ exchange, queue, and binding constants in `RabbitConfig`. Create the message DTO and producer. Replace the `updatePrice` stub with a real implementation:

```java
Product product = productMapper.selectById(id);
if (product == null) {
    throw new RuntimeException("商品不存在");
}
product.setPrice(newPrice);
productMapper.updateById(product);

CacheDeleteMessage message = new CacheDeleteMessage();
message.setBusinessId(id);
message.setCacheKey(PRODUCT_CACHE_KEY_PREFIX + id);
message.setType("PRODUCT_CACHE_DELETE");
cacheDeleteProducer.sendDeleteMessage(message);
```

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=ProductServiceUpdateTest test`

Expected: PASS with `Tests run: 1, Failures: 0, Errors: 0`.

**Step 5: Commit**

```bash
git add src/main/java/com/example/demo/config/RabbitConfig.java src/main/java/com/example/demo/mq/CacheDeleteMessage.java src/main/java/com/example/demo/mq/CacheDeleteProducer.java src/main/java/com/example/demo/service/impl/ProductServiceImpl.java src/test/java/com/example/demo/service/ProductServiceUpdateTest.java
git commit -m "feat: publish cache delete messages after db update"
```

### Task 4: Add The RabbitMQ Consumer With Manual Ack/Nack

**Files:**
- Create: `src/main/java/com/example/demo/mq/CacheDeleteConsumer.java`
- Test: `src/test/java/com/example/demo/mq/CacheDeleteConsumerTest.java`

**Step 1: Write the failing test**

```java
package com.example.demo.mq;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.*;

class CacheDeleteConsumerTest {

    @AfterEach
    void resetFlag() {
        CacheDeleteConsumer.setMockDeleteFail(false);
    }

    @Test
    void shouldAckWhenRedisDeleteSucceeds() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.delete("product:1")).thenReturn(Boolean.TRUE);
        CacheDeleteConsumer consumer = new CacheDeleteConsumer(redisTemplate);
        Channel channel = mock(Channel.class);

        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(7L);
        Message message = new Message(new byte[0], properties);

        CacheDeleteMessage payload = new CacheDeleteMessage();
        payload.setCacheKey("product:1");

        consumer.onMessage(payload, message, channel);

        verify(channel).basicAck(7L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void shouldNackAndRequeueWhenRedisDeleteFails() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        CacheDeleteConsumer consumer = new CacheDeleteConsumer(redisTemplate);
        CacheDeleteConsumer.setMockDeleteFail(true);
        Channel channel = mock(Channel.class);

        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(8L);
        Message message = new Message(new byte[0], properties);

        CacheDeleteMessage payload = new CacheDeleteMessage();
        payload.setCacheKey("product:1");

        consumer.onMessage(payload, message, channel);

        verify(channel).basicNack(8L, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=CacheDeleteConsumerTest test`

Expected: FAIL because the consumer does not exist.

**Step 3: Write minimal implementation**

Implement the consumer with a static failure toggle and Rabbit listener:

```java
@RabbitListener(queues = RabbitConfig.CACHE_DELETE_QUEUE)
public void onMessage(@Payload CacheDeleteMessage message,
                      org.springframework.amqp.core.Message amqpMessage,
                      Channel channel) throws Exception {
    long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
    try {
        if (MOCK_DELETE_FAIL) {
            throw new RuntimeException("模拟删除 Redis 失败");
        }
        stringRedisTemplate.delete(message.getCacheKey());
        channel.basicAck(deliveryTag, false);
    } catch (Exception e) {
        channel.basicNack(deliveryTag, false, true);
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=CacheDeleteConsumerTest test`

Expected: PASS with `Tests run: 2, Failures: 0, Errors: 0`.

**Step 5: Commit**

```bash
git add src/main/java/com/example/demo/mq/CacheDeleteConsumer.java src/test/java/com/example/demo/mq/CacheDeleteConsumerTest.java
git commit -m "feat: consume cache delete messages with manual ack"
```

### Task 5: Add The REST Controller

**Files:**
- Create: `src/main/java/com/example/demo/controller/ProductController.java`
- Test: `src/test/java/com/example/demo/controller/ProductControllerTest.java`

**Step 1: Write the failing test**

```java
package com.example.demo.controller;

import com.example.demo.entity.Product;
import com.example.demo.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void shouldReturnProduct() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 15");
        product.setPrice(new BigDecimal("5999.00"));
        when(productService.getByIdWithCache(1L)).thenReturn(product);

        mockMvc.perform(get("/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("iPhone 15"));
    }

    @Test
    void shouldUpdatePrice() throws Exception {
        mockMvc.perform(put("/product/1/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":6999}"))
                .andExpect(status().isOk());

        verify(productService).updatePrice(1L, new BigDecimal("6999"));
    }

    @Test
    void shouldToggleFailureFlag() throws Exception {
        mockMvc.perform(post("/product/mock/fail/true"))
                .andExpect(status().isOk());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=ProductControllerTest test`

Expected: FAIL because the controller does not exist.

**Step 3: Write minimal implementation**

Add a `ProductController` with:

```java
@GetMapping("/{id}")
public Product getById(@PathVariable Long id) {
    return productService.getByIdWithCache(id);
}

@PutMapping("/{id}/price")
public String updatePrice(@PathVariable Long id, @RequestBody UpdatePriceRequest request) {
    productService.updatePrice(id, request.getPrice());
    return "ok";
}

@PostMapping("/mock/fail/{flag}")
public String mockFail(@PathVariable Boolean flag) {
    CacheDeleteConsumer.setMockDeleteFail(flag);
    return "mock delete redis fail = " + flag;
}
```

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=ProductControllerTest test`

Expected: PASS with `Tests run: 3, Failures: 0, Errors: 0`.

**Step 5: Commit**

```bash
git add src/main/java/com/example/demo/controller/ProductController.java src/test/java/com/example/demo/controller/ProductControllerTest.java
git commit -m "feat: add product rest endpoints"
```

### Task 6: Add Demo Documentation And Run Final Verification

**Files:**
- Create: `README.md`

**Step 1: Write the documentation**

Document:

- Project purpose
- Stack
- Startup steps
- `docker-compose up -d`
- `mvn spring-boot:run`
- Query API example
- Update API example
- Failure simulation example
- Expected behavior notes

Include these sample commands:

```bash
curl http://localhost:8080/product/1
curl -X PUT http://localhost:8080/product/1/price -H "Content-Type: application/json" -d "{\"price\":6999}"
curl -X POST http://localhost:8080/product/mock/fail/true
curl -X POST http://localhost:8080/product/mock/fail/false
```

**Step 2: Run unit tests**

Run: `mvn test`

Expected: PASS with all unit tests green.

**Step 3: Run application packaging**

Run: `mvn package`

Expected: BUILD SUCCESS.

**Step 4: Commit**

```bash
git add README.md
git commit -m "docs: add project usage guide"
```

### Final Verification Checklist

- Run: `mvn test`
- Run: `mvn package`
- Run: `git status --short`
- Manually review `README.md` commands against the implemented endpoints
- Confirm `application.yml` uses MySQL `cache_demo`, Redis `localhost:6379`, RabbitMQ `localhost:5672`
- Confirm `schema.sql` and `data.sql` match the `Product` entity
