# Technical Debt

Este documento registra os principais débitos técnicos, melhorias planejadas e features futuras identificadas durante a construção incremental do monorepo de microsserviços do sistema de pedidos.

A estratégia do projeto é evoluir por pequenas features, mantendo o sistema funcional a cada etapa. Alguns itens começaram como débitos técnicos e depois foram parcial ou totalmente resolvidos em branches específicas. Quando isso acontece, o status é atualizado, mas o item permanece documentado para manter o histórico arquitetural.

---

## Situação atual do projeto

O monorepo já possui os seguintes serviços principais:

```text
user-api
product-api
order-api
delivery-estimator-api
fraud-detector-api
api-gateway / KrakenD
```

Infraestrutura local atual:

```text
PostgreSQL
RabbitMQ
KrakenD
OpenTelemetry Collector
Tempo
Prometheus
Grafana
```

Fluxo principal atual do pedido:

```text
Cliente
  ↓
KrakenD
  ↓
order-api
  ↓ REST
product-api / delivery-estimator-api
  ↓
order-api salva pedido como WAITING_STOCK
  ↓ Outbox
product.stock.reserve
  ↓ RabbitMQ
product-api reserva estoque
  ↓ RabbitMQ
stock.reserved / stock.rejected
  ↓
order-api atualiza status
  ↓ Outbox
order.created
  ↓ RabbitMQ
fraud-detector-api analisa fraude
  ↓ RabbitMQ
fraud.approved / fraud.rejected
  ↓
order-api atualiza para CONFIRMED ou REJECTED
```

---

## TD-001: Dockerizar os microsserviços

**Status:** concluído inicialmente na feature `dockerize-services`.

Os microsserviços Quarkus passaram a ter Dockerfile JVM e foram adicionados ao `docker-compose.yml`, junto com PostgreSQL, RabbitMQ e demais componentes.

Serviços dockerizados:

- user-api;
- product-api;
- order-api;
- delivery-estimator-api;
- fraud-detector-api.

O Docker Compose agora permite subir a stack completa localmente.

Evoluções futuras:

- melhorar healthchecks dos microsserviços;
- reduzir tamanho das imagens;
- criar imagem base comum;
- criar pipeline automatizado de build/push;
- preparar imagens para Kubernetes;
- validar execução com múltiplas réplicas.

Branch concluída:

```text
feature/dockerize-services
```

---

## TD-002: Criar imagens nativas com Quarkus

**Status:** aberto.

Atualmente os serviços estão rodando com Dockerfile JVM/Fast JAR usando imagem UBI OpenJDK.

A criação de imagens nativas com Quarkus Native, GraalVM/Mandrel e `ubi9-quarkus-micro-image` deve ser avaliada depois que o fluxo estiver mais estável.

Objetivos:

- reduzir tempo de inicialização;
- reduzir consumo de memória;
- comparar JVM vs Native;
- avaliar impacto em Docker e Kubernetes;
- documentar limitações do build nativo.

Branch planejada:

```text
feature/native-dockerfiles
```

---

## TD-003: Criar manifests Kubernetes ou Helm Charts

**Status:** aberto.

Após a dockerização, será necessário criar a estrutura de implantação em Kubernetes.

A primeira versão pode usar manifests YAML simples. Depois, pode evoluir para Helm Charts ou Kustomize.

Itens previstos:

- Namespace;
- Deployment por serviço;
- Service por serviço;
- ConfigMap;
- Secret;
- Ingress ou Gateway;
- readinessProbe;
- livenessProbe;
- resource requests e limits;
- configuração de variáveis de ambiente;
- integração com stack de observabilidade.

Branch planejada:

```text
feature/kubernetes-deployment
```

---

## TD-004: Implementar Transactional Outbox no order-api

**Status:** concluído na feature `order-outbox-pattern`.

O `order-api` deixou de publicar eventos diretamente no RabbitMQ logo após persistir o pedido.

Agora o fluxo é:

```text
OrderService
  ↓
salva pedido / altera status
  ↓
salva evento em orders.outbox_events
  ↓
commit da transação
  ↓
OutboxPublisherWorker publica no RabbitMQ
  ↓
marca evento como PUBLISHED
```

Eventos atualmente enviados pela Outbox do `order-api`:

- StockReservationRequested;
- OrderCreated;
- OrderCanceled.

Evoluções futuras:

- adicionar status `PROCESSING`;
- adicionar locking para múltiplas instâncias;
- evitar que duas réplicas publiquem o mesmo evento;
- criar dead letter para eventos com muitas falhas;
- adicionar painel administrativo da outbox;
- adicionar correlação por `correlationId`;
- propagar `eventId` em todos os consumidores.

Branch concluída:

```text
feature/order-outbox-pattern
```

---

## TD-005: Implementar idempotência no consumo e publicação de eventos

**Status:** aberto.

Os eventos possuem `eventId`, mas ainda não há estratégia completa de idempotência nos consumidores.

Riscos:

- uma mensagem pode ser reentregue;
- um consumidor pode aplicar o mesmo efeito duas vezes;
- um evento de estoque pode baixar o estoque duplicadamente;
- um evento de fraude pode tentar mudar status já finalizado.

Itens previstos:

- criar tabela de eventos processados por serviço;
- usar `eventId` como chave de idempotência;
- impedir efeitos duplicados;
- tratar reentrega sem inconsistência;
- padronizar estratégia de retry.

Branch planejada:

```text
feature/event-idempotency
```

---

## TD-006: Formalizar Saga do fluxo de pedido

**Status:** concluído parcialmente na feature `order-saga-flow`.

O `order-api` já possui:

- histórico de transições de status;
- validação centralizada de transições;
- endpoint para consultar histórico do pedido;
- fluxo `WAITING_STOCK -> WAITING_FRAUD -> CONFIRMED/REJECTED`.

Estados atuais:

```text
CREATED
WAITING_STOCK
WAITING_FRAUD
CONFIRMED
CANCELED
REJECTED
```

O que ainda falta:

- tabela explícita de instância da Saga;
- compensações;
- timeout por etapa;
- correlação entre eventos;
- política de retry por etapa;
- dead letter queues;
- painel para visualizar Sagas em andamento.

Branch concluída parcialmente:

```text
feature/order-saga-flow
```

Evolução futura:

```text
feature/order-saga-compensation
```

---

## TD-007: Reserva e baixa de estoque

**Status:** concluído inicialmente na feature `product-stock-events`.

O `product-api` passou a consumir eventos de reserva de estoque e publicar resultado:

```text
product.stock.reserve
  ↓
product-api
  ↓
stock.reserved ou stock.rejected
```

O `order-api` passou a aguardar estoque antes de enviar o pedido para análise de fraude.

Evoluções futuras:

- idempotência por pedido e produto;
- tabela de reservas;
- compensação de estoque;
- controle de concorrência;
- auditoria de estoque;
- endpoint para visualizar reservas.

Branch concluída:

```text
feature/product-stock-events
```

---

## TD-008: Integração com user-api ainda é simplificada

**Status:** aberto.

O `order-api` recebe `userId`, mas ainda não valida se o usuário existe no `user-api`.

Correção futura:

- criar REST Client para o `user-api`;
- validar usuário antes de criar pedido;
- tratar usuário inexistente;
- futuramente propagar autenticação JWT entre serviços.

Serviços impactados:

- order-api;
- user-api.

Branch planejada:

```text
feature/order-user-validation
```

---

## TD-009: Segurança entre microsserviços ainda não implementada

**Status:** aberto.

Atualmente os serviços se comunicam livremente em ambiente local.

Em uma evolução futura, será necessário proteger chamadas internas.

Possíveis abordagens:

- JWT entre serviços;
- client credentials;
- API keys internas;
- mTLS em Kubernetes;
- service mesh futuramente.

Serviços impactados:

- todos os microsserviços.

Branch planejada:

```text
feature/service-to-service-security
```

---

## TD-010: Observabilidade

**Status:** concluído inicialmente na feature `observability`.

A stack local de observabilidade foi adicionada com:

- OpenTelemetry Collector;
- Tempo;
- Prometheus;
- Grafana;
- instrumentação dos serviços Quarkus com OpenTelemetry;
- métricas via Micrometer/Prometheus.

Evoluções futuras:

- dashboards customizados por serviço;
- métricas de negócio;
- logs centralizados com Loki;
- correlation ID nos logs;
- trace de eventos RabbitMQ com melhor correlação;
- alertas no Prometheus/Grafana;
- documentação de troubleshooting.

Branch concluída inicialmente:

```text
feature/observability
```

Evolução futura:

```text
feature/observability-dashboards
```

---

## TD-011: Tratamento global de erros ainda precisa ser padronizado

**Status:** aberto.

Os serviços já lançam exceções como `BadRequestException`, `NotFoundException` e `WebApplicationException`, mas ainda não há um modelo global padronizado de erro.

Correção futura:

- criar `ErrorResponse`;
- criar `ExceptionMapper`;
- padronizar mensagens;
- incluir timestamp, path, status e código de erro;
- evitar respostas inconsistentes entre serviços.

Exemplo de resposta futura:

```json
{
  "timestamp": "2026-05-25T18:00:00",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Estoque insuficiente para o produto 1",
  "path": "/orders"
}
```

Branch planejada:

```text
feature/error-handling-standardization
```

---

## TD-012: Testes automatizados ainda precisam ser criados

**Status:** aberto.

Até o momento, a validação está sendo feita principalmente com `curl` e testes manuais.

Será necessário criar testes automatizados.

Tipos de teste previstos:

- testes unitários dos services;
- testes de resource;
- testes de repository;
- testes com banco usando Testcontainers ou Dev Services;
- testes de integração com RabbitMQ;
- testes de fluxo completo;
- testes do KrakenD;
- testes de Outbox.

Serviços impactados:

- todos os microsserviços.

Branch planejada:

```text
feature/automated-tests
```

---

## TD-013: Evoluir delivery-estimator-api com dados da Olist

**Status:** aberto.

O `delivery-estimator-api` usa atualmente uma abordagem simples baseada em rotas manuais e fallback.

A proposta futura é usar a base da Olist para calcular estimativas mais realistas.

Evolução prevista:

- importar dados da Olist;
- calcular medianas por origem e destino;
- alimentar a tabela `delivery_route_estimates`;
- criar baseline estatístico;
- comparar estimativa manual vs baseline baseado em dados;
- preparar dataset para modelo preditivo.

Branch planejada:

```text
feature/olist-delivery-baseline
```

---

## TD-014: Criar pipeline de dados para estimativa de entrega

**Status:** aberto.

A carga da base Olist ainda não possui pipeline automatizado.

Possíveis opções:

- script Java;
- script Python;
- Quarkus CLI command;
- serviço batch;
- pipeline com Prefect futuramente.

Saída esperada:

```text
dados Olist
  ↓
tratamento
  ↓
cálculo de prazos por rota
  ↓
carga em delivery.delivery_route_estimates
```

Branch planejada:

```text
feature/delivery-data-pipeline
```

---

## TD-015: Criar API Gateway com KrakenD

**Status:** concluído inicialmente na feature `api-gateway-krakend`.

O KrakenD foi adicionado como API Gateway do sistema, expondo uma porta única para acesso às APIs principais.

Porta local:

```text
http://localhost:8099
```

Responsabilidades atuais:

- expor rotas `/api/products`;
- expor rotas `/api/orders`;
- expor rotas `/api/delivery-estimates`;
- expor consultas de fraude;
- esconder portas internas dos serviços.

Evoluções futuras:

- autenticação JWT;
- rate limit;
- logs de acesso;
- headers de correlação;
- políticas de timeout;
- agregação de respostas;
- documentação das rotas públicas.

Branch concluída inicialmente:

```text
feature/api-gateway-krakend
```

---

## TD-016: Melhorar versionamento dos contratos de API e eventos

**Status:** aberto.

Os contratos REST e eventos RabbitMQ ainda estão implícitos no código.

No futuro, será necessário documentar e versionar os contratos.

Itens previstos:

- OpenAPI por serviço;
- AsyncAPI para eventos RabbitMQ;
- versionamento de payloads;
- documentação de routing keys;
- documentação de exchanges e filas.

Exemplos:

```text
REST:
GET /products/{id}
POST /orders

Eventos:
order.created.v1
order.canceled.v1
stock.reserved.v1
fraud.approved.v1
```

Branch planejada:

```text
feature/api-event-contracts
```

---

## TD-017: Melhorar resiliência nas chamadas REST

**Status:** aberto.

O `order-api` chama o `product-api` e o `delivery-estimator-api` via REST, mas ainda sem políticas explícitas de resiliência.

No futuro, aplicar:

- timeout;
- retry controlado;
- circuit breaker;
- fallback;
- bulkhead se necessário.

Serviços impactados:

- order-api;
- futuras integrações síncronas.

Branch planejada:

```text
feature/rest-client-resilience
```

---

## TD-018: Padronizar nomes dos serviços e bounded contexts

**Status:** aberto.

O projeto evoluiu ao longo das features e alguns nomes/conceitos podem precisar de revisão para alinhamento com bounded contexts.

Sugestão de padronização:

```text
user-api
product-api
order-api
delivery-estimator-api
fraud-detector-api
notification-api
api-gateway
ai-chat-bff
```

Branch planejada:

```text
feature/service-naming-cleanup
```

---

## TD-019: Criar documentação arquitetural do monorepo

**Status:** aberto.

O projeto ainda precisa de documentação arquitetural consolidada.

Itens previstos:

- visão geral da arquitetura;
- diagrama C4 Context;
- diagrama C4 Container;
- diagrama de sequência da criação de pedido;
- diagrama de eventos RabbitMQ;
- diagrama da Saga;
- diagrama da Outbox;
- decisões arquiteturais;
- registro de ADRs.

Branch planejada:

```text
feature/architecture-docs
```

---

## TD-020: Criar ADRs para decisões arquiteturais

**Status:** aberto.

As decisões principais ainda estão documentadas de forma informal.

Criar ADRs para registrar decisões como:

- uso de monorepo;
- uso de Quarkus;
- uso de PostgreSQL único com schema por serviço;
- uso de RabbitMQ;
- uso de KrakenD;
- uso de OpenTelemetry;
- uso de Saga;
- uso de Transactional Outbox;
- uso futuro de Kubernetes;
- uso futuro de LangChain4j no BFF.

Branch planejada:

```text
feature/architecture-decision-records
```

---

## TD-021: Melhorar o modelo de banco e migrações

**Status:** concluído parcialmente na feature `database-migrations`.

O `order-api` passou a usar Flyway para controlar o schema `orders`.

Motivação:

- o uso de `quarkus.hibernate-orm.database.generation=update` gerou problemas na evolução do enum `OrderStatus`;
- constraints do banco precisaram ser ajustadas manualmente;
- produção exige evolução controlada de schema.

Ainda falta migrar para Flyway/Liquibase nos demais serviços:

- product-api;
- delivery-estimator-api;
- fraud-detector-api;
- user-api.

Branch concluída parcialmente:

```text
feature/database-migrations
```

Evolução futura:

```text
feature/database-migrations-all-services
```

---

## TD-022: Criar seed de dados para desenvolvimento

**Status:** aberto.

Hoje os dados de produtos, rotas de entrega e pedidos são criados manualmente com `curl`.

Futuramente, criar scripts de seed.

Exemplos:

- produtos iniciais;
- rotas SP -> SE;
- rotas SP -> RJ;
- rotas SP -> BA;
- usuário de teste;
- pedidos de exemplo;
- dados para fraude;
- dados para estoque.

Branch planejada:

```text
feature/dev-seed-data
```

---

## TD-023: Criar coleções de teste HTTP

**Status:** aberto.

Atualmente os testes manuais são feitos com comandos `curl`.

Para facilitar aulas e validações, criar coleções HTTP.

Opções:

- arquivo `.http` para IntelliJ;
- coleção Postman;
- coleção Insomnia;
- Bruno.

Branch planejada:

```text
feature/http-client-collections
```

---

## TD-024: Implementar serviço de análise de fraude

**Status:** concluído inicialmente na feature `fraud-detector-api`.

O `fraud-detector-api` já:

- consome `order.created`;
- simula regra de fraude;
- salva análise no schema `fraud`;
- publica `fraud.approved` ou `fraud.rejected`.

Evoluções futuras:

- melhorar regras de fraude;
- adicionar modelo de score;
- usar histórico do cliente;
- implementar Outbox;
- implementar idempotência;
- melhorar explicabilidade da decisão.

Branch concluída:

```text
feature/fraud-detector-api
```

---

## TD-025: Implementar notification-api

**Status:** aberto.

Ainda não existe serviço de notificação.

Após a publicação de eventos, pode ser criado um consumidor para enviar notificações ou apenas registrar mensagens.

Eventos de interesse:

- order.created;
- order.canceled;
- fraud.rejected;
- order.confirmed;
- stock.rejected.

Branch planejada:

```text
feature/notification-api
```

---

## TD-026: Implementar Outbox também no fraud-detector-api

**Status:** aberto.

O `fraud-detector-api` consome `order.created`, salva a análise de fraude e publica `fraud.approved` ou `fraud.rejected` diretamente no RabbitMQ.

Para maior confiabilidade, a publicação do resultado da fraude também deve usar Transactional Outbox.

Branch planejada:

```text
feature/fraud-outbox-pattern
```

---

## TD-027: Implementar controle idempotente de reserva de estoque

**Status:** próxima feature.

O `product-api` realiza a reserva de estoque ao consumir `product.stock.reserve`, mas ainda não possui controle idempotente por pedido e produto.

Se uma mensagem for reprocessada, o estoque pode ser baixado mais de uma vez.

Evoluções previstas:

- criar tabela `products.stock_reservations`;
- registrar `orderId`, `productId` e quantidade reservada;
- impedir reserva duplicada;
- retornar sucesso idempotente se a reserva já tiver ocorrido;
- permitir compensação futura de estoque;
- publicar eventos com correlationId.

Branch planejada:

```text
feature/stock-reservation-idempotency
```

---

## TD-028: Evoluir Saga para suporte a compensações

**Status:** aberto.

A feature `order-saga-flow` adicionou histórico de transições e validação de estados, mas ainda não implementa compensações completas.

Evoluções futuras:

- devolver estoque quando pedido for cancelado após reserva;
- expirar pedidos presos em `WAITING_STOCK` ou `WAITING_FRAUD`;
- criar timeout por etapa;
- criar correlationId em todos os eventos;
- criar tabela explícita de instância da Saga;
- adicionar dead letter queues;
- implementar retry controlado por etapa.

Branch planejada:

```text
feature/order-saga-compensation
```

---

## TD-029: BFF Conversacional com LangChain4j

**Status:** planejado.

Criar um serviço `ai-chat-bff` em Quarkus usando LangChain4j para permitir interação conversacional com o ecommerce.

O serviço atuará como um BFF inteligente, expondo um endpoint de chat e utilizando ferramentas internas para:

- consultar produtos;
- consultar preços;
- estimar entrega;
- criar pedidos;
- consultar status;
- cancelar pedidos quando permitido.

Branch planejada:

```text
feature/ai-chat-bff-langchain4j
```

Fora do escopo inicial:

- pagamento;
- atendimento humano;
- integração com WhatsApp;
- fine-tuning;
- recomendação avançada.

---

## TD-030: Refatorar delivery-estimator-api para usar Tribuo

**Status:** planejado.

O `delivery-estimator-api` atualmente utiliza rotas cadastradas manualmente e fallback simples.

Como evolução futura, o serviço deve ser refatorado para usar Tribuo, biblioteca Java de Machine Learning, para treinar e executar um modelo de regressão capaz de estimar prazo de entrega com base em dados históricos.

Escopo previsto:

- importar dados da Olist;
- preparar dataset de treino;
- criar features como estado de origem, estado de destino, distância aproximada e quantidade de itens;
- treinar modelo de regressão com Tribuo;
- salvar modelo treinado;
- carregar modelo no startup do `delivery-estimator-api`;
- manter fallback manual quando o modelo não tiver confiança suficiente;
- expor versão do modelo na resposta de estimativa.

Branch planejada:

```text
feature/delivery-estimator-tribuo
```

Fora do escopo inicial:

- deep learning;
- Python;
- treinamento distribuído;
- retreinamento automático;
- feature store.

---

## TD-031: Implementar Outbox nos demais serviços

**Status:** aberto.

O padrão Transactional Outbox foi implementado inicialmente apenas no `order-api`.

Ainda falta avaliar o uso do mesmo padrão nos serviços que também publicam eventos:

- product-api;
- fraud-detector-api.

Branch planejada:

```text
feature/outbox-other-services
```

---

## TD-032: Melhorar observabilidade de negócio

**Status:** aberto.

A stack técnica de observabilidade já existe, mas ainda faltam métricas de negócio.

Métricas possíveis:

- pedidos criados por minuto;
- pedidos confirmados;
- pedidos rejeitados por estoque;
- pedidos rejeitados por fraude;
- tempo médio de confirmação;
- eventos pendentes na outbox;
- tentativas de publicação da outbox;
- reservas de estoque por produto.

Branch planejada:

```text
feature/business-metrics
```

---

## Priorização sugerida

Próximas features recomendadas:

1. `feature/stock-reservation-idempotency`
2. `feature/rest-client-resilience`
3. `feature/error-handling-standardization`
4. `feature/automated-tests`
5. `feature/database-migrations-all-services`
6. `feature/fraud-outbox-pattern`
7. `feature/delivery-estimator-tribuo`
8. `feature/architecture-docs`
9. `feature/kubernetes-deployment`
10. `feature/ai-chat-bff-langchain4j`

---

## Roadmap resumido

```text
Confiabilidade:
  - stock-reservation-idempotency
  - fraud-outbox-pattern
  - outbox-other-services
  - event-idempotency

Qualidade:
  - automated-tests
  - error-handling-standardization
  - api-event-contracts
  - architecture-docs

Produção:
  - database-migrations-all-services
  - service-to-service-security
  - kubernetes-deployment
  - native-dockerfiles

IA / Dados:
  - olist-delivery-baseline
  - delivery-data-pipeline
  - delivery-estimator-tribuo
  - ai-chat-bff-langchain4j

Observabilidade:
  - observability-dashboards
  - business-metrics
  - logs-centralized-loki
```
