# Technical Debt - Pendências Atuais

Este documento lista apenas os débitos técnicos, melhorias pendentes e features futuras ainda relevantes para o monorepo de microsserviços do sistema de pedidos.

Itens já concluídos, como Dockerização JVM, KrakenD, Observabilidade inicial, Saga básica, Transactional Outbox no `order-api` e Idempotência inicial de reserva de estoque, foram removidos da lista principal para manter o backlog técnico mais limpo.

---

## Itens já concluídos e removidos da lista principal

Os itens abaixo já foram implementados em features anteriores e não aparecem mais como débitos pendentes principais:

```text
feature/dockerize-services
feature/api-gateway-krakend
feature/observability
feature/order-saga-flow
feature/product-stock-events
feature/order-outbox-pattern
feature/stock-reservation-idempotency
feature/database-migrations
feature/fraud-detector-api
feature/order-fraud-integration
feature/order-delivery-integration
feature/rabbitmq-order-events
```

Observação: alguns itens concluídos ainda possuem evoluções futuras, como compensações da Saga, Outbox em outros serviços, dashboards customizados e migrações dos demais schemas. Essas evoluções aparecem como novos débitos pendentes específicos.

---

# Débitos técnicos pendentes

---

## TD-001: Criar imagens nativas com Quarkus

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

## TD-002: Criar manifests Kubernetes ou Helm Charts

**Status:** aberto.

Após a dockerização dos serviços, será necessário criar a estrutura de implantação em Kubernetes.

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
- integração com stack de observabilidade;
- estratégia de rollout e rollback.

Branch planejada:

```text
feature/kubernetes-deployment
```

---

## TD-003: Implementar idempotência geral no consumo de eventos

**Status:** aberto.

A reserva de estoque já recebeu controle idempotente inicial, mas ainda falta uma estratégia geral de idempotência para os consumidores de eventos.

Riscos:

- uma mensagem pode ser reentregue;
- um consumidor pode aplicar o mesmo efeito duas vezes;
- eventos de fraude podem tentar alterar status já finalizado;
- eventos futuros de notificação podem ser enviados em duplicidade.

Itens previstos:

- criar tabela de eventos processados por serviço;
- usar `eventId` como chave de idempotência;
- impedir efeitos duplicados;
- tratar reentrega sem inconsistência;
- padronizar estratégia de retry;
- criar métricas para eventos duplicados.

Branch planejada:

```text
feature/event-idempotency
```

---

## TD-004: Evoluir Saga para suporte a compensações

**Status:** aberto.

A Saga básica do pedido já possui histórico de transições e validação de estados, mas ainda não implementa compensações completas.

Evoluções futuras:

- devolver estoque quando pedido for cancelado após reserva;
- expirar pedidos presos em `WAITING_STOCK` ou `WAITING_FRAUD`;
- criar timeout por etapa;
- criar `correlationId` em todos os eventos;
- criar tabela explícita de instância da Saga;
- adicionar dead letter queues;
- implementar retry controlado por etapa;
- criar endpoint para visualizar Sagas em andamento.

Branch planejada:

```text
feature/order-saga-compensation
```

---

## TD-005: Integração do order-api com user-api

**Status:** aberto.

O `order-api` recebe `userId`, mas ainda não valida se o usuário existe no `user-api`.

Correção futura:

- criar REST Client para o `user-api`;
- validar usuário antes de criar pedido;
- tratar usuário inexistente;
- futuramente propagar autenticação JWT entre serviços;
- avaliar fallback ou indisponibilidade do `user-api`.

Serviços impactados:

- order-api;
- user-api.

Branch planejada:

```text
feature/order-user-validation
```

---

## TD-006: Segurança entre microsserviços

**Status:** aberto.

Atualmente os serviços se comunicam livremente em ambiente local.

Em uma evolução futura, será necessário proteger chamadas internas.

Possíveis abordagens:

- JWT entre serviços;
- client credentials;
- API keys internas;
- mTLS em Kubernetes;
- service mesh futuramente;
- autenticação e autorização no KrakenD.

Serviços impactados:

- todos os microsserviços.

Branch planejada:

```text
feature/service-to-service-security
```

---

## TD-007: Padronizar tratamento global de erros

**Status:** aberto.

Os serviços já lançam exceções como `BadRequestException`, `NotFoundException`, `ServiceUnavailableException` e `WebApplicationException`, mas ainda não há um modelo global padronizado de erro.

Correção futura:

- criar `ErrorResponse`;
- criar `ExceptionMapper`;
- padronizar mensagens;
- incluir timestamp, path, status e código de erro;
- evitar respostas inconsistentes entre serviços;
- melhorar comportamento do KrakenD diante de erros dos backends.

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

## TD-008: Criar testes automatizados

**Status:** aberto.

Até o momento, a validação está sendo feita principalmente com `curl` e testes manuais.

Será necessário criar testes automatizados.

Tipos de teste previstos:

- testes unitários dos services;
- testes de resources;
- testes de repositories;
- testes com banco usando Testcontainers ou Dev Services;
- testes de integração com RabbitMQ;
- testes de fluxo completo;
- testes do KrakenD;
- testes de Outbox;
- testes de idempotência de estoque;
- testes de resiliência REST.

Serviços impactados:

- todos os microsserviços.

Branch planejada:

```text
feature/automated-tests
```

---

## TD-009: Migrar todos os serviços para Flyway ou Liquibase

**Status:** aberto.

O `order-api` já usa Flyway para controlar o schema `orders`, mas os demais serviços ainda dependem de criação automática ou scripts manuais.

Ainda falta migrar:

- product-api;
- delivery-estimator-api;
- fraud-detector-api;
- user-api.

Objetivos:

- remover dependência de `quarkus.hibernate-orm.database.generation=update`;
- versionar schemas por serviço;
- padronizar migrations;
- facilitar execução em CI/CD;
- reduzir risco de inconsistência em ambientes.

Branch planejada:

```text
feature/database-migrations-all-services
```

---

## TD-010: Melhorar resiliência nas chamadas REST

**Status:** em andamento.

O `order-api` chama o `product-api` e o `delivery-estimator-api` via REST.

A feature em andamento adiciona:

- timeout;
- retry;
- circuit breaker;
- fallback controlado.

Decisão de comportamento:

- se o `product-api` estiver indisponível, o pedido não deve ser criado;
- se o `delivery-estimator-api` estiver indisponível, o pedido pode ser criado com estimativa fallback.

Evoluções futuras:

- configurar valores por ambiente;
- adicionar métricas de circuit breaker;
- adicionar testes automatizados;
- padronizar logs de fallback;
- expor falhas no Grafana.

Branch em andamento:

```text
feature/rest-client-resilience
```

---

## TD-011: Criar seed de dados para desenvolvimento

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

## TD-012: Criar coleções de teste HTTP

**Status:** aberto.

Atualmente os testes manuais são feitos com comandos `curl`.

Para facilitar aulas, validações e demonstrações, criar coleções HTTP.

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

## TD-013: Melhorar versionamento dos contratos REST e eventos

**Status:** aberto.

Os contratos REST e eventos RabbitMQ ainda estão implícitos no código.

No futuro, será necessário documentar e versionar os contratos.

Itens previstos:

- OpenAPI por serviço;
- AsyncAPI para eventos RabbitMQ;
- versionamento de payloads;
- documentação de routing keys;
- documentação de exchanges e filas;
- convenção para eventos `.v1`, `.v2` etc.

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

## TD-014: Criar documentação arquitetural do monorepo

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

## TD-015: Criar ADRs para decisões arquiteturais

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
- uso futuro de LangChain4j no BFF;
- uso futuro de Tribuo no `delivery-estimator-api`.

Branch planejada:

```text
feature/architecture-decision-records
```

---

## TD-016: Melhorar observabilidade de negócio

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
- reservas de estoque por produto;
- quantidade de fallback de entrega;
- quantidade de falhas em chamadas REST.

Branch planejada:

```text
feature/business-metrics
```

---

## TD-017: Criar dashboards customizados no Grafana

**Status:** aberto.

A stack Grafana/Prometheus/Tempo já está funcionando, mas ainda não existem dashboards customizados do domínio.

Dashboards sugeridos:

- visão geral dos serviços;
- pedidos por status;
- erros por serviço;
- latência por endpoint;
- eventos RabbitMQ por tipo;
- outbox pendente/publicada/falhada;
- estoque reservado/rejeitado;
- chamadas com fallback.

Branch planejada:

```text
feature/observability-dashboards
```

---

## TD-018: Logs centralizados com Loki

**Status:** aberto.

A observabilidade atual cobre métricas e traces, mas ainda não inclui logs centralizados.

Evolução futura:

- adicionar Loki;
- adicionar Promtail ou OpenTelemetry Logs;
- padronizar logs JSON;
- incluir `traceId`, `spanId` e `correlationId`;
- integrar logs com traces no Grafana.

Branch planejada:

```text
feature/logs-centralized-loki
```

---

## TD-019: Implementar Outbox no fraud-detector-api

**Status:** aberto.

O `fraud-detector-api` consome `order.created`, salva a análise de fraude e publica `fraud.approved` ou `fraud.rejected` diretamente no RabbitMQ.

Para maior confiabilidade, a publicação do resultado da fraude também deve usar Transactional Outbox.

Branch planejada:

```text
feature/fraud-outbox-pattern
```

---

## TD-020: Implementar Outbox nos demais serviços publicadores

**Status:** aberto.

O padrão Transactional Outbox foi implementado inicialmente apenas no `order-api`.

Ainda falta avaliar o uso do mesmo padrão nos serviços que também publicam eventos:

- product-api;
- fraud-detector-api.

Observação: o item específico do `fraud-detector-api` pode ser implementado primeiro e depois a estratégia ser reaproveitada no `product-api`.

Branch planejada:

```text
feature/outbox-other-services
```

---

## TD-021: Criar notification-api

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

## TD-022: Padronizar nomes dos serviços e bounded contexts

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

## TD-023: Evoluir KrakenD

**Status:** aberto.

O KrakenD já está funcionando como API Gateway inicial, mas ainda pode evoluir.

Evoluções futuras:

- autenticação JWT;
- rate limit;
- logs de acesso;
- headers de correlação;
- políticas de timeout;
- agregação de respostas;
- documentação das rotas públicas;
- separação de rotas públicas e administrativas.

Branch planejada:

```text
feature/krakend-hardening
```

---

## TD-024: Evoluir delivery-estimator-api com dados da Olist

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

## TD-025: Criar pipeline de dados para estimativa de entrega

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

## TD-026: Refatorar delivery-estimator-api para usar Tribuo

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

## TD-027: Criar BFF Conversacional com LangChain4j

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

## TD-028: Compensação de estoque

**Status:** aberto.

A reserva de estoque já possui idempotência inicial, mas ainda não há compensação para devolver estoque em cenários de cancelamento ou rejeição posterior.

Cenários:

- pedido cancelado após estoque reservado;
- fraude rejeitada após estoque reservado;
- timeout na etapa de fraude;
- compensação manual administrativa.

Itens previstos:

- criar evento `stock.release.requested`;
- product-api consumir solicitação de devolução;
- atualizar tabela `stock_reservations`;
- devolver quantidade ao estoque;
- publicar `stock.released` ou `stock.release.rejected`.

Branch planejada:

```text
feature/stock-compensation
```

---

# Priorização sugerida

Próximas features recomendadas:

1. `feature/rest-client-resilience`
2. `feature/error-handling-standardization`
3. `feature/automated-tests`
4. `feature/database-migrations-all-services`
5. `feature/fraud-outbox-pattern`
6. `feature/stock-compensation`
7. `feature/business-metrics`
8. `feature/observability-dashboards`
9. `feature/delivery-estimator-tribuo`
10. `feature/architecture-docs`
11. `feature/kubernetes-deployment`
12. `feature/ai-chat-bff-langchain4j`

---

# Roadmap resumido

```text
Confiabilidade:
  - rest-client-resilience
  - fraud-outbox-pattern
  - outbox-other-services
  - event-idempotency
  - stock-compensation
  - order-saga-compensation

Qualidade:
  - error-handling-standardization
  - automated-tests
  - api-event-contracts
  - architecture-docs
  - architecture-decision-records

Produção:
  - database-migrations-all-services
  - service-to-service-security
  - kubernetes-deployment
  - native-dockerfiles
  - krakend-hardening

IA / Dados:
  - olist-delivery-baseline
  - delivery-data-pipeline
  - delivery-estimator-tribuo
  - ai-chat-bff-langchain4j

Observabilidade:
  - business-metrics
  - observability-dashboards
  - logs-centralized-loki
```
