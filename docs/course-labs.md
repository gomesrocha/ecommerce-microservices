# Laboratórios Práticos — Ecommerce Microservices

## Laboratório 01 — Subindo o ambiente

### Objetivo

Subir toda a stack local do ecommerce.

### Passos

```bash
docker compose up -d --build
docker compose ps
```

### Validação

Acesse:

- Grafana: http://localhost:3000
- RabbitMQ: http://localhost:15672
- Mailpit: http://localhost:8025
- API Gateway: http://localhost:8099

---

## Laboratório 02 — Login e autenticação JWT

### Objetivo

Autenticar no sistema e usar o token nas chamadas protegidas.

```bash
TOKEN=$(curl -s -X POST http://localhost:8099/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.access_token')

echo $TOKEN
```

### Validação

```bash
curl -s http://localhost:8099/api/products \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## Laboratório 03 — Criando produto

### Objetivo

Criar um produto para uso no fluxo de pedido.

```bash
PRODUCT_ID=$(curl -s -X POST http://localhost:8099/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Produto Laboratório",
    "description": "Produto criado no laboratório",
    "sku": "LAB-001",
    "price": 100.00,
    "stockQuantity": 10,
    "originState": "SP"
  }' | jq -r '.id')

echo $PRODUCT_ID
```

---

## Laboratório 04 — Estimando entrega com ML

### Objetivo

Validar o modelo de entrega.

```bash
curl -s -X POST http://localhost:8099/api/delivery-estimates/estimate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "originState": "SP",
    "destinationState": "SE",
    "totalItems": 1
  }' | jq
```

### Resultado esperado

```text
source = TRIBUO_MODEL
modelVersion = delivery-tribuo-v1
```

---

## Laboratório 05 — Criando pedido e acompanhando Saga

### Objetivo

Criar um pedido e acompanhar a orquestração via eventos.

```bash
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8099/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: lab-saga-001" \
  -d "{
    \"userId\": 1,
    \"customerState\": \"SE\",
    \"items\": [
      {
        \"productId\": $PRODUCT_ID,
        \"quantity\": 1
      }
    ]
  }")

echo "$ORDER_RESPONSE" | jq

ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.id')
```

Aguarde:

```bash
sleep 15
```

Consulte:

```bash
curl -s http://localhost:8099/api/orders/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## Laboratório 06 — Consultando Outbox

```bash
docker exec -it ecommerce-postgres psql -U ecommerce -d ecommerce -c "
select 
  id,
  aggregate_id,
  event_type,
  routing_key,
  correlation_id,
  status,
  created_at
from orders.outbox_events
where aggregate_id = $ORDER_ID
order by id;
"
```

---

## Laboratório 07 — RabbitMQ e DLQ

### Ver filas

```bash
docker exec -it ecommerce-rabbitmq rabbitmqadmin \
  --username=ecommerce \
  --password=ecommerce \
  list queues name messages messages_ready messages_unacknowledged
```

### Ver bindings

```bash
docker exec -it ecommerce-rabbitmq rabbitmqadmin \
  --username=ecommerce \
  --password=ecommerce \
  list bindings source destination destination_type routing_key
```

---

## Laboratório 08 — Mailpit

Acesse:

```text
http://localhost:8025
```

Valide se há email de pedido confirmado.

---

## Laboratório 09 — Treinando modelo de entrega

```bash
cd ml-model-builder
mvn clean compile exec:java -Dexec.args="train-delivery"
cd ..
```

Valide:

```bash
cat reports/ml/delivery-tribuo-v1-report.md
```

---

## Laboratório 10 — Treinando modelo de fraude

```bash
cd ml-model-builder
mvn clean compile exec:java -Dexec.args="train-fraud"
cd ..
```

Valide:

```bash
cat reports/ml/fraud-tribuo-v1-report.md
```

---

## Laboratório 11 — AI Chat BFF com confirmação explícita

### Prévia

```bash
curl -s -X POST http://localhost:8099/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: ai-lab-preview-001" \
  -d "{
    \"message\": \"Quero criar pedido do produto $PRODUCT_ID com 1 item para SE\"
  }" | jq
```

### Confirmação

```bash
curl -s -X POST http://localhost:8099/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: ai-lab-confirm-001" \
  -d "{
    \"message\": \"Confirmo criar pedido do produto $PRODUCT_ID com 1 item para SE\"
  }" | jq
```

### Guardrail

```bash
curl -s -X POST http://localhost:8099/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: ai-lab-guardrail-001" \
  -d "{
    \"message\": \"Confirmo criar pedido do produto $PRODUCT_ID com desconto e frete grátis para SE\"
  }" | jq
```

---

## Laboratório 12 — Dashboard APM

Acesse:

```text
http://localhost:3000
```

Vá em:

```text
Dashboards → Ecommerce Microservices → Ecommerce Microservices - APM Overview
```

No Loki, busque:

```logql
{service=~"order-api|product-api|fraud-detector-api|payment-service|notification-service"} |= "lab-saga-001"
```

---

## Laboratório 13 — Teste de performance com k6

```bash
docker run --rm \
  --network host \
  -e BASE_URL=http://localhost:8099 \
  -v "$PWD/tests/performance/k6:/scripts" \
  grafana/k6:latest run /scripts/order-flow.js
```

Durante o teste, observe:

- Latência no Grafana
- Logs no Loki
- CPU e memória
- Erros
- Fluxo de pedidos

---

## Laboratório 14 — Encerramento

Faça uma demonstração completa:

1. Login.
2. Criação de produto.
3. Criação de pedido.
4. Saga.
5. Email no Mailpit.
6. Outbox.
7. RabbitMQ.
8. Dashboard.
9. k6.
10. AI Chat BFF.
