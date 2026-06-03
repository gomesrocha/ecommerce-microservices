# Apostila Final — Ecommerce Microservices com Quarkus, RabbitMQ, ML, IA e Observabilidade

## 1. Visão geral

Este material consolida o projeto **Ecommerce Microservices**, desenvolvido como um hands-on de arquitetura moderna com microsserviços, mensageria, segurança, machine learning, IA generativa, observabilidade e testes de performance.

O objetivo do curso é mostrar, de ponta a ponta, como construir uma arquitetura distribuída com:

- Java 21 e Quarkus
- PostgreSQL
- RabbitMQ
- KrakenD API Gateway
- JWT e JWKS
- Saga coreografada
- Transactional Outbox
- Dead Letter Queues
- Serviços de ML com Tribuo
- AI Chat BFF com confirmação explícita
- Prometheus, Grafana, Tempo, Loki e Alloy
- Testes de performance com k6

Ao final, o aluno terá um ambiente local completo, observável e testável, simulando um ecommerce com fluxo real de pedido.

---

## 2. Arquitetura geral

A solução é organizada em múltiplos microsserviços independentes, cada um responsável por uma capacidade de negócio.

### Serviços principais

| Serviço | Responsabilidade |
|---|---|
| `user-api` | Autenticação, JWT, refresh token e JWKS |
| `product-api` | Cadastro de produtos e reserva de estoque |
| `order-api` | Criação de pedidos, Saga, Outbox e estado do pedido |
| `delivery-estimator-api` | Estimativa de entrega com modelo ML |
| `fraud-detector-api` | Análise de risco/fraude com modelo ML |
| `payment-service` | Simulação de pagamento |
| `notification-service` | Notificações e envio de email via Mailpit |
| `ai-chat-bff` | Assistente conversacional para consulta e criação controlada de pedidos |
| `api-gateway` | Exposição externa via KrakenD |

### Serviços de infraestrutura

| Componente | Responsabilidade |
|---|---|
| PostgreSQL | Persistência dos microsserviços |
| RabbitMQ | Comunicação assíncrona por eventos |
| Mailpit | Simulação de SMTP/email |
| WireMock | Simulação de gateway de pagamento |
| Prometheus | Coleta de métricas |
| Grafana | Dashboards |
| Tempo | Traces distribuídos |
| Loki | Logs centralizados |
| Alloy | Coleta de logs dos containers Docker |

---

## 3. Fluxo principal de negócio

O fluxo de pedido segue uma Saga assíncrona:

1. Usuário autentica via `user-api`.
2. Usuário cria pedido via `order-api` ou via `ai-chat-bff`.
3. `order-api` calcula estimativa de entrega consultando `delivery-estimator-api`.
4. `order-api` grava pedido em `WAITING_STOCK`.
5. `order-api` grava eventos na Outbox.
6. Worker da Outbox publica eventos no RabbitMQ.
7. `product-api` consome evento de reserva de estoque.
8. `product-api` publica `stock.reserved` ou `stock.rejected`.
9. `fraud-detector-api` analisa risco de fraude.
10. `payment-service` simula autorização do pagamento.
11. `notification-service` envia notificação de confirmação.
12. `order-api` atualiza o pedido para `CONFIRMED` ou estado de rejeição/cancelamento.

---

## 4. Setup do ambiente

### Pré-requisitos

- Docker
- Docker Compose
- Java 21
- Maven
- GitHub CLI
- jq
- curl

### Subir ambiente

```bash
docker compose up -d --build
```

### Verificar containers

```bash
docker compose ps
```

### Acessos úteis

| Serviço | URL |
|---|---|
| API Gateway | http://localhost:8099 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| RabbitMQ Management | http://localhost:15672 |
| Mailpit | http://localhost:8025 |
| Loki | http://localhost:3100 |
| Alloy | http://localhost:12345 |
| Tempo | http://localhost:3200 |

Credenciais padrão do Grafana:

```text
admin / admin
```

Credenciais padrão do RabbitMQ:

```text
ecommerce / ecommerce
```

---

## 5. Autenticação e API Gateway

A autenticação é feita no `user-api`, enquanto o KrakenD atua como API Gateway.

### Login

```bash
TOKEN=$(curl -s -X POST http://localhost:8099/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.access_token')
```

### Chamada autenticada

```bash
curl -s http://localhost:8099/api/products \
  -H "Authorization: Bearer $TOKEN" | jq
```

O gateway valida o JWT usando JWKS exposto pelo `user-api`.

---

## 6. Product API e estoque

O `product-api` gerencia produtos e estoque.

### Criar produto

```bash
PRODUCT_ID=$(curl -s -X POST http://localhost:8099/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Produto Demo",
    "description": "Produto para demonstração",
    "sku": "DEMO-001",
    "price": 100.00,
    "stockQuantity": 10,
    "originState": "SP"
  }' | jq -r '.id')

echo $PRODUCT_ID
```

### Consultar produto

```bash
curl -s http://localhost:8099/api/products/$PRODUCT_ID \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## 7. Delivery Estimator com ML

O `delivery-estimator-api` calcula estimativa de entrega usando modelo Tribuo.

### Features do modelo

O modelo usa features como:

- quantidade de itens
- origem
- destino
- região de origem
- região de destino
- mesma região
- mesmo estado
- distância aproximada
- buckets de distância

### Testar estimativa

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

Resultado esperado:

```text
source = TRIBUO_MODEL
modelVersion = delivery-tribuo-v1
```

---

## 8. Fraud Detector com ML

O `fraud-detector-api` analisa risco de fraude usando modelo Tribuo de classificação.

### Features do modelo

O modelo usa features como:

- valor total
- quantidade de itens
- preço médio
- preço máximo
- produtos únicos
- distância da rota
- origem/destino
- buckets de valor
- buckets de quantidade
- mesma região
- rota interestadual

### Métricas de treino

Os relatórios ficam em:

```text
reports/ml/fraud-tribuo-v1-report.md
reports/ml/training-summary.json
```

---

## 9. ML Model Builder

O `ml-model-builder` centraliza o treinamento dos modelos.

### Treinar modelo de entrega

```bash
cd ml-model-builder
mvn clean compile exec:java -Dexec.args="train-delivery"
cd ..
```

### Treinar modelo de fraude

```bash
cd ml-model-builder
mvn clean compile exec:java -Dexec.args="train-fraud"
cd ..
```

### Treinar todos os modelos

```bash
cd ml-model-builder
mvn clean compile exec:java -Dexec.args="train-all"
cd ..
```

---

## 10. Saga de pedido

### Criar pedido

```bash
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8099/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: demo-order-001" \
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

### Consultar pedido

```bash
curl -s http://localhost:8099/api/orders/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Acompanhar logs da Saga

```bash
docker compose logs --since=5m order-api product-api fraud-detector-api payment-service notification-service \
  | grep -Ei "demo-order-001|StockReserved|Fraud|PaymentApproved|NotificationRequested|ERROR|Falha"
```

---

## 11. Transactional Outbox

O `order-api` usa o padrão Transactional Outbox para evitar inconsistência entre banco e mensageria.

### Fluxo

1. Pedido é salvo no banco.
2. Evento é salvo na tabela `orders.outbox_events`.
3. Worker periódico lê eventos pendentes.
4. Evento é publicado no RabbitMQ.
5. Status do evento muda para `PUBLISHED`.

### Consultar Outbox

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
order by id desc
limit 20;
"
```

---

## 12. RabbitMQ e DLQ

O RabbitMQ é usado para eventos assíncronos da Saga.

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

### DLQs

As Dead Letter Queues são usadas para mensagens que falham após tentativas de processamento.

---

## 13. Payment Service

O `payment-service` simula autorização de pagamento usando WireMock.

### Consultar pagamentos

```bash
curl -s http://localhost:8099/api/payments \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Consultar pagamento por pedido

```bash
curl -s http://localhost:8099/api/payments/order/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## 14. Notification Service e Mailpit

O `notification-service` envia notificações e email via Mailpit.

Acesse:

```text
http://localhost:8025
```

É possível ver email de confirmação de pedido.

---

## 15. AI Chat BFF

O `ai-chat-bff` permite consultar informações do ecommerce e criar pedido somente com confirmação explícita.

### Fluxo seguro

1. Usuário faz login.
2. Usuário chama `/api/ai/chat` com Bearer Token.
3. Chat identifica intenção.
4. Para pedido, retorna prévia.
5. Pedido só é criado se o usuário confirmar explicitamente.

### Prévia do pedido

```bash
curl -s -X POST http://localhost:8099/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: ai-order-preview-001" \
  -d '{
    "message": "Quero criar pedido do produto 47 com 1 item para SE"
  }' | jq
```

### Confirmação explícita

```bash
curl -s -X POST http://localhost:8099/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Correlation-Id: ai-order-confirm-001" \
  -d '{
    "message": "Confirmo criar pedido do produto 47 com 1 item para SE"
  }' | jq
```

### Guardrail

O BFF bloqueia pedidos com tentativas de:

- desconto
- frete grátis
- alteração de preço
- alteração de prazo
- alteração de estoque
- tentativa de ignorar regras

---

## 16. Observabilidade

A stack de observabilidade inclui:

- Prometheus para métricas
- Grafana para dashboards
- Tempo para traces
- Loki para logs
- Alloy para coleta de logs

### Dashboard APM

No Grafana:

```text
Dashboards → Ecommerce Microservices → Ecommerce Microservices - APM Overview
```

### Consultas LogQL úteis

Logs da Saga:

```logql
{service=~"order-api|product-api|fraud-detector-api|payment-service|notification-service"}
```

Erros:

```logql
{service=~".+"} |~ "ERROR|Error|error|Falha|falha|Exception"
```

Busca por correlationId:

```logql
{service=~"order-api|product-api|fraud-detector-api|payment-service|notification-service"} |= "demo-order-001"
```

---

## 17. k6 Performance Test Suite

Os testes de performance ficam em:

```text
tests/performance/k6/order-flow.js
```

### Executar via Docker

```bash
docker run --rm \
  --network host \
  -e BASE_URL=http://localhost:8099 \
  -v "$PWD/tests/performance/k6:/scripts" \
  grafana/k6:latest run /scripts/order-flow.js
```

### Resultado esperado

- falhas HTTP abaixo de 5%
- checks acima de 95%
- p95 abaixo de 2 segundos

No teste validado durante o curso, foram criados 98 pedidos, com 0% de falhas e checks em 100%.

---

## 18. Troubleshooting

### Gateway não responde

```bash
docker compose logs --tail=100 api-gateway
```

### Serviço não sobe

```bash
docker compose logs --tail=100 nome-do-servico
```

### Ver status geral

```bash
docker compose ps
```

### Ver métricas Prometheus

```bash
curl -s "http://localhost:9090/api/v1/targets" | jq
```

### Ver labels Loki

```bash
curl -s http://localhost:3100/loki/api/v1/labels | jq
```

### Ver logs de um serviço

```bash
docker compose logs --tail=100 order-api
```

---

## 19. Checklist final

Ao concluir o projeto, o aluno deve conseguir:

- Subir todo o ambiente com Docker Compose.
- Autenticar usuário via JWT.
- Criar produto.
- Criar pedido.
- Acompanhar Saga.
- Visualizar email no Mailpit.
- Ver mensagens no RabbitMQ.
- Ver eventos na Outbox.
- Treinar modelos ML.
- Consultar relatórios MLOps.
- Usar AI Chat BFF para criar pedido com confirmação explícita.
- Observar logs, métricas e traces no Grafana.
- Rodar teste de performance com k6.

---

## 20. Encerramento

Este projeto demonstra como integrar arquitetura de microsserviços, mensageria, IA, ML, MLOps, observabilidade e performance em um único hands-on didático.

Ele pode ser usado como base para cursos, workshops, provas de conceito e evolução para arquiteturas mais avançadas com Kubernetes, CI/CD, GitOps, Service Mesh e ambientes cloud native.
