# Ecommerce Microservices

Projeto de estudo de microsserviços usando Java, Quarkus, PostgreSQL e RabbitMQ.

## Serviços

| Serviço | Tecnologia | Responsabilidade |
|---|---|---|
| user-api | Java + Quarkus | Usuários, autenticação e JWT |
| product-api | Java + Quarkus | Produtos e estoque |
| shopping-api | Java + Quarkus | Carrinho e pedidos |
| estimated-delivery-api | Python + FastAPI | Predição de tempo de entrega |
| fraud-detector-api | Python ou Java | Análise de fraude |

## Infra local

- PostgreSQL
- RabbitMQ Management
- Docker Compose

## Subir ambiente

```bash
cp .env.example .env
docker compose up -d --build