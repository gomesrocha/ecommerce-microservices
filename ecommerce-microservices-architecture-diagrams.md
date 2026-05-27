# Diagramas da arquitetura - Ecommerce Microservices

Este arquivo contém diagramas Mermaid da arquitetura atual do projeto, incluindo API Gateway, microsserviços, mensageria, banco, observabilidade, IA/ML e notificações.

---

## 1. Diagrama completo de contexto e containers

```mermaid
flowchart LR
    %% =========================
    %% Usuários / Clientes
    %% =========================
    USER[Cliente / Frontend / Testes HTTP]
    DEV[Desenvolvedor]

    %% =========================
    %% Gateway
    %% =========================
    subgraph GATEWAY["API Gateway"]
        KRAKEND[KrakenD<br/>api-gateway<br/>:8099]
    end

    %% =========================
    %% Microsserviços Quarkus
    %% =========================
    subgraph SERVICES["Microsserviços Java + Quarkus"]
        USERAPI[user-api<br/>Auth / JWT / JWKS]
        PRODUCT[product-api<br/>Produtos / Estoque]
        ORDER[order-api<br/>Pedidos / Saga / Outbox]
        DELIVERY[delivery-estimator-api<br/>Entrega / Olist / Tribuo]
        FRAUD[fraud-detector-api<br/>Fraude / Tribuo]
        AICHAT[ai-chat-bff<br/>LangChain4j / Ollama / Guardrails]
        NOTIFY[notification-service<br/>SCREEN / EMAIL]
    end

    %% =========================
    %% Dados
    %% =========================
    subgraph DATA["Dados"]
        POSTGRES[(PostgreSQL<br/>schemas/tabelas dos serviços)]
        MODELS[(Modelos ML<br/>delivery-tribuo-v1.model<br/>fraud-tribuo-v1.model)]
    end

    %% =========================
    %% Mensageria
    %% =========================
    subgraph MQ["Mensageria"]
        RABBIT[(RabbitMQ<br/>Exchanges / Queues)]
    end

    %% =========================
    %% IA Local
    %% =========================
    subgraph AI["IA Local"]
        OLLAMA[Ollama<br/>llama3.2:latest]
    end

    %% =========================
    %% Email
    %% =========================
    subgraph MAIL["Email local"]
        MAILPIT[Mailpit<br/>SMTP :1025<br/>UI :8025]
    end

    %% =========================
    %% Observabilidade
    %% =========================
    subgraph OBS["Observabilidade"]
        OTEL[OpenTelemetry Collector]
        PROM[Prometheus]
        GRAFANA[Grafana]
        TEMPO[Tempo]
    end

    %% =========================
    %% Fluxos externos
    %% =========================
    USER --> KRAKEND
    DEV --> GRAFANA
    DEV --> PROM
    DEV --> MAILPIT
    DEV --> RABBIT

    %% =========================
    %% Gateway para serviços
    %% =========================
    KRAKEND --> USERAPI
    KRAKEND --> PRODUCT
    KRAKEND --> ORDER
    KRAKEND --> DELIVERY
    KRAKEND --> FRAUD
    KRAKEND --> AICHAT
    KRAKEND --> NOTIFY

    %% =========================
    %% Banco
    %% =========================
    USERAPI --> POSTGRES
    PRODUCT --> POSTGRES
    ORDER --> POSTGRES
    DELIVERY --> POSTGRES
    FRAUD --> POSTGRES
    NOTIFY --> POSTGRES

    %% =========================
    %% REST interno
    %% =========================
    ORDER --> PRODUCT
    ORDER --> DELIVERY
    ORDER --> FRAUD

    AICHAT --> PRODUCT
    AICHAT --> ORDER
    AICHAT --> DELIVERY
    AICHAT --> OLLAMA

    %% =========================
    %% RabbitMQ
    %% =========================
    ORDER --> RABBIT
    PRODUCT --> RABBIT
    FRAUD --> RABBIT
    RABBIT --> NOTIFY

    %% =========================
    %% ML
    %% =========================
    DELIVERY --> MODELS
    FRAUD --> MODELS

    %% =========================
    %% Email
    %% =========================
    NOTIFY --> MAILPIT

    %% =========================
    %% Observabilidade
    %% =========================
    USERAPI --> OTEL
    PRODUCT --> OTEL
    ORDER --> OTEL
    DELIVERY --> OTEL
    FRAUD --> OTEL
    AICHAT --> OTEL
    NOTIFY --> OTEL

    PROM --> USERAPI
    PROM --> PRODUCT
    PROM --> ORDER
    PROM --> DELIVERY
    PROM --> FRAUD
    PROM --> AICHAT
    PROM --> NOTIFY

    GRAFANA --> PROM
    GRAFANA --> TEMPO
    OTEL --> TEMPO
```

---

## 2. Diagrama do fluxo de pedido com notificações

```mermaid
sequenceDiagram
    autonumber
    actor Cliente
    participant Gateway as KrakenD API Gateway
    participant Order as order-api
    participant Product as product-api
    participant Delivery as delivery-estimator-api
    participant Fraud as fraud-detector-api
    participant Outbox as Outbox order-api
    participant Rabbit as RabbitMQ
    participant Notify as notification-service
    participant Mailpit as Mailpit
    participant DB as PostgreSQL

    Cliente->>Gateway: POST /api/orders
    Gateway->>Order: POST /orders

    Order->>Product: Valida produto / estoque
    Product-->>Order: Produto disponível

    Order->>Delivery: Estima prazo de entrega
    Delivery-->>Order: Prazo estimado via TRIBUO_MODEL

    Order->>DB: Salva pedido WAITING_STOCK / WAITING_FRAUD
    Order->>Outbox: Salva eventos de domínio

    Outbox->>Rabbit: Publica eventos pendentes

    Rabbit->>Fraud: order.created / análise de fraude
    Fraud->>DB: Salva análise de fraude
    Fraud->>Rabbit: fraud.approved ou fraud.rejected

    Rabbit->>Order: Evento de fraude
    Order->>DB: Atualiza status do pedido

    Order->>Outbox: Salva NotificationRequest
    Outbox->>Rabbit: Publica notifications.requested

    Rabbit->>Notify: Consome notifications.requested
    Notify->>DB: Salva Notification
    Notify->>DB: Salva deliveries SCREEN / EMAIL
    Notify->>Mailpit: Envia e-mail
    Notify-->>Rabbit: ACK

    Cliente->>Gateway: GET /api/notifications/user/1/unread
    Gateway->>Notify: Consulta notificações
    Notify-->>Cliente: Notificações de tela
```

---

## 3. Diagrama do AI Chat BFF

```mermaid
flowchart TD
    USER[Usuário / Frontend] --> GW[KrakenD<br/>/api/ai/chat]
    GW --> BFF[ai-chat-bff]

    BFF --> GUARD[ChatGuardrailService]
    GUARD -->|Bloqueado| BLOCK[Resposta guardrail<br/>sem chamar serviços]
    GUARD -->|Permitido| INTENT[ChatIntentService]

    INTENT -->|LIST_PRODUCTS| PRODUCT[product-api]
    INTENT -->|GET_PRODUCT| PRODUCT
    INTENT -->|ESTIMATE_DELIVERY| DELIVERY[delivery-estimator-api]
    INTENT -->|GET_ORDER| ORDER[order-api]
    INTENT -->|GENERAL_CHAT| LLM[Ollama<br/>llama3.2:latest]

    PRODUCT --> BFF
    DELIVERY --> BFF
    ORDER --> BFF
    LLM --> BFF

    BFF --> RESPONSE[ChatResponse]
    RESPONSE --> USER
```

---

## 4. Diagrama do Notification Service

```mermaid
flowchart LR
    GW[KrakenD<br/>/api/notifications] --> API[NotificationResource]
    RABBIT[(RabbitMQ<br/>notifications.requested)] --> CONSUMER[NotificationRequestedConsumer]

    API --> SERVICE[NotificationService]
    CONSUMER --> SERVICE

    SERVICE --> NOTIF[(notifications.notifications)]
    SERVICE --> DELIV[(notifications.notification_deliveries)]

    SERVICE --> DISPATCHER[NotificationDispatcher]

    DISPATCHER --> SCREEN[ScreenNotificationSender]
    DISPATCHER --> EMAIL[EmailNotificationSender]

    SCREEN --> NOTIF
    EMAIL --> MAILPIT[Mailpit]

    SERVICE --> METRICS[notification_deliveries_total]
    METRICS --> PROM[Prometheus]
    PROM --> GRAFANA[Grafana]
```

---

## 5. Diagrama de observabilidade

```mermaid
flowchart TD
    subgraph Services["Microsserviços Quarkus"]
        USERAPI[user-api]
        PRODUCT[product-api]
        ORDER[order-api]
        DELIVERY[delivery-estimator-api]
        FRAUD[fraud-detector-api]
        AICHAT[ai-chat-bff]
        NOTIFY[notification-service]
    end

    USERAPI --> OTEL[OpenTelemetry Collector]
    PRODUCT --> OTEL
    ORDER --> OTEL
    DELIVERY --> OTEL
    FRAUD --> OTEL
    AICHAT --> OTEL
    NOTIFY --> OTEL

    OTEL --> TEMPO[Tempo<br/>traces]

    PROM[Prometheus<br/>scrape /q/metrics] --> USERAPI
    PROM --> PRODUCT
    PROM --> ORDER
    PROM --> DELIVERY
    PROM --> FRAUD
    PROM --> AICHAT
    PROM --> NOTIFY

    GRAFANA[Grafana] --> PROM
    GRAFANA --> TEMPO

    DELIVERY --> M1[delivery_predictions_total]
    FRAUD --> M2[fraud_predictions_total]
    AICHAT --> M3[ai_chat_requests_total]
    NOTIFY --> M4[notification_deliveries_total]
```

---

## 6. Diagrama de ML

```mermaid
flowchart LR
    subgraph Training["Treinamento"]
        OLIST[Dataset Olist]
        ETL[ETL Python]
        BUILDER[ml-model-builder<br/>Java + Tribuo]
    end

    subgraph Models["Modelos versionados"]
        DMODEL[delivery-tribuo-v1.model]
        FMODEL[fraud-tribuo-v1.model]
    end

    subgraph Inference["Inferência em produção"]
        DELIVERY[delivery-estimator-api]
        FRAUD[fraud-detector-api]
    end

    OLIST --> ETL
    ETL --> BUILDER
    BUILDER --> DMODEL
    BUILDER --> FMODEL

    DMODEL --> DELIVERY
    FMODEL --> FRAUD

    DELIVERY --> DRESP[Prazo estimado]
    FRAUD --> FRESP[APPROVED / REJECTED]
```
