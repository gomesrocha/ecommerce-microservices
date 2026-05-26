# 08 — RabbitMQ e Eventos

## Objetivo

O RabbitMQ é usado para comunicação assíncrona entre microsserviços.

Enquanto REST é usado para consultas imediatas, eventos são usados para processos distribuídos e desacoplados.

## Exchange principal

```text
ecommerce.events
```

Tipo:

```text
topic
```

## Por que usar exchange topic?

Com exchange do tipo `topic`, os eventos são roteados por routing keys.

Exemplo:

```text
product.stock.reserve
stock.reserved
stock.rejected
order.created
order.canceled
fraud.approved
fraud.rejected
```

## Filas principais

Exemplos de filas usadas:

```text
product.stock-reservation
order.stock-reserved
order.stock-rejected
fraud.order-created
order.fraud-approved
order.fraud-rejected
orders.created
orders.canceled
```

## Eventos principais

### StockReservationRequested

Publicado pelo `order-api`.

Routing key:

```text
product.stock.reserve
```

Consumido por:

```text
product-api
```

Objetivo:

```text
Solicitar reserva de estoque para um pedido.
```

### StockReserved

Publicado pelo `product-api`.

Routing key:

```text
stock.reserved
```

Consumido por:

```text
order-api
```

Objetivo:

```text
Informar que o estoque foi reservado com sucesso.
```

### StockRejected

Publicado pelo `product-api`.

Routing key:

```text
stock.rejected
```

Consumido por:

```text
order-api
```

Objetivo:

```text
Informar que não foi possível reservar estoque.
```

### OrderCreated

Publicado pelo `order-api`.

Routing key:

```text
order.created
```

Consumido por:

```text
fraud-detector-api
```

Objetivo:

```text
Solicitar análise de fraude.
```

### FraudApproved

Publicado pelo `fraud-detector-api`.

Routing key:

```text
fraud.approved
```

Consumido por:

```text
order-api
```

Objetivo:

```text
Informar que pedido foi aprovado na análise de fraude.
```

### FraudRejected

Publicado pelo `fraud-detector-api`.

Routing key:

```text
fraud.rejected
```

Consumido por:

```text
order-api
```

Objetivo:

```text
Informar que pedido foi rejeitado na análise de fraude.
```

## Estrutura geral de evento

Um evento geralmente possui:

```text
eventId
eventType
sourceService
occurredAt
payload
```

## Por que eventId é importante?

O `eventId` permite:

- rastreabilidade;
- idempotência;
- correlação;
- auditoria;
- evitar processamento duplicado.

## Configuração no Quarkus

O projeto usa SmallRye Reactive Messaging com RabbitMQ.

Exemplo conceitual de canal outgoing:

```properties
mp.messaging.outgoing.order-created-out.connector=smallrye-rabbitmq
mp.messaging.outgoing.order-created-out.exchange.name=ecommerce.events
mp.messaging.outgoing.order-created-out.exchange.type=topic
mp.messaging.outgoing.order-created-out.default-routing-key=order.created
```

Exemplo conceitual de canal incoming:

```properties
mp.messaging.incoming.fraud-approved-in.connector=smallrye-rabbitmq
mp.messaging.incoming.fraud-approved-in.queue.name=order.fraud-approved
```

## Consumers

Um consumer recebe mensagens de um canal.

Exemplo conceitual:

```java
@Incoming("stock-reserved-in")
public CompletionStage<Void> consume(Message<JsonObject> message) {
    // processar evento
    return message.ack();
}
```

## Publishers

Um publisher envia eventos para um canal.

Exemplo conceitual:

```java
@Channel("fraud-approved-out")
Emitter<JsonObject> emitter;
```

## Painel RabbitMQ

Acesse:

```text
http://localhost:15672
```

Credenciais:

```text
ecommerce / ecommerce
```

## Comandos úteis

Listar filas:

```bash
docker exec -it ecommerce-rabbitmq rabbitmqctl list_queues name messages
```

Listar bindings:

```bash
docker exec -it ecommerce-rabbitmq rabbitmqctl list_bindings source_name destination_name routing_key
```

## Cuidados

Mensageria assíncrona exige atenção a:

- mensagens duplicadas;
- falhas temporárias;
- retries;
- dead letter;
- idempotência;
- observabilidade;
- versionamento de contratos.
