# 12 — Idempotência de Reserva de Estoque

## Problema

Em mensageria, a mesma mensagem pode ser entregue mais de uma vez.

Se o `product-api` processar duas vezes o mesmo evento de reserva, o estoque pode ser baixado duas vezes.

Exemplo:

```text
Estoque inicial: 10
Pedido reserva: 2

Primeiro processamento:
estoque = 8

Mensagem duplicada:
estoque = 6
```

Isso está errado.

## Solução

Criar uma tabela de reservas e usar uma chave lógica:

```text
orderId + productId
```

Se já existe reserva para aquele pedido e produto, o serviço não baixa o estoque novamente.

## Tabela

```text
products.stock_reservations
```

Campos:

```text
id
order_id
product_id
quantity
status
reason
created_at
updated_at
```

Índice único:

```text
order_id + product_id
```

## StockReservation

Entidade JPA da reserva.

## StockReservationStatus

Enum:

```text
RESERVED
REJECTED
```

## StockReservationRepository

Função principal:

```java
findByOrderIdAndProductId(Long orderId, Long productId)
```

Essa função garante a checagem idempotente.

## ProductService.reserveStock

Função principal da idempotência.

Fluxo:

```text
1. validar orderId e items;
2. verificar reservas existentes;
3. se todas já existem como RESERVED, retornar sucesso idempotente;
4. se alguma já existe como REJECTED, retornar rejeição;
5. validar estoque;
6. se estoque insuficiente, registrar REJECTED;
7. se estoque suficiente, baixar estoque e registrar RESERVED.
```

## StockReservationResult

Record que representa o resultado:

```text
reserved: true/false
reason: motivo
```

Métodos auxiliares:

```java
reserved(String reason)
rejected(String reason)
```

## Comportamento esperado

### Primeira mensagem

```text
Não existe reserva
  ↓
baixa estoque
  ↓
cria stock_reservations
  ↓
publica stock.reserved
```

### Mensagem duplicada

```text
Reserva já existe
  ↓
não baixa estoque
  ↓
retorna sucesso idempotente
  ↓
publica stock.reserved novamente, se necessário
```

### Estoque insuficiente

```text
não baixa estoque
  ↓
cria reserva REJECTED
  ↓
publica stock.rejected
```

### Reprocessamento de rejeição

```text
reserva REJECTED já existe
  ↓
não revalida
  ↓
retorna rejeição idempotente
```

## Por que publicar novamente stock.reserved?

Em alguns casos, o consumidor pode não ter recebido o evento de resposta.

Publicar novamente uma resposta idempotente ajuda o fluxo a continuar.

## Limitações atuais

Ainda falta:

- compensação de estoque;
- controle por eventId;
- métrica de duplicidade;
- locks mais fortes para concorrência extrema.

## Teste

Arquivo:

```text
tests/http/06-orders-stock-errors.http
```

Também é possível publicar manualmente uma mensagem duplicada pelo RabbitMQ Management API.
