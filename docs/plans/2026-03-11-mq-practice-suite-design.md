# MQ Practice Suite Design

**Date:** 2026-03-11

## Goal

Provide three completely independent Spring Boot practice projects for MQ learning:

1. `mq-order-notify-demo`: create an order, then send a notification asynchronously
2. `mq-payment-points-demo`: mark payment success, then grant points asynchronously
3. `mq-stock-sync-demo`: deduct stock, then sync to an external system asynchronously

The target reader is a beginner who needs step-by-step Markdown documents and can follow the documents to create each project from scratch.

## Why Three Independent Projects

The user explicitly asked that the business processes do not depend on each other and preferred full isolation over code reuse. Therefore:

- each project has its own Spring Boot application
- each project has its own database schema
- each project has its own RabbitMQ exchange, queue, and routing key
- each project has its own Docker Compose file
- each project can be started, stopped, and deleted independently

## Unified Technical Direction

- Java 17
- Spring Boot 3.2.5
- Spring Web
- Spring AMQP with RabbitMQ
- MyBatis-Plus Boot 3 starter
- MySQL 8.0 in Docker
- RabbitMQ management image in Docker
- Maven build

## Production-Style MQ Scenarios

### 1. Order Notify

Production meaning:

- order creation is the core transaction
- SMS/email/site-message notification is a non-core follow-up step
- notification should not block the API response

Practice target:

- save the order first
- publish an order notification message
- consume the message and record a notification log

### 2. Payment Points

Production meaning:

- payment success is the core transaction
- reward points are a derived action
- payment should return quickly even if the points subsystem is slow

Practice target:

- update payment status first
- publish a payment success message
- consume the message and record a points log
- explain beginner-level duplicate-consumption protection

### 3. Stock Sync

Production meaning:

- inventory deduction is the core transaction
- downstream systems often need an async sync after stock changes
- external-system integration is less reliable and should not block stock deduction

Practice target:

- deduct stock first
- publish a stock-change message
- consume the message and record an external sync log

## Scope

Included:

- three standalone Spring Boot projects
- one detailed Markdown implementation guide per project
- Docker Compose startup steps
- full file creation order
- complete code blocks with detailed comments
- API test commands and expected observations
- troubleshooting notes

Excluded:

- distributed transactions
- local message table
- dead-letter queues
- delayed queues
- full idempotency framework
- production monitoring, alerting, and tracing

## Implementation Style

The projects use an "entry-enhanced" level:

- simple enough for beginners
- realistic enough to show why MQ is used in production
- manual acknowledgment is introduced
- failure handling is explained at an introductory level
- over-abstraction is avoided

## Port Strategy

To avoid port conflicts when multiple practice projects are present on the same machine, each project uses a separate port group.

- `mq-order-notify-demo`
  - app: `8081`
  - mysql: `3307`
  - rabbitmq amqp: `5673`
  - rabbitmq console: `15673`
- `mq-payment-points-demo`
  - app: `8082`
  - mysql: `3308`
  - rabbitmq amqp: `5674`
  - rabbitmq console: `15674`
- `mq-stock-sync-demo`
  - app: `8083`
  - mysql: `3309`
  - rabbitmq amqp: `5675`
  - rabbitmq console: `15675`

## Document Deliverables

The following implementation documents will be created:

- `docs/plans/2026-03-11-mq-order-notify-demo.md`
- `docs/plans/2026-03-11-mq-payment-points-demo.md`
- `docs/plans/2026-03-11-mq-stock-sync-demo.md`

Each document contains:

- project purpose
- production scenario explanation
- final directory structure
- Docker startup steps
- every file to create
- complete sample code with detailed comments
- step-by-step API testing
- common error diagnosis

## Learning Order

Recommended learning order:

1. `mq-order-notify-demo`
2. `mq-payment-points-demo`
3. `mq-stock-sync-demo`

Reason:

- order notification is the most intuitive entry scenario
- payment points adds derived business and duplicate-consumption thinking
- stock sync introduces async integration with external systems
