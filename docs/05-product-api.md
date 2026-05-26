# 05 — Product API

## Responsabilidade

O `product-api` é responsável por:

- cadastrar produtos;
- listar produtos;
- atualizar produtos;
- desativar produtos;
- controlar estoque;
- consumir eventos de reserva de estoque;
- publicar eventos de estoque reservado ou rejeitado;
- garantir idempotência na reserva de estoque.

## Estrutura principal

```text
product-api/
└── src/main/java/br/com/ecommerce/
    ├── domain/
    ├── dto/
    ├── repository/
    ├── service/
    ├── resource/
    └── messaging/
```

## Product

Entidade principal do domínio de produto.

Campos comuns:

```text
id
name
description
sku
price
stockQuantity
originState
status
createdAt
updatedAt
```

### Funções importantes

#### `isActive()`

Indica se o produto está ativo.

Usado na validação de pedido e reserva de estoque.

#### `hasStock(quantity)`

Verifica se há estoque suficiente para uma quantidade solicitada.

Exemplo conceitual:

```java
return stockQuantity >= quantity;
```

## ProductStatus

Enum que representa o status do produto.

Valores típicos:

```text
ACTIVE
INACTIVE
```

## DTOs

### CreateProductRequest

Usado para criar produto.

Campos:

```text
name
description
sku
price
stockQuantity
originState
```

### UpdateProductRequest

Usado para atualizar produto.

Campos comuns:

```text
name
description
price
stockQuantity
originState
status
```

### ProductResponse

DTO de saída.

Evita expor diretamente a entidade JPA.

Normalmente possui método:

```java
fromEntity(Product product)
```

Esse método converte a entidade em resposta da API.

## ProductRepository

Repository usando Panache.

Responsabilidades:

- persistir produto;
- buscar por id;
- buscar por SKU;
- listar todos;
- aplicar filtros futuros.

Exemplo conceitual:

```java
public Optional<Product> findBySku(String sku) {
    return find("sku", sku).firstResultOptional();
}
```

## ProductService

Camada de regra de negócio.

### `create(CreateProductRequest request)`

Cria um novo produto.

Responsabilidades:

```text
1. validar SKU único;
2. criar entidade Product;
3. persistir no banco;
4. retornar ProductResponse.
```

### `update(Long id, UpdateProductRequest request)`

Atualiza produto existente.

### `deactivate(Long id)`

Desativa produto.

Importante: desativar evita que o produto seja usado em novos pedidos.

### `findById(Long id)`

Busca produto por id.

Se não existir, lança exceção.

### `reserveStock(Long orderId, List<StockReservationItem> items)`

Função adicionada para reserva de estoque idempotente.

Responsabilidades:

```text
1. verificar se já existe reserva para orderId + productId;
2. se já existe e está RESERVED, não baixar estoque novamente;
3. se já existe e está REJECTED, retornar rejeição novamente;
4. se não existe, validar estoque;
5. baixar estoque;
6. registrar reserva;
7. retornar resultado.
```

## StockReservation

Entidade criada para controlar idempotência.

Campos:

```text
id
orderId
productId
quantity
status
reason
createdAt
updatedAt
```

## StockReservationStatus

Enum:

```text
RESERVED
REJECTED
```

## StockReservationRepository

Repository responsável por consultar reservas.

Funções importantes:

### `findByOrderIdAndProductId(Long orderId, Long productId)`

Busca reserva única por pedido e produto.

É a chave lógica da idempotência.

### `listByOrderId(Long orderId)`

Lista reservas de um pedido.

## StockReservationResult

Record usado para retornar resultado da reserva.

Campos:

```text
reserved
reason
```

Métodos auxiliares:

```java
reserved(String reason)
rejected(String reason)
```

## ProductResource

Resource REST do serviço.

Endpoints comuns:

```text
POST /products
GET /products
GET /products/{id}
PUT /products/{id}
PATCH /products/{id}/deactivate
```

## Messaging

### StockReservationConsumer

Consumidor do evento:

```text
product.stock.reserve
```

Responsabilidades:

```text
1. receber mensagem do RabbitMQ;
2. converter payload para StockReservationRequestedEvent;
3. chamar ProductService.reserveStock;
4. publicar stock.reserved ou stock.rejected;
5. confirmar ack da mensagem.
```

### StockEventPublisher

Publica eventos de resultado do estoque.

Eventos:

```text
stock.reserved
stock.rejected
```

## Conceito: idempotência

Idempotência significa que executar a mesma operação várias vezes produz o mesmo efeito final.

No projeto:

```text
mesmo orderId + productId
```

não pode baixar estoque duas vezes.

## Banco de dados

Schema:

```text
products
```

Tabela principal:

```text
products.products
```

Tabela de reserva:

```text
products.stock_reservations
```

## Testes HTTP

Arquivo relacionado:

```text
tests/http/02-products.http
tests/http/06-orders-stock-errors.http
```
