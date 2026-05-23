# Technical Debt

Este documento registra os principais débitos técnicos identificados durante a construção incremental do monorepo de microsserviços do sistema de pedidos.

A estratégia atual é priorizar aprendizado, evolução incremental e funcionamento ponta a ponta antes de endurecer aspectos de produção, como Dockerfiles, Kubernetes, observabilidade avançada, resiliência, Saga e Outbox.

---

## TD-001: Dockerizar os microsserviços

Neste momento, o Docker Compose será usado apenas para infraestrutura local, como PostgreSQL e RabbitMQ.

Os microsserviços serão executados manualmente durante o desenvolvimento.

Após a implementação inicial dos serviços principais, será criada uma branch específica para Dockerfiles e inclusão dos serviços no Docker Compose.

Serviços impactados:

- user-api
- product-api
- order-api
- delivery-estimator-api
- futuros serviços, como fraud-detector-api

Branch planejada:

```text
feature/dockerize-services
```

---

## TD-002: Criar imagens nativas com Quarkus

O Quarkus gera exemplos de Dockerfiles para execução JVM e execução nativa. No momento, optamos por não usar builds nativos para evitar complexidade adicional durante a construção funcional dos serviços.

Após os serviços estarem funcionando em modo JVM, deverá ser avaliada a criação de imagens nativas usando Quarkus Native, GraalVM/Mandrel e a imagem `ubi9-quarkus-micro-image`.

Objetivo futuro:

- reduzir tempo de inicialização;
- reduzir consumo de memória;
- preparar melhor os serviços para Kubernetes;
- comparar imagem JVM vs imagem nativa.

Branch planejada:

```text
feature/native-dockerfiles
```

---

## TD-003: Criar manifests Kubernetes ou Helm Charts

Após a dockerização dos serviços, será necessário criar a estrutura de implantação em Kubernetes.

A primeira versão pode usar manifests YAML simples. Depois, pode evoluir para Helm Charts ou Kustomize.

Itens previstos:

- Deployment por serviço;
- Service por serviço;
- ConfigMap;
- Secret;
- Ingress ou Gateway;
- readinessProbe;
- livenessProbe;
- resource requests e limits;
- namespaces separados;
- configuração de variáveis de ambiente.

Branch planejada:

```text
feature/kubernetes-deployment
```

---

## TD-004: Implementar Transactional Outbox no order-api

Atualmente o `order-api` publica eventos diretamente no RabbitMQ após persistir o pedido.

Esse modelo é suficiente para estudo inicial, mas pode gerar inconsistência em produção.

Exemplo de risco:

- o pedido é salvo no banco;
- a publicação no RabbitMQ falha;
- o sistema fica com pedido criado, mas sem evento `order.created`.

Para maior confiabilidade, implementar o padrão Transactional Outbox.

Fluxo desejado:

```text
salva pedido
salva evento em tabela outbox_events na mesma transação
worker publica evento no RabbitMQ
marca evento como publicado
```

Serviço impactado:

- order-api

Branch planejada:

```text
feature/order-outbox-pattern
```

---

## TD-005: Implementar idempotência no consumo e publicação de eventos

Os eventos publicados no RabbitMQ ainda não possuem uma estratégia completa de idempotência.

Embora os eventos tenham `eventId`, ainda não existe controle para evitar reprocessamento duplicado por consumidores futuros.

Quando forem criados consumidores, como `fraud-detector-api`, `notification-api` ou integração de estoque, será necessário registrar eventos processados.

Itens previstos:

- criar tabela de eventos processados;
- usar `eventId` como chave de idempotência;
- evitar aplicar a mesma mudança duas vezes;
- tratar retry sem duplicidade de efeito.

Branch planejada:

```text
feature/event-idempotency
```

---

## TD-006: Implementar Saga para o fluxo de pedido

Atualmente o `order-api` confirma o pedido de forma simples após validar produto, estoque e entrega.

No futuro, o fluxo de pedido deve evoluir para uma Saga, especialmente quando houver reserva de estoque, análise de fraude e confirmação final.

Fluxo futuro esperado:

```text
OrderCreated
  ↓
StockReservationRequested
  ↓
StockReserved ou StockRejected
  ↓
FraudAnalysisRequested
  ↓
FraudApproved ou FraudRejected
  ↓
OrderConfirmed ou OrderCanceled
```

Estados futuros do pedido:

```text
CREATED
WAITING_STOCK
WAITING_FRAUD
CONFIRMED
CANCELED
REJECTED
```

Branch planejada:

```text
feature/order-saga-flow
```

---

## TD-007: Baixa e reserva de estoque ainda não implementadas

O `order-api` atualmente consulta o `product-api` para verificar se há estoque suficiente, mas não realiza reserva nem baixa efetiva de estoque.

Isso significa que, em concorrência, dois pedidos podem consultar o mesmo estoque disponível.

Correção futura:

- criar endpoint ou consumidor no `product-api` para reservar estoque;
- publicar eventos de reserva;
- aplicar controle transacional no produto;
- retornar `stock.reserved` ou `stock.rejected`.

Serviços impactados:

- product-api
- order-api

Branch planejada:

```text
feature/product-stock-events
```

---

## TD-008: Integração com user-api ainda é simplificada

O `order-api` recebe `userId`, mas ainda não valida se o usuário existe no `user-api`.

No momento, o campo é aceito diretamente para simplificar o fluxo inicial.

Correção futura:

- criar REST Client para o `user-api`;
- validar usuário antes de criar pedido;
- tratar usuário inexistente;
- futuramente, propagar autenticação JWT entre serviços.

Serviços impactados:

- order-api
- user-api

Branch planejada:

```text
feature/order-user-validation
```

---

## TD-009: Segurança entre microsserviços ainda não implementada

Atualmente os serviços se comunicam livremente em ambiente local.

Ainda não existe autenticação ou autorização entre serviços.

Em uma evolução futura, será necessário proteger chamadas internas.

Possíveis abordagens:

- JWT entre serviços;
- client credentials;
- API keys internas;
- mTLS em ambiente Kubernetes;
- service mesh, futuramente.

Serviços impactados:

- todos os microsserviços

Branch planejada:

```text
feature/service-to-service-security
```

---

## TD-010: Observabilidade ainda não implementada

Os serviços ainda não possuem observabilidade padronizada.

No futuro, será importante adicionar:

- logs estruturados;
- correlation ID;
- tracing distribuído com OpenTelemetry;
- métricas Prometheus;
- dashboards no Grafana;
- rastreamento de chamadas REST e eventos RabbitMQ.

Serviços impactados:

- user-api
- product-api
- order-api
- delivery-estimator-api
- futuros consumidores

Branch planejada:

```text
feature/observability
```

---

## TD-011: Tratamento global de erros ainda pode ser padronizado

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
  "timestamp": "2026-05-22T18:00:00",
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

Até o momento, a validação está sendo feita principalmente com `curl` e testes manuais.

Será necessário criar testes automatizados para cada serviço.

Tipos de teste previstos:

- testes unitários dos services;
- testes de resource;
- testes de repository;
- testes com banco usando Testcontainers ou Dev Services;
- testes de integração entre serviços;
- testes de publicação de eventos RabbitMQ.

Serviços impactados:

- todos os microsserviços

Branch planejada:

```text
feature/automated-tests
```

---

## TD-013: Evoluir delivery-estimator-api com dados da Olist

O `delivery-estimator-api` usa atualmente uma abordagem simples baseada em rotas manuais e fallback.

A proposta futura é usar a base da Olist para calcular estimativas mais realistas.

Evolução prevista:

- importar dados da Olist;
- calcular medianas por origem e destino;
- alimentar a tabela `delivery_route_estimates`;
- criar baseline estatístico;
- futuramente treinar modelo de machine learning.

Branch planejada:

```text
feature/olist-delivery-baseline
```

---

## TD-014: Criar pipeline de dados para estimativa de entrega

A carga da base Olist ainda não possui pipeline automatizado.

No futuro, será interessante criar um pipeline separado para importar, tratar e carregar as rotas estimadas.

Possíveis opções:

- script Java;
- script Python;
- Quarkus CLI command;
- serviço batch;
- pipeline com Prefect, futuramente.

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

## TD-015: Criar API Gateway ou BFF

Atualmente os serviços são acessados diretamente por porta:

```text
user-api                -> 8094
product-api             -> 8095
order-api               -> 8096
delivery-estimator-api  -> 8097
```

Para uma aplicação real ou frontend, o ideal é criar um API Gateway ou BFF.

Responsabilidades futuras:

- centralizar entrada das chamadas;
- esconder topologia dos microsserviços;
- validar autenticação;
- encaminhar requisições;
- aplicar rate limit;
- agregar respostas, quando necessário.

Branch planejada:

```text
feature/api-gateway
```

---

## TD-016: Melhorar versionamento dos contratos de API e eventos

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

O `order-api` chama o `product-api` e o `delivery-estimator-api` via REST, mas ainda sem políticas explícitas de resiliência.

No futuro, aplicar:

- timeout;
- retry controlado;
- circuit breaker;
- fallback;
- bulkhead, se necessário.

Serviços impactados:

- order-api
- futuras integrações síncronas

Branch planejada:

```text
feature/rest-client-resilience
```

---

## TD-018: Padronizar nomes dos serviços e bounded contexts

O projeto começou com nomes como `shopping-api`, mas evoluiu para `order-api`.

Será importante manter os nomes alinhados aos bounded contexts.

Sugestão de padronização:

```text
user-api
product-api
order-api
delivery-estimator-api
fraud-detector-api
notification-api
api-gateway
```

Branch planejada:

```text
feature/service-naming-cleanup
```

---

## TD-019: Criar documentação arquitetural do monorepo

O projeto ainda precisa de documentação arquitetural consolidada.

Itens previstos:

- visão geral da arquitetura;
- diagrama C4 Context;
- diagrama C4 Container;
- diagrama de sequência da criação de pedido;
- diagrama de eventos RabbitMQ;
- decisões arquiteturais;
- registro de ADRs.

Branch planejada:

```text
feature/architecture-docs
```

---

## TD-020: Criar ADRs para decisões arquiteturais

As decisões principais ainda estão documentadas apenas de forma informal.

Criar ADRs para registrar decisões como:

- uso de monorepo;
- uso de Quarkus;
- uso de PostgreSQL único com schema por serviço;
- uso de RabbitMQ;
- execução manual dos serviços durante desenvolvimento;
- Dockerização posterior;
- Kubernetes posterior;
- REST síncrono antes de eventos assíncronos;
- uso futuro de Saga.

Branch planejada:

```text
feature/architecture-decision-records
```

---

## TD-021: Melhorar o modelo de banco e migrações

Atualmente usamos:

```properties
quarkus.hibernate-orm.database.generation=update
```

Isso é conveniente durante o desenvolvimento, mas não é recomendado como estratégia principal para ambientes controlados.

Futuramente, adotar ferramenta de migração.

 Observação: durante a evolução do `OrderStatus`, foi necessário atualizar manualmente a constraint `customer_orders_status_check`, pois o `hibernate-orm.database.generation=update` não recriou corretamente a constraint do enum. Esse caso reforça a necessidade de adoção de Flyway ou Liquibase.

Opções:

- Flyway;
- Liquibase.

Objetivo:

- controlar evolução do schema;
- versionar scripts SQL;
- evitar mudanças automáticas indesejadas;
- preparar o projeto para produção.

Branch planejada:

```text
feature/database-migrations
```

---

## TD-022: Criar seed de dados para desenvolvimento

Hoje os dados de produtos, rotas de entrega e pedidos são criados manualmente com `curl`.

Futuramente, criar scripts de seed.

Exemplos:

- produtos iniciais;
- rotas SP -> SE;
- rotas SP -> RJ;
- rotas SP -> BA;
- usuário de teste;
- pedidos de exemplo.

Branch planejada:

```text
feature/dev-seed-data
```

---

## TD-023: Criar coleções de teste HTTP

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

O serviço `fraud-detector-api` ainda não foi implementado.

Evolução prevista:

- consumir evento `order.created`;
- simular regra de fraude;
- publicar `fraud.approved` ou `fraud.rejected`;
- futuramente usar score ou modelo de ML simples.

Branch planejada:

```text
feature/fraud-detector-api
```

---

## TD-025: Implementar notification-api

Ainda não existe serviço de notificação.

Após a publicação de eventos, pode ser criado um consumidor para enviar notificações ou apenas registrar mensagens.

Eventos de interesse:

- order.created;
- order.canceled;
- fraud.rejected;
- order.confirmed.

Branch planejada:

```text
feature/notification-api
```

---

## Priorização sugerida

Antes de avançar para Kubernetes, priorizar:

1. TD-001: Dockerizar os microsserviços.
2. TD-004: Implementar Transactional Outbox.
3. TD-010: Observabilidade.
4. TD-012: Testes automatizados.
5. TD-021: Migrações de banco.

## TD-026: Implementar Outbox também no fraud-detector-api

O fraud-detector-api consome `order.created`, salva a análise de fraude e publica `fraud.approved` ou `fraud.rejected` diretamente no RabbitMQ.

Para maior confiabilidade, a publicação do resultado da fraude também deve usar Transactional Outbox.

Branch planejada:

```text
feature/fraud-outbox-pattern

```

---

## 5. Verifique o status do Git

Na raiz do monorepo:  

```bash
git status
```