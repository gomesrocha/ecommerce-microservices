# 18 — Roadmap e Débitos Técnicos

## Objetivo

Este capítulo resume o que já foi concluído e o que ainda falta para evoluir o projeto.

## Concluído na V1

```text
Monorepo
Git Flow por feature
user-api com JWT
product-api
delivery-estimator-api
order-api
fraud-detector-api
RabbitMQ
Saga básica
Transactional Outbox no order-api
Idempotência de estoque
Docker Compose completo
KrakenD API Gateway
Acesso apenas pelo Gateway
JWT validado no KrakenD
JWKS no user-api
OpenTelemetry + Tempo + Prometheus + Grafana
Tratamento padronizado de erros no order-api
Suíte HTTP/JSON
```

## Próximas features recomendadas

### 1. automated-tests

Criar testes automatizados.

```text
feature/automated-tests
```

Escopo:

- Quarkus Test;
- JUnit;
- REST Assured;
- testes de resources;
- testes de services;
- testes de validação;
- testes de autenticação.

### 2. database-migrations-all-services

Migrar todos os serviços para Flyway/Liquibase.

```text
feature/database-migrations-all-services
```

### 3. fraud-outbox-pattern

Implementar Outbox no `fraud-detector-api`.

```text
feature/fraud-outbox-pattern
```

### 4. event-idempotency

Criar idempotência geral por `eventId`.

```text
feature/event-idempotency
```

### 5. stock-compensation

Devolver estoque em cancelamento ou fraude rejeitada.

```text
feature/stock-compensation
```

### 6. order-saga-compensation

Adicionar compensações e timeouts na Saga.

```text
feature/order-saga-compensation
```

### 7. business-metrics

Adicionar métricas de negócio.

```text
feature/business-metrics
```

Exemplos:

```text
pedidos criados
pedidos confirmados
pedidos rejeitados
fallbacks de entrega
outbox pendente
```

### 8. observability-dashboards

Criar dashboards Grafana.

```text
feature/observability-dashboards
```

### 9. logs-centralized-loki

Adicionar Loki.

```text
feature/logs-centralized-loki
```

### 10. delivery-estimator-tribuo

Refatorar estimativa de entrega para usar ML com Tribuo.

```text
feature/delivery-estimator-tribuo
```

### 11. ai-chat-bff-langchain4j

Criar BFF conversacional com LangChain4j.

```text
feature/ai-chat-bff-langchain4j
```

## Roadmap por tema

### Confiabilidade

```text
fraud-outbox-pattern
outbox-other-services
event-idempotency
stock-compensation
order-saga-compensation
```

### Qualidade

```text
automated-tests
api-event-contracts
architecture-docs
architecture-decision-records
```

### Produção

```text
database-migrations-all-services
service-to-service-security
kubernetes-deployment
native-dockerfiles
krakend-hardening
```

### IA e Dados

```text
olist-delivery-baseline
delivery-data-pipeline
delivery-estimator-tribuo
ai-chat-bff-langchain4j
```

### Observabilidade

```text
business-metrics
observability-dashboards
logs-centralized-loki
```

## Considerações finais

A V1 do projeto já demonstra um fluxo completo e rico de microsserviços.

O principal ganho didático é que cada etapa foi construída incrementalmente:

```text
serviço simples
  ↓
integração REST
  ↓
eventos
  ↓
Saga
  ↓
Outbox
  ↓
idempotência
  ↓
Gateway
  ↓
JWT
  ↓
Observabilidade
  ↓
testes HTTP
```

A próxima etapa natural é transformar validações manuais em testes automatizados.
