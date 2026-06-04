# Testes de Performance com k6

Esta pasta contém scripts de teste de performance para o ecommerce-microservices.

## Requisitos

É possível executar com k6 instalado localmente ou via Docker.

## Teste disponível

- `k6/order-flow.js`: executa login, criação de produto, criação de pedido e consulta do pedido.

## Executar com k6 local

```bash
k6 run tests/performance/k6/order-flow.js
```
