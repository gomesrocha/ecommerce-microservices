# Roteiro de Demonstração Final — Ecommerce Microservices

## Objetivo

Executar uma demonstração ponta a ponta do projeto, mostrando arquitetura, segurança, Saga, ML, IA, observabilidade e performance.

---

## 1. Abrir visão geral

Apresente a arquitetura:

- API Gateway
- Microsserviços de negócio
- RabbitMQ
- PostgreSQL
- Serviços de ML
- AI Chat BFF
- Observabilidade
- k6

Explique que o sistema simula um ecommerce distribuído com fluxo real de pedido.

---

## 2. Subir ambiente

```bash
docker compose up -d --build
docker compose ps
```

Mostrar containers principais rodando.

---

## 3. Login

```bash
TOKEN=$(curl -s -X POST http://localhost:8099/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.access_token')
```

Explique JWT, JWKS e validação no KrakenD.

---

## 4. Criar produto

```bash
PRODUCT_ID=$(curl -s -X POST http://localhost:8099/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Produto Demo Final",
    "description": "Produto usado na demonstração final",
    "sku": "DEMO-FINAL-001",
    "price": 100.00,
    "stockQuantity": 10,
    "originState": "SP"
  }' | jq -r '.id')

echo $PRODUCT_ID
```

---

## 5. Estimar entrega

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

Explique modelo `delivery-tribuo-v1`.

---

## 6. Criar pedido via API

```bash
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8099/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: demo-final-api-001" \
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

---

## 7. Acompanhar Saga

```bash
sleep 15

curl -s http://localhost:8099/api/orders/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN" | jq
```

Mostrar:

- status
- entrega
- pagamento
- fraude
- estoque

---

## 8. Consultar Outbox

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

Explique Transactional Outbox.

---

## 9. Mostrar RabbitMQ

```bash
docker exec -it ecommerce-rabbitmq rabbitmqadmin \
  --username=ecommerce \
  --password=ecommerce \
  list queues name messages messages_ready messages_unacknowledged
```

Explique filas, exchange, routing keys e DLQ.

---

## 10. Mostrar Mailpit

Acesse:

```text
http://localhost:8025
```

Mostrar email de pedido confirmado.

---

## 11. Criar pedido via AI Chat BFF

### Prévia

```bash
curl -s -X POST http://localhost:8099/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: demo-final-ai-preview-001" \
  -d "{
    \"message\": \"Quero criar pedido do produto $PRODUCT_ID com 1 item para SE\"
  }" | jq
```

### Confirmação

```bash
curl -s -X POST http://localhost:8099/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: demo-final-ai-confirm-001" \
  -d "{
    \"message\": \"Confirmo criar pedido do produto $PRODUCT_ID com 1 item para SE\"
  }" | jq
```

Explique:

- deterministic router
- confirmação explícita
- guardrails
- usuário autenticado
- correlationId

---

## 12. Mostrar guardrail

```bash
curl -s -X POST http://localhost:8099/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: demo-final-ai-guardrail-001" \
  -d "{
    \"message\": \"Confirmo criar pedido do produto $PRODUCT_ID com desconto e frete grátis para SE\"
  }" | jq
```

Mostrar bloqueio.

---

## 13. Mostrar Grafana

Acesse:

```text
http://localhost:3000
```

Dashboard:

```text
Dashboards → Ecommerce Microservices → Ecommerce Microservices - APM Overview
```

Mostrar:

- logs por serviço
- busca por correlationId
- erros
- latência
- JVM
- CPU
- RabbitMQ, se habilitado

---

## 14. Executar k6

```bash
docker run --rm \
  --network host \
  -e BASE_URL=http://localhost:8099 \
  -v "$PWD/tests/performance/k6:/scripts" \
  grafana/k6:latest run /scripts/order-flow.js
```

Mostrar no terminal:

- total de requisições
- checks
- falhas
- p95
- pedidos criados

Mostrar no Grafana o impacto.

---

## 15. Mostrar relatórios ML

```bash
cat reports/ml/delivery-tribuo-v1-report.md
cat reports/ml/fraud-tribuo-v1-report.md
cat reports/ml/training-summary.json | jq
```

Explique:

- features
- métricas
- versões dos modelos
- MLOps básico

---

## 16. Encerramento

Finalize destacando:

- arquitetura distribuída
- consistência eventual
- Saga
- Outbox
- DLQ
- ML em microsserviços
- IA com guardrails
- observabilidade
- performance
- potencial de evolução para Kubernetes e CI/CD
