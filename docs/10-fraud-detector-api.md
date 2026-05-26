# 10 — Fraud Detector API

## Responsabilidade

O `fraud-detector-api` é responsável por analisar pedidos e decidir se há risco de fraude.

Na versão atual, a análise é simplificada e baseada em regras.

## Fluxo

```text
order-api publica order.created
  ↓
fraud-detector-api consome
  ↓
fraud-detector-api calcula score
  ↓
fraud-detector-api salva análise
  ↓
fraud-detector-api publica fraud.approved ou fraud.rejected
  ↓
order-api atualiza pedido
```

## Entidade de análise

Uma análise de fraude normalmente possui:

```text
id
orderId
userId
totalAmount
riskScore
status
reason
createdAt
```

## Eventos consumidos

### OrderCreated

Evento recebido para iniciar análise.

Routing key:

```text
order.created
```

## Eventos publicados

### FraudApproved

Publicado quando o pedido é aprovado.

Routing key:

```text
fraud.approved
```

### FraudRejected

Publicado quando o pedido é rejeitado.

Routing key:

```text
fraud.rejected
```

## Regras de fraude

A regra inicial pode considerar:

- valor total do pedido;
- quantidade de itens;
- usuário;
- histórico;
- estado de destino.

No projeto atual, usamos uma regra simples para fins didáticos.

Exemplo conceitual:

```text
se totalAmount > limite:
    rejeitar
senão:
    aprovar
```

## FraudEventPublisher

Classe responsável por publicar eventos de resultado da análise.

Funções típicas:

### `publishApproved(...)`

Publica evento `fraud.approved`.

### `publishRejected(...)`

Publica evento `fraud.rejected`.

## FraudAnalysisService

Camada de regra de negócio.

Responsabilidades:

```text
1. receber evento de pedido;
2. calcular score;
3. gerar motivo;
4. salvar análise;
5. publicar resultado.
```

## FraudAnalysisResource

Permite consultar análises.

Endpoints:

```text
GET /fraud-analyses
GET /fraud-analyses/order/{orderId}
```

Via gateway:

```text
GET /api/fraud-analyses
GET /api/fraud-analyses/order/{orderId}
```

## Limitação atual

O `fraud-detector-api` ainda publica eventos diretamente no RabbitMQ.

Débito técnico:

```text
feature/fraud-outbox-pattern
```

## Evoluções futuras

- Transactional Outbox no fraud-detector-api;
- idempotência por eventId;
- regras mais elaboradas;
- modelo de machine learning;
- explicabilidade da decisão;
- métricas de fraude;
- dashboard de fraude.

## Testes HTTP

Arquivo relacionado:

```text
tests/http/07-orders-fraud-flow.http
```
