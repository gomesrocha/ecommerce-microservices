# Apostila V1 — Microsserviços na Prática com Quarkus, RabbitMQ, PostgreSQL e KrakenD

Esta apostila documenta a primeira grande etapa do projeto de ecommerce baseado em microsserviços.

O objetivo é registrar, de forma didática, tudo que foi construído até aqui:

- Monorepo com múltiplos microsserviços;
- Java 21 com Quarkus;
- PostgreSQL com schema por serviço;
- RabbitMQ para comunicação assíncrona;
- Saga básica para pedido;
- Transactional Outbox no `order-api`;
- Idempotência na reserva de estoque;
- API Gateway com KrakenD;
- Autenticação JWT validada no gateway;
- JWKS exposto pelo `user-api`;
- Observabilidade com OpenTelemetry, Tempo, Prometheus e Grafana;
- Tratamento padronizado de erros;
- Suíte de testes HTTP/JSON.

## Público-alvo

Esta apostila foi pensada para estudantes, desenvolvedores e arquitetos que desejam aprender microsserviços por meio de um projeto prático e incremental.

## Pré-requisitos

Conhecimentos recomendados:

- Java básico/intermediário;
- REST APIs;
- Docker e Docker Compose;
- Git e GitHub;
- noções de banco de dados relacional;
- noções de mensageria.

## Organização dos capítulos

| Capítulo | Arquivo |
|---|---|
| 01 | `01-visao-geral.md` |
| 02 | `02-monorepo-git-flow.md` |
| 03 | `03-infraestrutura-local.md` |
| 04 | `04-user-api-auth-jwt.md` |
| 05 | `05-product-api.md` |
| 06 | `06-delivery-estimator-api.md` |
| 07 | `07-order-api.md` |
| 08 | `08-rabbitmq-eventos.md` |
| 09 | `09-saga-pedido.md` |
| 10 | `10-fraud-detector-api.md` |
| 11 | `11-transactional-outbox.md` |
| 12 | `12-idempotencia-estoque.md` |
| 13 | `13-api-gateway-krakend.md` |
| 14 | `14-jwt-gateway-jwks.md` |
| 15 | `15-observabilidade.md` |
| 16 | `16-tratamento-erros.md` |
| 17 | `17-testes-http-json.md` |
| 18 | `18-roadmap-debitos-tecnicos.md` |

## Como usar esta apostila

Clone o projeto, suba a infraestrutura com Docker Compose e siga os capítulos em ordem.

```bash
docker compose up -d --build
```

O acesso principal ao sistema deve ser feito pelo API Gateway:

```text
http://localhost:8099
```

Os microsserviços não devem ser acessados diretamente pelo host. Eles ficam disponíveis apenas na rede interna do Docker Compose.

## Convenções usadas

- `user-api`: serviço de usuários e autenticação;
- `product-api`: serviço de produtos e estoque;
- `delivery-estimator-api`: serviço de estimativa de entrega;
- `order-api`: serviço de pedidos e orquestração do fluxo;
- `fraud-detector-api`: serviço de análise de fraude;
- `api-gateway`: KrakenD;
- `ecommerce.events`: exchange principal do RabbitMQ;
- `orders`, `products`, `delivery`, `fraud`: schemas lógicos no PostgreSQL.

## Status da apostila

Esta é a versão V1, cobrindo o projeto até a suíte de testes HTTP/JSON.

Uma versão V2 pode ser criada depois com:

- testes automatizados;
- Kubernetes;
- métricas de negócio;
- dashboards Grafana;
- logs centralizados;
- Outbox nos demais serviços;
- Tribuo no `delivery-estimator-api`;
- BFF conversacional com LangChain4j.
