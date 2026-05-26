# 07 — Order API

## Responsabilidade

O `order-api` é o serviço central do fluxo de pedido.

Ele coordena:

- validação de produtos;
- cálculo de valor total;
- estimativa de entrega;
- criação do pedido;
- transições de status;
- publicação de eventos via Outbox;
- consumo de eventos de estoque e fraude.

## Entidades principais

### Order

Representa o pedido.

Campos principais:

```text
id
userId
customerState
status
totalAmount
minDeliveryDays
estimatedDeliveryDays
maxDeliveryDays
deliverySource
deliveryModelVersion
fraudRiskScore
fraudReason
stockReason
items
createdAt
updatedAt
```

### OrderItem

Representa um item do pedido.

Campos:

```text
id
productId
productName
productSku
quantity
unitPrice
totalPrice
originState
```

### OrderStatus

Enum de status do pedido.

Valores atuais:

```text
CREATED
WAITING_STOCK
WAITING_FRAUD
CONFIRMED
CANCELED
REJECTED
```

### OrderStatusHistory

Representa o histórico de mudanças de status.

Campos:

```text
id
orderId
previousStatus
newStatus
triggerEvent
reason
createdAt
```

## DTOs

### CreateOrderRequest

Entrada para criar pedido.

Campos:

```text
userId
customerState
items
```

Validações:

```text
userId obrigatório
customerState obrigatório
customerState com 2 letras
items não pode ser vazio
```

### CreateOrderItemRequest

Campos:

```text
productId
quantity
```

Validações:

```text
productId obrigatório
quantity maior que zero
```

### OrderResponse

Resposta da API de pedidos.

Possui método:

```java
fromEntity(Order order)
```

Responsável por converter entidade em DTO.

### OrderItemResponse

Converte `OrderItem` em resposta.

### OrderStatusHistoryResponse

Converte histórico de status em resposta.

## OrderResource

Endpoints principais:

```text
POST /orders
GET /orders
GET /orders/{id}
GET /orders/{id}/history
PATCH /orders/{id}/cancel
```

### `create(@Valid CreateOrderRequest request)`

Cria pedido.

O `@Valid` aciona Bean Validation.

### `listAll(@QueryParam("userId") Long userId)`

Lista todos os pedidos ou filtra por usuário.

### `findById(Long id)`

Consulta pedido por id.

### `listStatusHistory(Long id)`

Retorna histórico de status.

### `cancel(Long id)`

Cancela pedido, quando permitido.

## OrderService

Camada de regra de negócio.

### `create(CreateOrderRequest request)`

Função mais importante do serviço.

Fluxo conceitual:

```text
1. recebe request;
2. para cada item, busca produto;
3. valida produto;
4. cria OrderItem;
5. soma total;
6. consulta estimativa de entrega;
7. cria Order;
8. define status WAITING_STOCK;
9. registra histórico;
10. salva evento StockReservationRequested na Outbox.
```

### `findProductOrThrow(Long productId)`

Busca produto via `ProductCatalogGateway`.

Responsabilidades:

```text
1. chamar product-api;
2. se produto não existir, lançar BadRequestException;
3. se product-api estiver indisponível, lançar ServiceUnavailableException.
```

### `validateProductForOrder(...)`

Valida se produto pode entrar no pedido.

Critérios:

```text
produto ativo
quantidade válida
preço válido
estoque inicial coerente
```

### `createOrderItem(...)`

Cria item do pedido a partir do produto retornado pelo `product-api`.

### `changeStatus(...)`

Altera status do pedido e registra histórico.

### `cancel(Long id)`

Cancela pedido.

Em uma evolução futura, se o estoque já estiver reservado, deve publicar evento de compensação.

## ProductCatalogGateway

Classe criada para resiliência na chamada ao `product-api`.

Usa:

```text
@Timeout
@Retry
@CircuitBreaker
@Fallback
```

Decisão:

```text
produto inexistente -> erro de negócio
product-api indisponível -> 503
```

## DeliveryEstimatorGateway

Classe criada para resiliência na chamada ao `delivery-estimator-api`.

Decisão:

```text
delivery-estimator-api indisponível -> usar fallback conservador
```

Fallback:

```text
minDays = 7
estimatedDays = 10
maxDays = 15
source = FALLBACK_RESILIENCE
modelVersion = fallback-resilience-v1
```

## Integração assíncrona

O `order-api` publica eventos via Outbox e consome eventos de:

```text
stock.reserved
stock.rejected
fraud.approved
fraud.rejected
```

## Testes HTTP

Arquivos relacionados:

```text
tests/http/04-orders-success.http
tests/http/05-orders-validation-errors.http
tests/http/06-orders-stock-errors.http
tests/http/07-orders-fraud-flow.http
```
