# AI Function Calling主流示例项目文档

**目标：** 从零创建一个 Spring Boot 3 示例项目，使用当前主流的 `OpenAI Responses API + Function Calling` 实现一个“智能客服工单助手”，让模型能够根据用户问题自动调用本地后端工具，例如查询订单、查询物流、创建售后工单。

**架构说明：** 这个项目采用 `Spring Boot + OpenAI Responses API + 自定义函数工具` 的模式。AI 模型并不直接访问你的数据库或系统，而是先根据用户提问判断是否需要调用函数；如果需要，就返回函数名和参数；然后由你的后端执行真实业务方法，再把工具执行结果回传给模型，最后由模型生成自然语言答复。这正是现在最主流的 function calling 集成方式之一。

**技术栈：** Java 17、Spring Boot 3.2.5、Spring Web、Spring Validation、Jackson、Lombok、OpenAI Responses API、`RestClient`

---

## 一、为什么我推荐你学这个示例项目

你这次要的是“现在主流的 AI Function Calling 示例项目”，而不是一个概念演示。

我选择这个方案，是因为当前官方资料显示：

1. OpenAI 现在主推的是 `Responses API`
2. `Assistants API` 已经进入弃用迁移阶段
3. `Responses API` 原生支持 function calling
4. OpenAI 当前模型文档也明确把 function calling 作为标准能力之一

截至 `2026-03-24`，OpenAI 官方文档显示：

- Responses API 是统一接口，支持工具调用
- 在 Responses API 里，工具调用和工具输出是独立 item，并通过 `call_id` 关联
- 模型选择上，如果你要复杂推理可以从 `gpt-5.4` 起步；如果你要低延迟、低成本的高频业务场景，可以选择 `gpt-5-mini`

所以这份文档的项目默认采用：

- `OpenAI Responses API`
- `gpt-5-mini`

原因：

- 成本更适合做 function calling 示例
- 延迟更低
- 功能上支持 function calling 和 structured outputs

参考：

- [OpenAI Models 总览](https://developers.openai.com/api/docs/models)
- [GPT-5 mini Model](https://developers.openai.com/api/docs/models/gpt-5-mini)
- [Migrate to the Responses API](https://developers.openai.com/api/docs/guides/migrate-to-responses)
- [Function Calling in the OpenAI API](https://help.openai.com/en/articles/8555517-function-calling-updates)

---

## 二、这个项目在真实开发里像什么

这个项目模拟的是一个常见企业场景：

- 用户在客服聊天框里提问
- AI 助手根据问题判断要不要调用业务工具
- 如果只是普通问答，模型直接回答
- 如果用户问“订单在哪”“帮我创建售后工单”“帮我查订单详情”，模型就触发 function calling
- 你的后端执行真实业务代码
- 再把结果回给模型，由模型生成最终自然语言答案

这类场景在真实开发里非常常见，例如：

1. 智能客服助手
2. 智能工单系统
3. 智能内部运营助手
4. 智能 CRM 助手
5. 智能订单查询机器人

---

## 三、Function Calling 的核心流程到底是什么

你必须先把这 5 步理解清楚，不然后面只是在抄代码：

1. 你把“可用工具列表”告诉模型
2. 模型判断：这次问题是否需要调用工具
3. 如果需要，模型返回“工具名 + 参数”
4. 你的后端执行这个工具
5. 你把工具结果回传给模型，让模型生成最终答复

也就是说：

- 模型只负责“决定调用什么工具、用什么参数”
- 真正执行工具的，是你自己的后端代码

这是 function calling 最关键的一点。

---

## 四、项目最终目录结构

```text
openai-function-calling-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.example.aifunctioncalling
│   │   │       ├── AiFunctionCallingApplication.java
│   │   │       ├── common
│   │   │       │   └── Result.java
│   │   │       ├── config
│   │   │       │   └── OpenAiProperties.java
│   │   │       ├── controller
│   │   │       │   └── AiAssistantController.java
│   │   │       ├── dto
│   │   │       │   ├── AssistantChatRequest.java
│   │   │       │   └── AssistantChatResponse.java
│   │   │       ├── service
│   │   │       │   ├── AiAssistantService.java
│   │   │       │   ├── OpenAiResponsesService.java
│   │   │       │   ├── ToolDispatcherService.java
│   │   │       │   ├── OrderToolService.java
│   │   │       │   ├── LogisticsToolService.java
│   │   │       │   └── TicketToolService.java
│   │   │       └── util
│   │   │           └── ToolSchemaFactory.java
│   │   └── resources
│   │       └── application.yml
```

这段代码的作用：

- 这是你后面照着创建文件的完整项目结构
- 这个项目重点是 function calling，所以没有接数据库，业务数据用内存模拟
- 这样你能把注意力集中在“模型调用工具”这条主线

---

## 五、先创建 `pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <!-- Maven POM 模型版本。 -->
    <modelVersion>4.0.0</modelVersion>

    <!-- 继承 Spring Boot 父工程。 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>openai-function-calling-demo</artifactId>
    <version>1.0.0</version>
    <name>openai-function-calling-demo</name>
    <description>AI Function Calling 主流示例项目</description>

    <properties>
        <!-- Java 17 是 Spring Boot 3 的主流选择。 -->
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Web 开发依赖，用于提供 REST 接口。 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 参数校验依赖，用于校验聊天请求。 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Lombok 用于减少样板代码。 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot 测试依赖。 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot 打包插件。 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

这段代码的作用：

- 这份示例项目只需要最基本的 Web、Validation、Jackson、Lombok 能力
- 我这里没有直接引入第三方 OpenAI SDK，而是用 Spring 自带的 `RestClient` 直接调用 Responses API
- 这样你会更容易理解 function calling 的底层交互过程

---

## 六、创建 `application.yml`

```yaml
server:
  # 应用运行端口。
  port: 8080

spring:
  application:
    # 应用名称。
    name: openai-function-calling-demo

openai:
  # OpenAI API 基础地址。
  base-url: https://api.openai.com/v1
  # 从环境变量读取 API Key，避免把密钥硬编码到代码里。
  api-key: ${OPENAI_API_KEY:}
  # 当前示例默认使用 gpt-5-mini。
  # 官方模型总览说明：如果要低成本、低延迟高频任务，可以优先从 gpt-5-mini 开始。
  model: gpt-5-mini
```

这段代码的作用：

- 配置 OpenAI API 的地址、模型和 API Key
- 真实开发里一定不要把密钥写死在代码或 Git 仓库里

---

## 七、先准备环境变量

在 Windows PowerShell 里执行：

```powershell
$env:OPENAI_API_KEY="你的OpenAI_API_Key"
```

这段代码的作用：

- 在当前终端会话里注入 API Key
- 后面 Spring Boot 启动时，就能从 `application.yml` 里读取这个环境变量

---

## 八、创建启动类 `AiFunctionCallingApplication.java`

```java
package com.example.aifunctioncalling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication 是 Spring Boot 项目的启动入口注解。
@SpringBootApplication
public class AiFunctionCallingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiFunctionCallingApplication.class, args);
    }
}
```

这段代码的作用：

- 启动整个 Spring Boot 项目

---

## 九、创建统一返回对象 `common/Result.java`

```java
package com.example.aifunctioncalling.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    // 业务状态码。
    private Integer code;

    // 返回消息。
    private String message;

    // 实际数据。
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

---

## 十、创建 OpenAI 配置类 `config/OpenAiProperties.java`

```java
package com.example.aifunctioncalling.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

    // API 基础地址。
    private String baseUrl;

    // API Key。
    private String apiKey;

    // 默认模型。
    private String model;
}
```

这段代码的作用：

- 把 `application.yml` 里的 OpenAI 配置映射成 Java 对象
- 这样后面服务层就能直接读取 `baseUrl`、`apiKey`、`model`

---

## 十一、创建 DTO

### 11.1 创建 `dto/AssistantChatRequest.java`

```java
package com.example.aifunctioncalling.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssistantChatRequest {

    // 用户原始提问内容。
    @NotBlank(message = "用户问题不能为空")
    private String message;
}
```

这段代码的作用：

- 接收前端或 Postman 发来的聊天问题

### 11.2 创建 `dto/AssistantChatResponse.java`

```java
package com.example.aifunctioncalling.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssistantChatResponse {

    // 最终给用户的自然语言答复。
    private String answer;

    // 这次实际调用了哪些工具。
    private List<String> calledTools;

    // 原始模型响应 ID，方便排查链路。
    private String responseId;
}
```

这段代码的作用：

- 返回最终答案
- 同时把这次调用过的工具名也返回出来，方便你学习和调试

---

## 十二、创建工具定义工厂 `util/ToolSchemaFactory.java`

```java
package com.example.aifunctioncalling.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ToolSchemaFactory {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<JsonNode> buildTools() {
        // 这里一次性告诉模型：当前系统里有哪些工具可用。
        return List.of(
                buildQueryOrderTool(),
                buildQueryLogisticsTool(),
                buildCreateTicketTool()
        );
    }

    private JsonNode buildQueryOrderTool() {
        return objectMapper.createObjectNode()
                .put("type", "function")
                .put("name", "query_order_detail")
                .put("description", "查询订单详情，适用于用户询问订单金额、状态、商品信息")
                .set("parameters", objectMapper.createObjectNode()
                        .put("type", "object")
                        .set("properties", objectMapper.createObjectNode()
                                .set("order_no", objectMapper.createObjectNode()
                                        .put("type", "string")
                                        .put("description", "订单号")))
                        .put("additionalProperties", false)
                        .set("required", objectMapper.createArrayNode().add("order_no")));
    }

    private JsonNode buildQueryLogisticsTool() {
        return objectMapper.createObjectNode()
                .put("type", "function")
                .put("name", "query_logistics_status")
                .put("description", "查询订单物流状态，适用于用户询问快递到哪了、预计送达时间")
                .set("parameters", objectMapper.createObjectNode()
                        .put("type", "object")
                        .set("properties", objectMapper.createObjectNode()
                                .set("order_no", objectMapper.createObjectNode()
                                        .put("type", "string")
                                        .put("description", "订单号")))
                        .put("additionalProperties", false)
                        .set("required", objectMapper.createArrayNode().add("order_no")));
    }

    private JsonNode buildCreateTicketTool() {
        return objectMapper.createObjectNode()
                .put("type", "function")
                .put("name", "create_after_sale_ticket")
                .put("description", "创建售后工单，适用于用户申请退款、退货、破损赔付")
                .set("parameters", objectMapper.createObjectNode()
                        .put("type", "object")
                        .set("properties", objectMapper.createObjectNode()
                                .set("order_no", objectMapper.createObjectNode()
                                        .put("type", "string")
                                        .put("description", "订单号"))
                                .set("reason", objectMapper.createObjectNode()
                                        .put("type", "string")
                                        .put("description", "售后原因"))
                                .set("contact_phone", objectMapper.createObjectNode()
                                        .put("type", "string")
                                        .put("description", "联系电话")))
                        .put("additionalProperties", false)
                        .set("required", objectMapper.createArrayNode()
                                .add("order_no")
                                .add("reason")
                                .add("contact_phone")));
    }
}
```

这段代码的作用：

- 用 JSON Schema 的方式把 3 个工具定义告诉模型
- function calling 的关键不是“让模型随便猜”，而是“把工具边界和参数说清楚”

---

## 十三、创建 3 个业务工具服务

### 13.1 创建 `service/OrderToolService.java`

```java
package com.example.aifunctioncalling.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OrderToolService {

    public Map<String, Object> queryOrderDetail(String orderNo) {
        // 这里用内存数据模拟真实订单系统。
        return Map.of(
                "order_no", orderNo,
                "status", "PAID",
                "amount", "199.00",
                "goods_name", "无线耳机",
                "buyer", "张三"
        );
    }
}
```

这段代码的作用：

- 模拟“查询订单详情”这个真实业务工具

### 13.2 创建 `service/LogisticsToolService.java`

```java
package com.example.aifunctioncalling.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LogisticsToolService {

    public Map<String, Object> queryLogisticsStatus(String orderNo) {
        // 这里用内存数据模拟真实物流系统。
        return Map.of(
                "order_no", orderNo,
                "company", "顺丰",
                "status", "运输中",
                "latest_trace", "快件已到达杭州转运中心",
                "estimated_arrival", "明天 18:00 前"
        );
    }
}
```

这段代码的作用：

- 模拟“查询物流状态”这个真实业务工具

### 13.3 创建 `service/TicketToolService.java`

```java
package com.example.aifunctioncalling.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class TicketToolService {

    public Map<String, Object> createAfterSaleTicket(String orderNo, String reason, String contactPhone) {
        // 这里用 UUID 模拟工单号生成。
        String ticketNo = "TICKET-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        return Map.of(
                "ticket_no", ticketNo,
                "order_no", orderNo,
                "reason", reason,
                "contact_phone", contactPhone,
                "status", "CREATED"
        );
    }
}
```

这段代码的作用：

- 模拟“创建售后工单”这个真实业务工具
- 在真实系统里，这里通常会落数据库、发 MQ、通知客服系统

---

## 十四、创建工具分发器 `service/ToolDispatcherService.java`

```java
package com.example.aifunctioncalling.service;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String dispatch(String toolName, String argumentsJson) {
        try {
            JsonNode argsNode = objectMapper.readTree(argumentsJson);

            // 根据模型返回的工具名，路由到本地真实业务方法。
            return switch (toolName) {
                case "query_order_detail" -> objectMapper.writeValueAsString(
                        orderToolService.queryOrderDetail(argsNode.get("order_no").asText())
                );
                case "query_logistics_status" -> objectMapper.writeValueAsString(
                        logisticsToolService.queryLogisticsStatus(argsNode.get("order_no").asText())
                );
                case "create_after_sale_ticket" -> objectMapper.writeValueAsString(
                        ticketToolService.createAfterSaleTicket(
                                argsNode.get("order_no").asText(),
                                argsNode.get("reason").asText(),
                                argsNode.get("contact_phone").asText()
                        )
                );
                default -> throw new IllegalArgumentException("未知工具：" + toolName);
            };
        } catch (Exception e) {
            throw new RuntimeException("执行工具失败：" + toolName, e);
        }
    }
}
```

这段代码的作用：

- 把模型返回的“函数名 + 参数”真正映射到你的本地业务代码
- 这就是 function calling 项目里最核心的一层“工具执行层”

---

## 十五、创建 OpenAI Responses API 调用服务 `service/OpenAiResponsesService.java`

```java
package com.example.aifunctioncalling.service;

import com.example.aifunctioncalling.config.OpenAiProperties;
import com.example.aifunctioncalling.util.ToolSchemaFactory;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode createFirstResponse(String userMessage) {
        RestClient client = buildClient();

        JsonNode requestBody = objectMapper.createObjectNode()
                // 当前使用的模型。
                .put("model", openAiProperties.getModel())
                // 系统指令：告诉模型自己是客服助手，并且需要优先使用工具。
                .put("instructions", "你是一个电商客服助手。如果问题涉及订单、物流、售后，请优先调用工具获取真实业务数据，再给用户答复。")
                // 用户输入。
                .put("input", userMessage)
                // 把 tools 挂进去，模型才能知道有哪些函数可调用。
                .set("tools", objectMapper.valueToTree(toolSchemaFactory.buildTools()));

        return client.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode createSecondResponse(String previousResponseId, List<JsonNode> toolOutputs) {
        RestClient client = buildClient();

        JsonNode requestBody = objectMapper.createObjectNode()
                .put("model", openAiProperties.getModel())
                // 告诉 Responses API：这是接在上一个响应后面的继续推理。
                .put("previous_response_id", previousResponseId)
                // 这里把 function_call_output items 传回去。
                .set("input", objectMapper.valueToTree(toolOutputs));

        return client.post()
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

- 专门负责和 OpenAI Responses API 通信
- 第一次请求：把用户问题和工具列表发给模型
- 第二次请求：把工具执行结果回传给模型，让模型生成最终答复

---

## 十六、创建 AI 助手主服务 `service/AiAssistantService.java`

```java
package com.example.aifunctioncalling.service;

import com.example.aifunctioncalling.dto.AssistantChatResponse;
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

    // 专门负责调用 OpenAI Responses API。
    private final OpenAiResponsesService openAiResponsesService;
    // 专门负责把模型请求的工具路由到本地 Java 方法。
    private final ToolDispatcherService toolDispatcherService;
    // Spring Boot 默认已经帮我们准备好了 ObjectMapper，这里直接注入即可。
    private final ObjectMapper objectMapper;

    public AssistantChatResponse chat(String userMessage) {
        // 第 1 步：先把用户问题和可用工具列表发给模型。
        JsonNode firstResponse = openAiResponsesService.createFirstResponse(userMessage);

        // 记录本轮模型调用过哪些工具，方便我们在接口返回里展示给调用方。
        List<String> calledTools = new ArrayList<>();

        // 第 2 步：解析模型是否返回了 function_call。
        List<JsonNode> toolOutputs = buildToolOutputs(firstResponse, calledTools);

        // 如果没有任何工具调用，说明模型认为这次问题可以直接自然语言回答。
        if (toolOutputs.isEmpty()) {
            return new AssistantChatResponse(
                    extractTextAnswer(firstResponse),
                    calledTools,
                    firstResponse.path("id").asText()
            );
        }

        // 第 3 步：如果模型要求调用工具，就把工具结果回传给模型继续生成最终答案。
        JsonNode secondResponse = openAiResponsesService.createSecondResponse(
                firstResponse.path("id").asText(),
                toolOutputs
        );

        // 第 4 步：把第二轮模型生成的最终文本答复返回给前端。
        return new AssistantChatResponse(
                extractTextAnswer(secondResponse),
                calledTools,
                secondResponse.path("id").asText()
        );
    }

    private List<JsonNode> buildToolOutputs(JsonNode responseNode, List<String> calledTools) {
        List<JsonNode> toolOutputs = new ArrayList<>();

        // Responses API 的核心输出在 output 数组里。
        for (JsonNode item : responseNode.path("output")) {
            // 只有 type=function_call 的 item 才表示“模型要求调用函数”。
            if (!"function_call".equals(item.path("type").asText())) {
                continue;
            }

            // 取出模型指定的工具名。
            String toolName = item.path("name").asText();
            // arguments 是 JSON 字符串，例如 {"order_no":"ORD-1001"}。
            String argumentsJson = item.path("arguments").asText("{}");
            // call_id 是工具调用的唯一关联 ID，后面必须原样回传。
            String callId = item.path("call_id").asText();

            // 真正执行本地工具逻辑。
            String toolResult = toolDispatcherService.dispatch(toolName, argumentsJson);

            // 记录调用过的工具名，便于学习和调试。
            calledTools.add(toolName);

            // 根据官方文档，这里要构造 type=function_call_output 的 item 回传给模型。
            ObjectNode toolOutput = objectMapper.createObjectNode();
            toolOutput.put("type", "function_call_output");
            toolOutput.put("call_id", callId);
            // output 通常传字符串即可，JSON 字符串也是很常见的方式。
            toolOutput.put("output", toolResult);

            toolOutputs.add(toolOutput);
        }

        return toolOutputs;
    }

    private String extractTextAnswer(JsonNode responseNode) {
        // Responses API 在很多场景下会直接给出 output_text，优先读取这个最方便。
        String outputText = responseNode.path("output_text").asText();
        if (outputText != null && !outputText.isBlank()) {
            return outputText;
        }

        // 如果 output_text 为空，就退化到手动解析 output -> message -> content -> output_text。
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

        // 如果还是没有解析到文本，就返回一个兜底提示，避免接口直接返回 null。
        if (answerBuilder.isEmpty()) {
            return "模型本次没有返回可直接展示的文本内容，请检查工具输出或响应结构。";
        }
        return answerBuilder.toString();
    }
}
```

这段代码的作用：

- 这是整个示例项目最核心的业务服务，负责把“模型调用工具”的完整闭环串起来
- 第一轮请求拿到 `function_call`
- 后端执行真实工具
- 再把 `function_call_output` 回传给模型生成最终答案
- 如果模型根本不需要工具，就直接返回普通文本回答

---

## 十七、创建控制器 `controller/AiAssistantController.java`

```java
package com.example.aifunctioncalling.controller;

import com.example.aifunctioncalling.common.Result;
import com.example.aifunctioncalling.dto.AssistantChatRequest;
import com.example.aifunctioncalling.dto.AssistantChatResponse;
import com.example.aifunctioncalling.service.AiAssistantService;
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

    // Controller 只负责收请求和回响应，真正业务交给 Service。
    private final AiAssistantService aiAssistantService;

    @PostMapping("/chat")
    public Result<AssistantChatResponse> chat(@Valid @RequestBody AssistantChatRequest request) {
        // 把用户输入交给 AI 助手服务处理。
        AssistantChatResponse response = aiAssistantService.chat(request.getMessage());
        return Result.success(response);
    }
}
```

这段代码的作用：

- 对外暴露一个统一的聊天接口
- 前端只需要传一句用户问题，后端就会自动完成模型调用、工具执行和最终答复生成

---

## 十八、到这里你已经真正实现了什么

到这里，这个项目其实已经具备了一个完整的 function calling 最小闭环：

1. 用户发送自然语言问题
2. 后端把问题和工具清单交给模型
3. 模型按需返回 `function_call`
4. 后端执行本地 Java 工具
5. 后端把 `function_call_output` 回传给模型
6. 模型生成最终自然语言答案
7. 接口把答案返回给前端

这就是现在主流 AI Agent / AI Copilot / AI 客服集成里最常见的一种后端写法。

---

## 十九、启动项目

在项目根目录执行：

```powershell
mvn spring-boot:run
```

这段代码的作用：

- 启动 Spring Boot 项目
- 启动后默认监听 `8080` 端口

如果你更习惯先编译再运行，也可以执行：

```powershell
mvn clean package
java -jar target/openai-function-calling-demo-1.0.0.jar
```

这段代码的作用：

- 先把项目打成 jar 包
- 再通过 `java -jar` 方式启动

---

## 二十、开始测试接口

下面我给你 4 个最有代表性的测试场景。

### 20.1 普通对话，不触发工具

```powershell
curl.exe -X POST "http://localhost:8080/ai/assistant/chat" ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"你好，你是谁？\"}"
```

这段代码的作用：

- 测试一个纯闲聊问题
- 理论上这类问题不需要查订单、不需要查物流，也不需要建工单
- 所以模型大概率会直接回答，而不会触发 function calling

预期现象：

- `calledTools` 为空数组
- `answer` 里有一段普通自然语言答复

### 20.2 查询订单详情，触发 `query_order_detail`

```powershell
curl.exe -X POST "http://localhost:8080/ai/assistant/chat" ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"帮我查一下订单 ORD-1001 的详情\"}"
```

这段代码的作用：

- 测试“查询订单详情”场景
- 模型应该识别出这属于订单查询问题
- 然后自动调用 `query_order_detail`

预期现象：

- `calledTools` 包含 `query_order_detail`
- 最终答复里会包含订单号、状态、金额、商品名称等信息

### 20.3 查询物流，触发 `query_logistics_status`

```powershell
curl.exe -X POST "http://localhost:8080/ai/assistant/chat" ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"订单 ORD-1001 现在物流到哪里了？\"}"
```

这段代码的作用：

- 测试“查物流”场景
- 模型应该自动选择物流工具，而不是订单详情工具

预期现象：

- `calledTools` 包含 `query_logistics_status`
- 最终答复里会包含物流公司、运输状态、最新轨迹等信息

### 20.4 创建售后工单，触发 `create_after_sale_ticket`

```powershell
curl.exe -X POST "http://localhost:8080/ai/assistant/chat" ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"订单 ORD-1001 的耳机有杂音，帮我创建售后工单，我的手机号是 13800000000\"}"
```

这段代码的作用：

- 测试“创建售后工单”场景
- 模型应该提取出订单号、售后原因、联系电话
- 然后调用 `create_after_sale_ticket`

预期现象：

- `calledTools` 包含 `create_after_sale_ticket`
- `answer` 里会告诉你工单创建成功，并带上工单号

---

## 二十一、一个典型返回结果长什么样

你调用接口后，返回结果大概会长这样：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "answer": "我已经帮你查询到订单 ORD-1001，当前状态为已支付，商品是无线耳机，订单金额为 199.00 元。",
    "calledTools": [
      "query_order_detail"
    ],
    "responseId": "resp_123456789"
  }
}
```

这段代码的作用：

- `answer` 表示最终给用户展示的自然语言答案
- `calledTools` 表示这轮对话里到底调用了哪些本地工具
- `responseId` 是 Responses API 返回的响应 ID，后续排查问题时很有帮助

---

## 二十二、为什么这个项目写法是现在主流

你会发现，这个项目有几个很典型的“主流 AI 后端集成”特征：

1. **模型不直接碰数据库**
   模型只是决定“该调哪个工具”，真正查订单、查物流、建工单都由你自己的后端执行。

2. **工具由后端显式注册**
   你把工具 Schema 明确告诉模型，模型不会胡乱猜测有哪些能力。

3. **工具调用和工具输出分离**
   Responses API 把 `function_call` 和 `function_call_output` 分成两类 item，这比早期“把一切塞进 messages”更清晰。

4. **天然适合扩展**
   你后面完全可以继续加：
   - 查询退款状态
   - 查询优惠券
   - 修改收货地址
   - 创建人工客服转接单

也就是说，这个示例不是玩具，而是一个真实项目里非常像样的骨架。

---

## 二十三、后续怎么扩展成真正可用的业务项目

如果你想把这个示例继续升级成“更接近生产”的版本，下一步一般会做这些事：

1. 把内存模拟数据替换成 MySQL / Redis / RPC 调用
2. 给工具执行层增加异常码、超时、重试和审计日志
3. 给每个工具增加更严格的参数校验
4. 加上用户上下文，例如用户 ID、店铺 ID、租户 ID
5. 把历史对话存到数据库或会话系统里
6. 对工具结果做脱敏，避免把敏感字段直接返回给模型
7. 对不同模型做路由，例如简单问题走 `gpt-5-mini`，复杂问题走更强模型

---

## 二十四、常见报错排查

### 24.1 启动时报 API Key 为空

原因：

- 你没有设置 `OPENAI_API_KEY`
- 或者当前终端设置了，但 IDE 启动项目时没有继承到这个环境变量

排查方法：

```powershell
echo $env:OPENAI_API_KEY
```

这段代码的作用：

- 检查当前 PowerShell 会话里是否真的存在 API Key

### 24.2 模型没有调用工具，直接瞎答

原因可能有这些：

1. 你的工具描述写得太模糊
2. 你的系统指令没有强调“先调工具拿真实数据”
3. 用户提问方式太模糊，模型无法判断应该用哪个工具

改进建议：

- 把工具 `description` 写得更明确
- 把 `parameters` 的字段名写得更业务化
- 在 `instructions` 里强调“订单、物流、售后问题必须优先调用工具”

### 24.3 第二轮请求后没有最终文本

原因可能有这些：

1. 你没有把 `function_call_output` 正确回传
2. 你漏传了 `call_id`
3. 你传回去的 `output` 不是字符串或格式异常

这一点非常关键：

- `call_id` 必须和第一轮模型返回的 `call_id` 对应上
- 否则模型无法知道“这是哪一个工具调用的结果”

### 24.4 工具执行报错

原因：

- 模型生成的参数 JSON 不完整
- 你的本地工具代码没有做空值处理
- 你的工具分发器里 `toolName` 和 Schema 里的名字不一致

建议：

- 统一把工具名写成常量
- 对 `arguments` 做必填字段校验
- 在 `dispatch` 里打印工具名和参数日志

### 24.5 为什么我发了多个工具，模型只调用一个

这其实是正常现象。

模型会根据问题内容判断到底要不要调用多个工具。  
例如用户只问“订单详情”，它通常没必要再去查物流或创建工单。

---

## 二十五、你通过这个项目真正学到了什么

如果你把这份文档完整照着做一遍，你会真正掌握下面这些能力：

1. 知道现在主流的 OpenAI function calling 集成方式是什么
2. 知道为什么推荐用 `Responses API`
3. 知道 function calling 不是“模型自己执行函数”，而是“模型决定调用，后端真实执行”
4. 知道如何定义工具 Schema
5. 知道如何解析 `function_call`
6. 知道如何构造 `function_call_output`
7. 知道如何把工具结果再次交给模型生成最终答案
8. 知道怎样把一个 AI Demo 扩展成企业真实项目

---

## 二十六、这份示例和你以后工作里的对应关系

你以后在工作里看到的很多项目，其实都可以套用这个骨架：

- 智能客服机器人
- 智能工单助手
- 智能 CRM 助手
- 智能售后机器人
- 智能运营查询机器人

差别往往不在于 function calling 原理变了，而在于：

- 工具更多
- 权限更复杂
- 数据源更多
- 安全要求更高

所以你现在把这份文档吃透，后面迁移到别的业务场景会非常快。

---

## 二十七、官方参考资料

- [OpenAI Models 总览](https://developers.openai.com/api/docs/models)
- [GPT-5 mini Model](https://developers.openai.com/api/docs/models/gpt-5-mini)
- [Migrate to the Responses API](https://developers.openai.com/api/docs/guides/migrate-to-responses)
- [Function calling](https://developers.openai.com/api/docs/guides/function-calling)
- [Create a model response](https://developers.openai.com/api/reference/resources/responses/methods/create)
- [Spring Framework RestClient](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)

---

## 二十八、最后总结

如果你只是想学“AI function calling 现在主流怎么做”，这份项目已经足够完整：

- 技术路线是主流的
- 接口是能跑通的
- 代码结构是接近真实开发的
- 你能从里面直接看到：模型、工具、后端代码是怎么协作的

如果你下一步想继续学，我建议你按这个顺序升级：

1. 接 MySQL，把订单数据改成真实查询
2. 接 Redis，给会话和工具结果做缓存
3. 接用户体系，让模型根据当前登录用户查自己的订单
4. 接流式输出，把最终答案改成流式返回
5. 接 MCP / 更复杂工具系统，做成真正的 Agent
