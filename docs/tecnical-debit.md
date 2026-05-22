# Technical Debt

## TD-001: Dockerizar os microsserviços

Neste momento, o Docker Compose será usado apenas para infraestrutura local, como PostgreSQL e RabbitMQ.

Os microsserviços serão executados manualmente durante o desenvolvimento.

Após a implementação inicial dos serviços principais, será criada uma branch específica para Dockerfiles e inclusão dos serviços no Docker Compose.

Branch planejada:

```text
feature/dockerize-services



## TD-004: Implementar Transactional Outbox no order-api

Atualmente o order-api publica eventos diretamente no RabbitMQ após persistir o pedido.

Para maior confiabilidade, implementar o padrão Transactional Outbox, salvando os eventos em uma tabela `outbox_events` na mesma transação do pedido e publicando-os posteriormente por um worker.