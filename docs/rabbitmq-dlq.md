# RabbitMQ Dead Letter Queues

Esta documentação descreve o padrão de **Dead Letter Queue (DLQ)** usado no projeto `ecommerce-microservices`.

## Objetivo

Garantir que mensagens com falha de processamento não sejam perdidas e possam ser analisadas ou reprocessadas posteriormente.

No fluxo atual, os microsserviços se comunicam por eventos usando RabbitMQ. Quando um consumer falha ao processar uma mensagem, a mensagem pode ser rejeitada e enviada para uma fila de erro específica, chamada **Dead Letter Queue**.

## Exchange principal

Os eventos de negócio trafegam pelo exchange principal:

```text
ecommerce.events
```

Exemplos de routing keys:

```text
order.created
product.stock.reserve
stock.reserved
stock.rejected
fraud.approved
fraud.rejected
payment.requested
payment.approved
payment.rejected
notifications.requested
```

## Dead Letter Exchange

As mensagens rejeitadas são encaminhadas para o exchange de dead letter:

```text
ecommerce.dlx
```

Esse exchange é do tipo:

```text
topic
```

## Policy aplicada

A policy `ecommerce-dlx` aplica o Dead Letter Exchange nas filas consumidoras principais.

Filas contempladas:

```text
product.stock-reservation
order.stock-reserved
order.stock-rejected
fraud.order-created
order.fraud-approved
order.fraud-rejected
payment.requested
order.payment-approved
order.payment-rejected
notifications.requested
```

A policy usada é:

```bash
docker exec -it ecommerce-rabbitmq rabbitmqctl set_policy ecommerce-dlx \
  "^(product\.stock-reservation|order\.stock-reserved|order\.stock-rejected|fraud\.order-created|order\.fraud-approved|order\.fraud-rejected|payment\.requested|order\.payment-approved|order\.payment-rejected|notifications\.requested)$" \
  '{"dead-letter-exchange":"ecommerce.dlx"}' \
  --apply-to queues \
  --priority 10
```

## Mapeamento das DLQs

| Fila original | Routing key original | DLQ |
|---|---|---|
| `product.stock-reservation` | `product.stock.reserve` | `product.stock-reservation.dlq` |
| `order.stock-reserved` | `stock.reserved` | `order.stock-reserved.dlq` |
| `order.stock-rejected` | `stock.rejected` | `order.stock-rejected.dlq` |
| `fraud.order-created` | `order.created` | `fraud.order-created.dlq` |
| `order.fraud-approved` | `fraud.approved` | `order.fraud-approved.dlq` |
| `order.fraud-rejected` | `fraud.rejected` | `order.fraud-rejected.dlq` |
| `payment.requested` | `payment.requested` | `payment.requested.dlq` |
| `order.payment-approved` | `payment.approved` | `order.payment-approved.dlq` |
| `order.payment-rejected` | `payment.rejected` | `order.payment-rejected.dlq` |
| `notifications.requested` | `notifications.requested` | `notifications.requested.dlq` |

## Aplicar configuração em ambiente existente

Para aplicar a configuração de DLQ em um ambiente RabbitMQ já existente, execute:

```bash
./infra/rabbitmq/setup-dlq.sh
```

Esse script cria:

```text
ecommerce.dlx
filas .dlq
bindings entre ecommerce.dlx e as DLQs
policy ecommerce-dlx
```

## Verificar policies

```bash
docker exec -it ecommerce-rabbitmq rabbitmqctl list_policies
```

Saída esperada:

```text
vhost  name           pattern        apply-to  definition                                 priority
/      ecommerce-dlx  ...            queues    {"dead-letter-exchange":"ecommerce.dlx"}  10
```

## Verificar filas

```bash
docker exec -it ecommerce-rabbitmq rabbitmqadmin \
  --username=ecommerce \
  --password=ecommerce \
  list queues name messages messages_ready messages_unacknowledged
```

As filas `.dlq` devem aparecer na lista, por exemplo:

```text
payment.requested.dlq
notifications.requested.dlq
order.fraud-approved.dlq
```

## Teste de DLQ

Para testar a DLQ, publique uma mensagem inválida em `payment.requested`.

```bash
docker exec -it ecommerce-rabbitmq rabbitmqadmin \
  --username=ecommerce \
  --password=ecommerce \
  publish \
  exchange=ecommerce.events \
  routing_key=payment.requested \
  payload='{
    "eventId": "dlq-test-payment-001",
    "orderId": null,
    "userId": null,
    "amount": null
  }'
```

Como o `payment-service` exige `orderId`, `userId` e `amount`, essa mensagem deve falhar no consumer e ser enviada para:

```text
payment.requested.dlq
```

## Consultar filas após o teste

```bash
docker exec -it ecommerce-rabbitmq rabbitmqadmin \
  --username=ecommerce \
  --password=ecommerce \
  list queues name messages messages_ready messages_unacknowledged
```

Resultado esperado:

```text
payment.requested      0
payment.requested.dlq  1
```

## Ver logs do serviço que falhou

```bash
docker compose logs --since=2m payment-service | grep -Ei "Falha|PaymentRequested|ERROR|Exception"
```

Exemplo esperado:

```text
Falha ao processar PaymentRequested
BadRequestException: orderId é obrigatório
```

## Ler mensagem da DLQ sem remover

Use `ackmode=ack_requeue_true` para visualizar a mensagem sem removê-la da DLQ.

```bash
docker exec -it ecommerce-rabbitmq rabbitmqadmin \
  --username=ecommerce \
  --password=ecommerce \
  get queue=payment.requested.dlq ackmode=ack_requeue_true count=1
```

## Ler mensagem da DLQ removendo

Use `ackmode=ack_requeue_false` para visualizar e remover a mensagem da DLQ.

```bash
docker exec -it ecommerce-rabbitmq rabbitmqadmin \
  --username=ecommerce \
  --password=ecommerce \
  get queue=payment.requested.dlq ackmode=ack_requeue_false count=1
```

## Limpar mensagem de teste

Caso queira limpar a DLQ de teste:

```bash
docker exec -it ecommerce-rabbitmq rabbitmqadmin \
  --username=ecommerce \
  --password=ecommerce \
  purge queue name=payment.requested.dlq
```

## Validar fluxo normal após DLQ

Depois de configurar DLQ, é importante garantir que o fluxo normal do ecommerce continua funcionando.

### Gerar token

```bash
TOKEN=$(curl -s -X POST http://localhost:8099/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.access_token')
```

### Criar produto

```bash
PRODUCT_ID=$(curl -s -X POST http://localhost:8099/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Produto Teste DLQ",
    "description": "Produto para validar fluxo após DLQ",
    "sku": "DLQ-FLOW-001",
    "price": 100.00,
    "stockQuantity": 10,
    "originState": "SP"
  }' | jq -r '.id')

echo $PRODUCT_ID
```

### Criar pedido

```bash
ORDER_ID=$(curl -s -X POST http://localhost:8099/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"userId\": 1,
    \"customerState\": \"SE\",
    \"items\": [
      {
        \"productId\": $PRODUCT_ID,
        \"quantity\": 1
      }
    ]
  }" | jq -r '.id')

echo $ORDER_ID
```

### Consultar pedido

```bash
sleep 15

curl -s http://localhost:8099/api/orders/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN" | jq
```

Resultado esperado:

```text
status = CONFIRMED
paymentStatus = APPROVED
```

## Verificar filas depois do fluxo normal

```bash
docker exec -it ecommerce-rabbitmq rabbitmqadmin \
  --username=ecommerce \
  --password=ecommerce \
  list queues name messages messages_ready messages_unacknowledged
```

As filas principais devem ficar sem mensagens pendentes. As filas DLQ também devem ficar vazias, exceto se ainda houver mensagens de teste.

## Definitions

A configuração também deve estar versionada em:

```text
infra/rabbitmq/definitions.json
```

O arquivo deve conter:

```text
exchange ecommerce.dlx
filas .dlq
bindings da DLQ
policy ecommerce-dlx
```

Isso garante que a topologia do RabbitMQ seja reprodutível em uma nova máquina ou ambiente de aula.

## Observações

- O uso de policy evita hardcode de `x-dead-letter-exchange` diretamente nas filas.
- Filas já existentes não precisam ser apagadas para receber a policy.
- Caso o ambiente seja recriado do zero, o `definitions.json` deve criar a topologia completa.
- Caso o ambiente já exista, o script `setup-dlq.sh` pode ser usado para aplicar a configuração sem remover volumes.
