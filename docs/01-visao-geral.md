# 01 — Visão Geral do Projeto

## Objetivo

O objetivo do projeto é construir um sistema simples de ecommerce baseado em microsserviços, usando tecnologias modernas e práticas arquiteturais associadas a sistemas distribuídos.

O domínio escolhido é propositalmente simples: usuários, produtos, pedidos, estimativa de entrega, estoque e fraude.

Apesar da simplicidade do domínio, o projeto aplica conceitos importantes:

- API Gateway;
- comunicação REST;
- comunicação assíncrona com RabbitMQ;
- Saga;
- Transactional Outbox;
- idempotência;
- observabilidade;
- autenticação JWT;
- padronização de erros;
- testes HTTP versionados.

## Arquitetura geral

A arquitetura atual possui os seguintes componentes:

```text
Cliente / HTTP Client
  ↓
KrakenD API Gateway
  ↓
Microsserviços internos
  ├── user-api
  ├── product-api
  ├── order-api
  ├── delivery-estimator-api
  └── fraud-detector-api
  ↓
PostgreSQL + RabbitMQ
```

## Serviços

### user-api

Responsável por:

- cadastro e gestão de usuários;
- autenticação;
- geração de access token e refresh token;
- exposição de JWKS para validação do JWT no gateway.

### product-api

Responsável por:

- cadastro de produtos;
- controle de estoque;
- consumo de eventos de reserva de estoque;
- publicação de eventos de estoque reservado ou rejeitado;
- idempotência de reserva.

### delivery-estimator-api

Responsável por:

- cadastro de rotas de entrega;
- estimativa de prazo com base em origem e destino;
- fallback para prazo conservador em caso de indisponibilidade.

### order-api

Responsável por:

- criação de pedidos;
- integração com produto e entrega;
- controle de status do pedido;
- histórico de status;
- publicação de eventos via Outbox;
- consumo de eventos de estoque e fraude.

### fraud-detector-api

Responsável por:

- consumir eventos de pedido criado;
- analisar risco de fraude;
- publicar aprovação ou rejeição.

### api-gateway

Implementado com KrakenD.

Responsável por:

- expor a porta pública do sistema;
- encaminhar chamadas para microsserviços internos;
- validar JWT;
- impedir acesso direto aos microsserviços.

## Fluxo principal de pedido

```text
1. Cliente cria pedido pelo gateway.
2. order-api valida produto via product-api.
3. order-api consulta prazo no delivery-estimator-api.
4. order-api cria pedido como WAITING_STOCK.
5. order-api grava evento StockReservationRequested na Outbox.
6. worker da Outbox publica evento no RabbitMQ.
7. product-api consome evento e reserva estoque.
8. product-api publica stock.reserved ou stock.rejected.
9. order-api atualiza status.
10. Se estoque foi reservado, order-api publica order.created.
11. fraud-detector-api consome order.created.
12. fraud-detector-api publica fraud.approved ou fraud.rejected.
13. order-api atualiza pedido para CONFIRMED ou REJECTED.
```

## Principais decisões arquiteturais

### Monorepo

Todos os microsserviços ficam no mesmo repositório para facilitar o aprendizado, versionamento conjunto e execução local.

### PostgreSQL único com schemas por serviço

Embora em produção cada serviço possa ter seu próprio banco, neste projeto usamos um PostgreSQL único para simplificar o ambiente local.

Cada serviço possui seu schema:

```text
users
products
orders
delivery
fraud
```

### RabbitMQ

Usado para eventos de domínio e integração assíncrona entre serviços.

### KrakenD

Usado como API Gateway para centralizar acesso, autenticação e roteamento.

### OpenTelemetry

Usado para instrumentar os serviços e enviar traces para o Collector.

## Relação com os 12 fatores

O projeto aplica vários princípios dos 12 fatores:

- configuração por variáveis de ambiente;
- serviços independentes;
- logs para saída padrão;
- processos stateless;
- infraestrutura declarada em Docker Compose;
- separação entre build e execução;
- serviços de apoio como recursos anexados.
