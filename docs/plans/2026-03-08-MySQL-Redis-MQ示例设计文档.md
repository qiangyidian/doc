# MySQL Redis MQ Demo Design

**Date:** 2026-03-08

## Goal

Build a minimal runnable Spring Boot 3 demo that shows this write path:

1. Update MySQL product data
2. Send a cache-delete message to RabbitMQ
3. Consume the message and delete the Redis cache asynchronously
4. Retry via RabbitMQ redelivery when Redis deletion fails

The project is intentionally scoped as an interview-style demo, not a production-ready implementation.

## Scope

Included:

- Single Spring Boot application
- REST API for query, update, and failure simulation
- MyBatis-Plus for `product` table access
- Redis cache-aside read path
- RabbitMQ producer and manual-ack consumer
- Docker Compose for MySQL, Redis, RabbitMQ
- SQL initialization scripts
- Minimal unit tests for service and consumer behavior
- README with startup and demo commands

Excluded:

- Dead-letter queue
- Max retry count
- Local message table
- Transactional messaging
- Testcontainers or full integration tests
- Production hardening

## Architecture

The application is a small monolith with these packages:

- `controller`: HTTP endpoints
- `service`: cache-aside query and update workflow
- `mapper`: MyBatis-Plus mapper
- `entity`: product entity
- `mq`: message DTO, producer, consumer
- `config`: RabbitMQ config

Runtime layout:

- MySQL, Redis, RabbitMQ run through Docker Compose
- Spring Boot app runs locally with Maven
- Spring Boot loads schema and seed data on startup

## Data Model

Table: `product`

Fields:

- `id`
- `name`
- `price`
- `update_time`

Seed data:

- `iPhone 15`
- `Mate 60`

## API Design

### Query Product

`GET /product/{id}`

Behavior:

1. Read Redis key `product:{id}`
2. If found, deserialize and return
3. If missing, query MySQL
4. If found, write back to Redis with a 30-minute TTL
5. Return product

### Update Price

`PUT /product/{id}/price`

Request body:

```json
{
  "price": 6999
}
```

Behavior:

1. Load product from MySQL
2. Throw error if not found
3. Update price in MySQL
4. Publish a cache-delete message to RabbitMQ

### Toggle Failure Simulation

`POST /product/mock/fail/{flag}`

Behavior:

1. Toggle a static flag in the consumer
2. When enabled, consumer throws before deleting Redis

## MQ Design

Exchange:

- `cache.delete.exchange`

Queue:

- `cache.delete.queue`

Routing key:

- `cache.delete`

Message payload:

- `cacheKey`
- `businessId`
- `type`

Listener behavior:

1. Receive message
2. If failure simulation is enabled, throw
3. Delete Redis key
4. `basicAck` on success
5. `basicNack(..., false, true)` on failure to requeue

## Error Handling

The demo intentionally focuses on one failure mode:

- MySQL update succeeds
- Redis deletion fails in the consumer
- RabbitMQ redelivers until deletion succeeds

Known boundary:

- This demo does not solve the case where MySQL update succeeds but message publishing fails

## Testing Strategy

Keep testing minimal and focused:

- Service unit test: verify `updatePrice` updates the entity and sends a delete message
- Consumer unit test: verify successful delete triggers ACK
- Consumer unit test: verify simulated failure triggers NACK with requeue

No integration test stack will be added.

## Deliverables

- `pom.xml`
- `docker-compose.yml`
- Spring Boot source tree
- `src/main/resources/application.yml`
- `src/main/resources/schema.sql`
- `src/main/resources/data.sql`
- Minimal unit tests
- `README.md`

## Implementation Notes

- Use Spring Boot 3.2.x and Java 17
- Use `StringRedisTemplate` and Jackson for cache serialization
- Use Spring AMQP manual acknowledgment mode
- Keep the code small and interview-friendly

## Non-Goals

- Exactly-once message semantics
- End-to-end transactional consistency
- Operational dashboards or metrics
- Multi-module decomposition
