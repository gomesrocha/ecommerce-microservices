# 09 — Saga do Pedido

## O que é Saga?

Saga é um padrão para coordenar transações distribuídas em sistemas de microsserviços.

Em vez de usar uma transação única envolvendo vários serviços, cada serviço executa sua transação local e publica eventos para continuar o fluxo.

## Por que usar Saga?

Em microsserviços, cada serviço deve ser autônomo.

Não é recomendado depender de uma transação distribuída global entre:

```text
order-api
product-api
fraud-detector-api
```

A Saga permite coordenar o processo por eventos.

## Saga neste projeto

Fluxo simplificado:

```text
CREATED
  ↓
WAITING_STOCK
  ↓
WAITING_FRAUD
  ↓
CONFIRMED
```

Fluxo de rejeição por estoque:

```text
CREATED
  ↓
WAITING_STOCK
  ↓
REJECTED
```

Fluxo de rejeição por fraude:

```text
CREATED
  ↓
WAITING_STOCK
  ↓
WAITING_FRAUD
  ↓
REJECTED
```

## Estados do pedido

### CREATED

Estado inicial conceitual.

### WAITING_STOCK

Pedido criado e aguardando reserva de estoque.

### WAITING_FRAUD

Estoque reservado e aguardando análise de fraude.

### CONFIRMED

Pedido confirmado.

### CANCELED

Pedido cancelado.

### REJECTED

Pedido rejeitado por estoque, fraude ou outra regra.

## OrderStatusHistory

Cada mudança de status é registrada.

Campos:

```text
orderId
previousStatus
newStatus
triggerEvent
reason
createdAt
```

## Triggers de mudança

Exemplos:

```text
ORDER_CREATED
STOCK_RESERVED
STOCK_REJECTED
FRAUD_APPROVED
FRAUD_REJECTED
ORDER_CANCELED
```

## Por que registrar histórico?

O histórico permite:

- auditoria;
- rastreabilidade;
- debug;
- explicação do fluxo para o cliente;
- monitoramento de pedidos presos.

## Endpoint de histórico

```text
GET /orders/{id}/history
```

Via gateway:

```text
GET /api/orders/{id}/history
```

## Função `changeStatus`

A função de mudança de status deve:

```text
1. validar transição;
2. alterar status;
3. registrar histórico;
4. atualizar data de alteração;
5. salvar motivo.
```

## Transições válidas

Exemplos:

```text
CREATED -> WAITING_STOCK
WAITING_STOCK -> WAITING_FRAUD
WAITING_STOCK -> REJECTED
WAITING_FRAUD -> CONFIRMED
WAITING_FRAUD -> REJECTED
CONFIRMED -> CANCELED
```

## Saga orquestrada ou coreografada?

A implementação atual é uma combinação:

- o `order-api` mantém o estado e decide transições;
- os outros serviços respondem por eventos.

Isso se aproxima de uma Saga orquestrada pelo `order-api`, mas com comunicação assíncrona.

## O que ainda falta?

A Saga atual ainda não possui:

- compensação completa;
- timeout por etapa;
- dead letter;
- tabela explícita de instância de Saga;
- painel de monitoramento;
- retry controlado por etapa.

## Compensação futura

Exemplo de compensação:

```text
Pedido rejeitado por fraude
  ↓
order-api publica stock.release.requested
  ↓
product-api devolve estoque
  ↓
product-api publica stock.released
```

## Testes HTTP

Arquivos relacionados:

```text
tests/http/04-orders-success.http
tests/http/06-orders-stock-errors.http
tests/http/07-orders-fraud-flow.http
```
