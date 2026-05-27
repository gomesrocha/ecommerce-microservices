# Ecommerce Microservices com Java, Quarkus, RabbitMQ, PostgreSQL, IA e Observabilidade

Projeto de referência para estudo, demonstração e evolução de uma arquitetura moderna de microsserviços usando **Java 21**, **Quarkus**, **RabbitMQ**, **PostgreSQL**, **KrakenD**, **OpenTelemetry**, **Prometheus**, **Grafana**, **Tribuo**, **LangChain4j**, **Ollama** e **Mailpit**.

O projeto simula um ecommerce distribuído e evolui incrementalmente desde serviços básicos até padrões avançados de arquitetura, como Saga, Transactional Outbox, idempotência, API Gateway, autenticação JWT, Machine Learning, BFF conversacional com IA generativa, guardrails, métricas de negócio, dashboards e notificações multicanal.

---

## Objetivos do projeto

Este projeto demonstra, de forma prática, como construir uma arquitetura de microsserviços moderna com Java e Quarkus.

Ele cobre:

- criação de microsserviços independentes;
- comunicação síncrona via REST;
- comunicação assíncrona via RabbitMQ;
- persistência com PostgreSQL;
- autenticação e autorização com JWT;
- API Gateway com KrakenD;
- padrões de consistência como Saga e Transactional Outbox;
- análise de entrega e fraude com Machine Learning;
- BFF conversacional com LangChain4j e Ollama;
- guardrails para uso seguro de IA;
- observabilidade com Prometheus, Grafana, Tempo e OpenTelemetry;
- notificações multicanal com tela e e-mail.

---

## Arquitetura geral

```text
Frontend / Cliente
  ↓
KrakenD API Gateway
  ↓
Microsserviços Quarkus
  ↓
PostgreSQL + RabbitMQ
  ↓
Observabilidade + IA/ML + Notificações
```

Fluxo principal de pedido:

```text
Cliente cria pedido
  ↓
order-api
  ↓
product-api reserva estoque
  ↓
delivery-estimator-api estima entrega
  ↓
fraud-detector-api analisa risco
  ↓
order-api confirma ou rejeita pedido
  ↓
notification-service notifica o cliente
```

---

## Microsserviços

| Serviço | Responsabilidade |
|---|---|
| `user-api` | Autenticação, login, refresh token, JWT e JWKS |
| `product-api` | Cadastro, consulta e controle de produtos e estoque |
| `order-api` | Criação de pedidos, orquestração do fluxo, histórico e outbox |
| `delivery-estimator-api` | Estimativa de prazo de entrega com baseline Olist e modelo Tribuo |
| `fraud-detector-api` | Análise de fraude/risco com modelo Tribuo |
| `ai-chat-bff` | BFF conversacional com LangChain4j, Ollama, roteamento determinístico e guardrails |
| `notification-service` | Notificações multicanal por tela e e-mail |
| `api-gateway` | Gateway KrakenD para expor APIs públicas |

---

## Tecnologias

| Categoria | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Quarkus |
| Banco de dados | PostgreSQL |
| Mensageria | RabbitMQ |
| API Gateway | KrakenD |
| Autenticação | JWT / JWKS |
| Machine Learning | Tribuo |
| IA Generativa | LangChain4j + Ollama |
| Observabilidade | OpenTelemetry, Prometheus, Grafana, Tempo |
| E-mail local | Mailpit |
| Containerização | Docker Compose |

---

## Padrões arquiteturais aplicados

- API Gateway;
- Backend for Frontend;
- Saga;
- Transactional Outbox;
- Idempotência;
- Event-driven architecture;
- Mensageria com RabbitMQ;
- Health checks;
- Métricas Prometheus;
- Dashboards Grafana;
- Guardrails para IA;
- Separação entre treinamento e inferência de modelos;
- Notificação multicanal.


---

## API Gateway

O KrakenD centraliza o acesso externo aos microsserviços.

URL local:

```text
http://localhost:8099
```

Rotas principais:

```text
/api/auth/login
/api/products
/api/orders
/api/delivery-estimates/estimate
/api/fraud-analyses
/api/ai/chat
/api/notifications
```

---

## Autenticação

O `user-api` gera tokens JWT e expõe JWKS para validação.

Login:

```bash
curl -X POST http://localhost:8099/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Uso do token:

```text
Authorization: Bearer <access_token>
```

JWKS:

```text
http://localhost:8099/api/auth/.well-known/jwks.json
```

---

## Product API

Responsável pelo catálogo de produtos e controle de estoque.

```bash
curl -X POST http://localhost:8099/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Produto Demo",
    "description": "Produto para teste",
    "sku": "DEMO-001",
    "price": 100.00,
    "stockQuantity": 10,
    "originState": "SP"
  }'
```

---

## Order API

Responsável pela criação e acompanhamento de pedidos.

```bash
curl -X POST http://localhost:8099/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "userId": 1,
    "customerState": "SE",
    "items": [
      {
        "productId": 1,
        "quantity": 1
      }
    ]
  }'
```

O `order-api` possui suporte a:

- histórico de status;
- cancelamento;
- integração com estoque;
- integração com análise de fraude;
- Transactional Outbox para publicação segura de eventos.

---

## Delivery Estimator API

O `delivery-estimator-api` estima o prazo de entrega.

Ele usa:

- baseline baseado na base Olist;
- modelo de regressão com Tribuo;
- fallback por regra quando necessário.

```bash
curl -X POST http://localhost:8099/api/delivery-estimates/estimate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "originState": "BA",
    "destinationState": "AL",
    "totalItems": 1
  }'
```

Resposta esperada:

```json
{
  "originState": "BA",
  "destinationState": "AL",
  "totalItems": 1,
  "minDays": 23,
  "estimatedDays": 25,
  "maxDays": 29,
  "source": "TRIBUO_MODEL",
  "modelVersion": "delivery-tribuo-v1"
}
```

---

## Fraud Detector API

O `fraud-detector-api` analisa risco de fraude usando um modelo de classificação com Tribuo treinado a partir de uma base de risco simulada com dados Olist.

Possíveis decisões:

```text
APPROVED
REJECTED
```

---

## Machine Learning com Tribuo

O projeto possui uma trilha de Machine Learning usando **Tribuo**, uma biblioteca Java para ML.

Modelos atuais:

```text
models/delivery/delivery-tribuo-v1.model
models/fraud/fraud-tribuo-v1.model
```

O treinamento é feito separadamente no módulo:

```text
ml-model-builder
```

Separação adotada:

```text
ml-model-builder = treinamento
microsserviços = inferência
```


---

## AI Chat BFF

O `ai-chat-bff` oferece uma interface conversacional usando **LangChain4j + Ollama**.

Ele usa:

- roteamento determinístico para intenções conhecidas;
- consulta real aos serviços internos;
- LLM local apenas para perguntas gerais;
- guardrails server-side para impedir ações indevidas.

Exemplos:

```text
Liste alguns produtos disponíveis.
Estime a entrega de 1 item saindo da BA para AL.
Consulte o pedido 10.
```

Guardrails impedem pedidos como:

```text
Mude o preço do produto para R$ 1.
Reduza o prazo de entrega para amanhã.
Aplique desconto no produto.
Altere o estoque.
Cancele o pedido.
```

```bash
curl -X POST http://localhost:8099/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "message": "Liste alguns produtos disponíveis no ecommerce."
  }'
```

---

## Notification Service

O `notification-service` é um hub de notificações multicanal.

Canais iniciais:

| Canal | Descrição |
|---|---|
| `SCREEN` | Notificação persistida no banco para exibição no frontend |
| `EMAIL` | Envio SMTP usando Mailpit no ambiente local |

Canais preparados para evolução:

| Canal | Futuro |
|---|---|
| `WHATSAPP` | Integração com WhatsApp Cloud API, Twilio, Z-API ou outro provedor |
| `WEBHOOK` | Integração com sistemas externos |
| `PUSH` | Push notification web/mobile |

Criar notificação:

```bash
curl -X POST http://localhost:8099/api/notifications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "eventId": "demo-001",
    "eventType": "ORDER_CONFIRMED",
    "aggregateType": "ORDER",
    "aggregateId": 123,
    "userId": 1,
    "title": "Pedido confirmado",
    "message": "Seu pedido 123 foi confirmado com sucesso.",
    "email": "cliente@ecommerce.local",
    "severity": "INFO",
    "channels": ["SCREEN", "EMAIL"],
    "metadata": {
      "orderId": 123
    }
  }'
```

Listar notificações de tela:

```bash
curl http://localhost:8099/api/notifications/user/1 \
  -H "Authorization: Bearer $TOKEN"
```

Listar não lidas:

```bash
curl http://localhost:8099/api/notifications/user/1/unread \
  -H "Authorization: Bearer $TOKEN"
```

Marcar como lida:

```bash
curl -X PUT http://localhost:8099/api/notifications/1/read \
  -H "Authorization: Bearer $TOKEN"
```

E-mails locais podem ser vistos no Mailpit:

```text
http://localhost:8025
```

---

## Observabilidade

O projeto possui stack de observabilidade com:

- OpenTelemetry;
- Prometheus;
- Grafana;
- Tempo.

URLs:

| Ferramenta | URL |
|---|---|
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| Tempo | http://localhost:3200 |

Métricas de negócio:

```text
delivery_predictions_total
fraud_predictions_total
ai_chat_requests_total
notification_deliveries_total
```

Dashboard principal:

```text
Ecommerce ML & AI - Business Metrics
```


---

## Como executar

Subir toda a stack:

```bash
docker compose up -d --build
```

Serviços principais:

| Serviço | URL |
|---|---|
| API Gateway | http://localhost:8099 |
| User API | http://localhost:8094 |
| Product API | http://localhost:8095 |
| Order API | http://localhost:8096 |
| Delivery Estimator API | http://localhost:8097 |
| Fraud Detector API | http://localhost:8098 |
| AI Chat BFF | http://localhost:8100 |
| Notification Service | http://localhost:8101 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| RabbitMQ Management | http://localhost:15672 |
| Mailpit | http://localhost:8025 |

Credenciais padrão do RabbitMQ:

```text
usuário: ecommerce
senha: ecommerce
```

Credenciais padrão do Grafana:

```text
usuário: admin
senha: admin
```

---

## Testes HTTP

Os testes manuais estão em:

```text
tests/http/
```

Incluem:

- autenticação;
- produtos;
- pedidos;
- entrega;
- fraude;
- AI Chat;
- notificações.

---

## Documentação

A documentação do projeto fica em:

```text
docs/
```

Conteúdos previstos ou já existentes:

- arquitetura de microsserviços;
- trilha de IA/ML;
- Tribuo;
- Olist;
- BFF com LangChain4j/Ollama;
- guardrails;
- observabilidade;
- notificações;
- roadmap.

---

## Roadmap

Próximas evoluções possíveis:

- integração automática do `order-api` com o `notification-service`;
- publicação de eventos `NotificationRequested` via Outbox;
- canal WhatsApp;
- canal Webhook;
- criação de pedido via AI Chat com confirmação explícita;
- DLQ padronizada para RabbitMQ;
- correlation ID entre serviços;
- model registry;
- relatório de métricas dos modelos;
- melhoria dos datasets de delivery e fraude;
- frontend web para interação com o ecommerce;
- documentação final em apostila.

---

## Objetivo educacional

Este projeto é uma base prática para estudar:

- Java moderno com Quarkus;
- microsserviços;
- arquitetura orientada a eventos;
- mensageria com RabbitMQ;
- padrões de consistência;
- observabilidade;
- segurança com JWT;
- API Gateway;
- IA generativa;
- Machine Learning em Java;
- notificações multicanal;
- evolução incremental de arquitetura.

É indicado para aulas, workshops, demonstrações técnicas, estudos de arquitetura e experimentos com sistemas distribuídos modernos.
