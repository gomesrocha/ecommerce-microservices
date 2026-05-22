# Technical Debt

## TD-001: Dockerizar os microsserviços

Neste momento, o Docker Compose será usado apenas para infraestrutura local, como PostgreSQL e RabbitMQ.

Os microsserviços serão executados manualmente durante o desenvolvimento.

Após a implementação inicial dos serviços principais, será criada uma branch específica para Dockerfiles e inclusão dos serviços no Docker Compose.

Branch planejada:

```text
feature/dockerize-services