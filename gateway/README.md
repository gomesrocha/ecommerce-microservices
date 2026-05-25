# API Gateway - KrakenD

Este diretório contém a configuração do KrakenD usada como API Gateway do monorepo de ecommerce.

## Porta

O gateway expõe as APIs na porta:

```text
http://localhost:8099
```

## Serviços internos

O KrakenD encaminha chamadas para os serviços internos na rede Docker:

product-api:8080
order-api:8080
delivery-estimator-api:8080
fraud-detector-api:8080

## Rotas principais
GET    /api/products
POST   /api/products
GET    /api/products/{id}
PUT    /api/products/{id}
PATCH  /api/products/{id}/deactivate

GET    /api/orders
POST   /api/orders
GET    /api/orders/{id}
GET    /api/orders/{id}/history
PATCH  /api/orders/{id}/cancel

POST   /api/delivery-estimates/estimate
GET    /api/delivery-estimates/routes
PUT    /api/delivery-estimates/routes

GET    /api/fraud-analyses
GET    /api/fraud-analyses/order/{orderId}
