# 11 — Transactional Outbox

## Problema

Em sistemas distribuídos, é comum precisar:

```text
1. salvar uma alteração no banco;
2. publicar um evento no broker.
```

Exemplo:

```text
salvar pedido
publicar product.stock.reserve
```

O problema ocorre quando uma operação funciona e a outra falha.

Exemplo:

```text
pedido salvo no banco
RabbitMQ indisponível
evento não publicado
```

Isso deixa o sistema inconsistente.

## Solução: Transactional Outbox

O padrão Transactional Outbox resolve esse problema salvando o evento no banco na mesma transação da alteração principal.

Fluxo:

```text
1. salvar pedido;
2. salvar evento na tabela outbox_events;
3. commit;
4. worker lê outbox;
5. worker publica evento no RabbitMQ;
6. worker marca evento como PUBLISHED.
```

## Implementação no order-api

Tabela:

```text
orders.outbox_events
```

Campos principais:

```text
id
eventId
aggregateType
aggregateId
eventType
routingKey
payload
status
attempts
lastError
createdAt
updatedAt
publishedAt
```

## OutboxStatus

Enum:

```text
PENDING
PUBLISHED
FAILED
```

## OutboxEvent

Entidade JPA que representa evento pendente ou publicado.

### Campos importantes

#### `eventId`

Identificador único do evento.

#### `aggregateType`

Tipo do agregado.

Exemplo:

```text
Order
```

#### `aggregateId`

Id do pedido.

#### `eventType`

Tipo do evento.

Exemplo:

```text
StockReservationRequested
OrderCreated
OrderCanceled
```

#### `routingKey`

Routing key do RabbitMQ.

Exemplo:

```text
product.stock.reserve
order.created
order.canceled
```

#### `payload`

JSON do evento.

#### `status`

Status da publicação.

## OutboxEventRepository

Repository Panache.

Função principal:

### `listPending(int limit)`

Lista eventos pendentes em ordem de criação.

## OutboxService

Camada de negócio da Outbox.

### `saveEvent(...)`

Salva um evento na tabela Outbox.

Usado dentro da transação do pedido.

### `listPending(int limit)`

Retorna eventos pendentes.

### `markPublished(Long id)`

Marca evento como publicado.

### `markFailed(Long id, Throwable throwable)`

Incrementa tentativas e registra erro.

Se atingir o máximo de tentativas, marca como `FAILED`.

## OrderEventPublisher

Antes, publicava diretamente no RabbitMQ.

Agora, salva eventos na Outbox.

Funções:

### `publishOrderCreated(Order order)`

Salva evento `OrderCreated`.

### `publishOrderCanceled(Order order)`

Salva evento `OrderCanceled`.

### `publishStockReservationRequested(Order order)`

Salva evento `StockReservationRequested`.

## OutboxPublisherWorker

Worker agendado que publica eventos pendentes.

Usa:

```java
@Scheduled(every = "2s")
```

Fluxo:

```text
1. buscar eventos PENDING;
2. identificar emitter pela routingKey;
3. publicar no RabbitMQ;
4. marcar como PUBLISHED;
5. em caso de erro, marcar falha.
```

## Vantagens

- evita perder eventos;
- melhora confiabilidade;
- permite retry;
- permite auditoria;
- desacopla transação do broker.

## Limitações atuais

Ainda falta:

- lock para múltiplas instâncias;
- status PROCESSING;
- dead letter;
- painel de Outbox;
- idempotência ponta a ponta.

## Teste de falha

1. Parar RabbitMQ.
2. Criar pedido.
3. Ver evento em `orders.outbox_events`.
4. Subir RabbitMQ.
5. Worker publica evento.

## Consulta SQL

```sql
SELECT id, event_type, routing_key, status, attempts, last_error
FROM orders.outbox_events
ORDER BY id DESC
LIMIT 10;
```
