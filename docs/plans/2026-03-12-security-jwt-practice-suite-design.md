# Spring Security + JWT 练习套件设计文档

**Date:** 2026-03-12

## 目标

提供 3 个完全独立的 Spring Boot 练习项目，用来系统学习 `Spring Security + JWT`：

1. `security-jwt-login-demo`：登录成功后签发 JWT，并访问受保护接口
2. `security-jwt-role-demo`：基于 JWT 做角色权限控制
3. `security-jwt-refresh-demo`：实现 Access Token、Refresh Token、续期和退出登录

目标读者是初学者，需要中文 Markdown 文档，并且可以照着文档一步一步创建项目、创建文件、复制代码、启动 Docker 和测试接口。

## 为什么选择这 3 个场景

这 3 个场景基本覆盖了 Spring Security + JWT 在业务系统里最常见的 3 个层次：

- 认证：你是谁
- 授权：你能访问什么
- 令牌生命周期：过期、刷新、退出登录怎么做

## 为什么仍然拆成 3 个独立项目

沿用你之前 MQ、Redis、MyBatis 文档已经确认过的交付方式：

- 每个场景一个独立 Spring Boot 项目
- 每个项目单独一份 `docker-compose.yml`
- 每个项目单独一个 MySQL 数据库
- 每个项目都可以独立启动、独立练习、独立删除

这样最适合小白分阶段练习，不会把多个知识点堆到一个工程里。

## 统一技术路线

- Java 17
- Spring Boot 3.2.5
- Spring Security
- Spring Data JPA
- JJWT 0.13.0
- MySQL 8.0
- Docker Compose
- Maven
- Lombok

## 3 个生产风格场景

### 1. 登录发 JWT

生产意义：

- 用户登录成功后，服务端不再保存传统 Session
- 服务端把用户身份信息放到 JWT 里，前端后续请求带上 Bearer Token
- 资源接口根据 JWT 完成身份校验

练习目标：

- 用户注册
- 用户登录
- 登录成功后签发 JWT
- 使用 JWT 访问受保护的 `/users/me`

### 2. 角色权限控制

生产意义：

- 同样登录了系统，不同角色能访问的接口不同
- 比如普通用户能看个人中心，管理员还能看后台报表
- Spring Security 非常适合做这类请求级、方法级授权

练习目标：

- 登录签发 JWT
- 基于 `ROLE_USER`、`ROLE_ADMIN` 控制接口访问
- 演示 `authorizeHttpRequests` 和 `@PreAuthorize`

### 3. Refresh Token 续期和退出登录

生产意义：

- Access Token 不应该有效期太长
- 需要 Refresh Token 来换取新的 Access Token
- 退出登录时，至少要让 Refresh Token 失效

练习目标：

- 登录返回 accessToken 和 refreshToken
- refreshToken 落库管理
- 调用 `/auth/refresh` 续期
- 调用 `/auth/logout` 使 refreshToken 失效

## 范围

包含：

- 3 个独立 Spring Boot 项目
- 1 份总设计文档
- 3 份详细实现文档
- Docker 启动步骤
- 完整目录结构
- 每个文件的创建顺序
- 完整代码和中文注释
- curl 测试步骤
- 常见报错说明

不包含：

- OAuth2 授权服务器
- 第三方登录
- Redis Token 黑名单
- 单点登录
- 多因子认证
- Spring Authorization Server

## 端口规划

为了避免本机冲突，3 个项目分别使用不同端口：

- `security-jwt-login-demo`
  - 应用端口：`8111`
  - MySQL：`3316`
- `security-jwt-role-demo`
  - 应用端口：`8112`
  - MySQL：`3317`
- `security-jwt-refresh-demo`
  - 应用端口：`8113`
  - MySQL：`3318`

## 文档交付物

将创建以下 4 个文档：

- `docs/plans/2026-03-12-security-jwt-practice-suite-design.md`
- `docs/plans/2026-03-12-security-jwt-login-demo.md`
- `docs/plans/2026-03-12-security-jwt-role-demo.md`
- `docs/plans/2026-03-12-security-jwt-refresh-demo.md`

每份实现文档都包含：

- 场景说明
- 项目目录结构
- Docker 启动
- `pom.xml`
- `application.yml`
- `schema.sql`
- `data.sql`
- `entity`
- `repository`
- `dto`
- `security`
- `service`
- `controller`
- 接口测试
- 常见错误排查

## 推荐学习顺序

1. `security-jwt-login-demo`
2. `security-jwt-role-demo`
3. `security-jwt-refresh-demo`

原因：

- 先掌握最基础的注册、登录、JWT 校验
- 再学习基于角色的接口访问控制
- 最后理解 Token 续期和退出登录
