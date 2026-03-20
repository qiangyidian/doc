# Spring Boot 开发调试与联调工具指南

## 一、这一份文档解决什么问题

这份文档专门讲 Spring Boot 本地开发时最常见的一组“调试工具”和“联调工具”。

很多新手会写 Controller、Service，但项目一运行出问题就不知道该从哪里看。  
实际上，Spring Boot 本地开发最常见的调试链路通常是：

1. 用 DevTools 提高本地重启效率
2. 用 Actuator 查看健康状态和监控端点
3. 用日志配置观察运行细节
4. 用 curl 或 Postman 调接口
5. 用 Docker Compose 启动依赖环境

---

## 二、Spring Boot DevTools

### 1. DevTools 是什么

DevTools 是 Spring Boot 官方提供的开发期辅助工具。  
它最常见的价值是：在本地开发时提升代码修改后的反馈速度。

### 2. 添加依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

这段代码的作用：

- 把 DevTools 加入项目
- `runtime` 表示它主要用于运行期开发辅助，不是业务代码核心依赖
- `optional` 表示这个依赖不会强制传递给下游模块

### 3. 本地开发常见配置

```yaml
spring:
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
```

这段代码的作用：

- `restart.enabled` 打开自动重启能力
- `livereload.enabled` 打开前端 LiveReload 支持
- 这类配置通常只在本地开发阶段使用

---

## 三、Spring Boot Actuator

### 1. Actuator 是什么

Actuator 是 Spring Boot 官方提供的应用监控和运维端点工具。  
它可以帮助你查看：

- 应用是否健康
- 当前启用了哪些端点
- 环境信息
- 指标信息

### 2. 添加依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

这段代码的作用：

- 引入 Spring Boot Actuator
- 项目启动后就可以暴露健康检查等管理端点

### 3. 暴露常见端点

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,env,metrics
  endpoint:
    health:
      show-details: always
```

这段代码的作用：

- `include` 指定哪些端点可以通过 HTTP 访问
- `health` 是最常见的健康检查端点
- `show-details: always` 让健康信息展示更完整，便于本地排查问题

### 4. 本地查看健康状态

```powershell
curl http://localhost:8080/actuator/health
```

这段代码的作用：

- 通过 HTTP 请求查看应用健康状态
- 这是排查“服务到底有没有正常启动”的最基础方式之一

---

## 四、日志配置

### 1. 为什么日志这么重要

在 Spring Boot 开发中，日志几乎是你排查问题的第一入口。  
遇到启动失败、参数绑定异常、数据库连接失败、消息发送失败时，第一时间都应该看日志。

### 2. 最基础的日志级别配置

```yaml
logging:
  level:
    root: info
    com.example: debug
```

这段代码的作用：

- `root: info` 表示全局默认日志级别为 `info`
- `com.example: debug` 表示你自己业务包下输出更详细的调试日志
- 这样可以避免第三方框架日志过多，又能看到自己代码的执行细节

### 3. 在代码里打印日志

```java
package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// @Slf4j 是 Lombok 提供的日志注解。
// 它会自动为当前类生成一个 log 对象，便于记录日志。
@Slf4j
@Service
public class UserService {

    public void loadUser(Long userId) {
        log.info("开始查询用户，userId={}", userId);
        log.debug("这里可以输出更细的调试信息");
    }
}
```

这段代码的作用：

- `@Slf4j` 省去了手动创建 Logger 的样板代码
- `info` 适合记录业务关键节点
- `debug` 适合本地调试细节

---

## 五、curl 与 Postman

### 1. 为什么要学接口调试

Spring Boot 很多时候是做后端接口开发。  
所以你必须会一种最基础的接口调试方式。

### 2. 用 curl 调一个 GET 接口

```powershell
curl http://localhost:8080/users/1
```

这段代码的作用：

- 用最简单的方式调用一个 GET 接口
- 不依赖图形界面工具，适合快速验证服务是否正常

### 3. 用 curl 调一个 POST 接口

```powershell
curl -X POST "http://localhost:8080/users" `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"alice\",\"email\":\"alice@test.com\"}"
```

这段代码的作用：

- `-X POST` 表示发送 POST 请求
- `-H` 设置请求头
- `-d` 发送 JSON 请求体
- 这是最常见的本地接口联调方式之一

### 4. Postman 的定位

如果你不想每次都手敲 `curl`，可以使用 Postman：

- 保存接口集合
- 保存环境变量
- 重复发送请求更方便

但即使你用 Postman，也建议先学会 `curl`，因为它更轻量，也更通用。

---

## 六、Docker Compose

### 1. 为什么本地开发离不开它

Spring Boot 项目经常需要依赖数据库、缓存、MQ。  
如果每个中间件都手动安装，本地环境会很容易变乱。

Docker Compose 的价值是：

- 一次性定义多个服务
- 一条命令启动本地依赖
- 删除重建成本低

### 2. 一个最小示例

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: demo-mysql
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: demo_db
    ports:
      - "3306:3306"

  redis:
    image: redis:7
    container_name: demo-redis
    ports:
      - "6379:6379"
```

这段代码的作用：

- 定义了两个本地依赖服务：`mysql` 和 `redis`
- `ports` 把容器端口映射到宿主机，便于 Spring Boot 直接连接
- `environment` 用来传递初始化参数

### 3. 启动与停止命令

```powershell
# 后台启动容器
docker compose up -d

# 查看运行日志
docker compose logs -f

# 停止并删除容器
docker compose down
```

这段代码的作用：

- `up -d` 让依赖服务在后台运行
- `logs -f` 方便观察中间件启动是否成功
- `down` 适合练习完成后统一清理环境

---

## 七、推荐的本地开发调试顺序

建议你做 Spring Boot 项目时，按下面顺序排查问题：

1. 先看服务是否启动成功
2. 再看 `actuator/health`
3. 再看控制台日志
4. 再用 `curl` 或 Postman 调接口
5. 如果依赖服务连不上，再看 Docker Compose 日志

### 一组常见排查命令

```powershell
# 看 Spring Boot 服务日志
.\mvnw.cmd spring-boot:run

# 看健康检查
curl http://localhost:8080/actuator/health

# 看 Docker 中间件日志
docker compose logs -f
```

这段代码的作用：

- 第一条确认 Spring Boot 进程是否真的启动
- 第二条确认应用健康状态
- 第三条确认依赖中间件是否正常可用

---

## 八、初学者最常见的坑

### 1. DevTools 配了，但没有真正生效

原因通常是：

- 不是以本地开发方式运行
- IDE 编译没有刷新

### 2. Actuator 端点没有暴露

原因通常是：

- 没有配置 `management.endpoints.web.exposure.include`

### 3. 日志太少，看不出问题

原因通常是：

- 包级日志没有开到 `debug`

### 4. Docker Compose 启动了，但服务还是连不上

原因通常是：

- 容器端口没映射
- 用户名密码配错
- 容器虽然启动了，但服务实际上还没准备好

---

## 九、这一份文档你应该记住什么

Spring Boot 本地开发的效率，很大程度不取决于你写代码多快，而取决于你会不会这几类工具：

- DevTools
- Actuator
- 日志
- curl / Postman
- Docker Compose

这几样工具用熟了，你后面调数据库、缓存、MQ、搜索服务时，问题定位会快很多。
