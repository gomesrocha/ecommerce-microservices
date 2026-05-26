# 03 — Infraestrutura Local com Docker Compose

## Objetivo

A infraestrutura local permite executar o sistema completo em ambiente de desenvolvimento usando Docker Compose.

Componentes principais:

```text
PostgreSQL
RabbitMQ
KrakenD
OpenTelemetry Collector
Tempo
Prometheus
Grafana
```

## PostgreSQL

O PostgreSQL é usado como banco relacional do projeto.

Embora microsserviços normalmente tenham bancos independentes, neste ambiente local usamos um único PostgreSQL com schemas separados.

### Serviço no Docker Compose

```yaml
postgres:
  image: postgres:16
  container_name: ecommerce-postgres
  environment:
    POSTGRES_DB: ecommerce
    POSTGRES_USER: ecommerce
    POSTGRES_PASSWORD: ecommerce
  ports:
    - "5432:5432"
```

### Por que manter a porta 5432 exposta?

Para desenvolvimento local, é útil acessar o banco com:

- DataGrip;
- DBeaver;
- psql;
- ferramentas de administração.

Em produção, essa porta não deve ficar pública.

## RabbitMQ

O RabbitMQ é usado para comunicação assíncrona entre serviços.

### Portas

```text
5672  -> protocolo AMQP
15672 -> painel web de administração
```

### Painel

```text
http://localhost:15672
```

Usuário e senha:

```text
ecommerce / ecommerce
```

## KrakenD

O KrakenD é o API Gateway do projeto.

É o único ponto de entrada HTTP dos microsserviços.

```text
http://localhost:8099
```

Os microsserviços não possuem mais `ports` publicados no host. Eles usam apenas `expose`, ficando acessíveis somente pela rede interna do Compose.

## Expose vs Ports

### `ports`

Publica uma porta no host:

```yaml
ports:
  - "8096:8080"
```

Permite acesso via:

```text
http://localhost:8096
```

### `expose`

Disponibiliza a porta apenas para outros containers da mesma rede:

```yaml
expose:
  - "8080"
```

Isso impede acesso direto pelo host.

## OpenTelemetry Collector

Recebe traces dos microsserviços e encaminha para o Tempo.

Portas:

```text
4317 -> OTLP gRPC
4318 -> OTLP HTTP
8888 -> métricas do collector
```

## Tempo

Armazena traces distribuídos.

```text
http://localhost:3200
```

## Prometheus

Coleta métricas dos serviços.

```text
http://localhost:9090
```

## Grafana

Visualiza métricas e traces.

```text
http://localhost:3000
```

Credenciais locais:

```text
admin / admin
```

## Rede Docker

Todos os containers usam a rede:

```text
ecommerce-net
```

Dentro dessa rede, os serviços se comunicam pelo nome:

```text
user-api:8080
product-api:8080
order-api:8080
delivery-estimator-api:8080
fraud-detector-api:8080
rabbitmq:5672
postgres:5432
```

## Healthchecks

PostgreSQL e RabbitMQ possuem healthcheck.

Isso evita que serviços dependentes subam antes da infraestrutura estar pronta.

## Comandos úteis

Subir tudo:

```bash
docker compose up -d --build
```

Ver containers:

```bash
docker compose ps
```

Ver logs:

```bash
docker compose logs -f order-api
```

Recriar um serviço:

```bash
docker compose up -d --build order-api
```

Recriar gateway:

```bash
docker compose up -d --force-recreate api-gateway
```

Parar:

```bash
docker compose down
```

Parar sem apagar volumes:

```bash
docker compose down
```

Parar apagando volumes:

```bash
docker compose down -v
```

Use `-v` com cuidado, pois apaga dados do banco e RabbitMQ.
