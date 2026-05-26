# 02 — Monorepo e Fluxo com Git

## O que é um monorepo?

Um monorepo é um repositório único contendo múltiplos projetos ou serviços.

Neste projeto, o monorepo contém:

```text
user-api/
product-api/
order-api/
delivery-estimator-api/
fraud-detector-api/
gateway/
infra/
tests/
docs/
docker-compose.yml
```

## Por que usar monorepo neste projeto?

Para fins didáticos, o monorepo traz vantagens:

- facilita a navegação;
- facilita execução local;
- mantém os contratos entre serviços no mesmo repositório;
- facilita criação de branches por feature;
- reduz complexidade inicial de CI/CD.

## Organização das pastas

### `user-api/`

Microsserviço de usuários e autenticação.

### `product-api/`

Microsserviço de produtos, estoque e reserva idempotente.

### `order-api/`

Microsserviço central do fluxo de pedido.

### `delivery-estimator-api/`

Microsserviço de estimativa de prazo.

### `fraud-detector-api/`

Microsserviço de análise de fraude.

### `gateway/`

Configuração do KrakenD.

### `infra/`

Configurações de infraestrutura:

- PostgreSQL;
- RabbitMQ;
- observabilidade;
- migrations auxiliares.

### `tests/http/`

Suíte de testes HTTP/JSON.

### `docs/`

Documentação técnica e apostila.

## Branches usadas

O fluxo adotado usa branches curtas por feature.

Exemplo:

```bash
git checkout develop
git pull origin develop
git checkout -b feature/nome-da-feature
```

Ao finalizar:

```bash
git add .
git commit -m "feat: descrição da feature"
git push -u origin feature/nome-da-feature
gh pr create --base develop --head feature/nome-da-feature
```

Após o merge:

```bash
git checkout develop
git pull origin develop
```

## Branches criadas até aqui

Entre as features já implementadas, estão:

```text
feature/product-api
feature/order-api
feature/order-delivery-integration
feature/rabbitmq-order-events
feature/fraud-detector-api
feature/order-fraud-integration
feature/product-stock-events
feature/order-saga-flow
feature/dockerize-services
feature/api-gateway-krakend
feature/observability
feature/order-outbox-pattern
feature/stock-reservation-idempotency
feature/rest-client-resilience
feature/error-handling-standardization
feature/api-gateway-only-access
feature/gateway-jwt-auth
feature/http-json-test-suite
```

## Convenção de commits

Exemplos:

```text
feat: add product api
feat: add transactional outbox to order api
fix: adjust rabbitmq routing key
docs: update technical debt roadmap
test: add http json test suite
chore: expose services only through api gateway
```

## Por que fazer PR mesmo trabalhando sozinho?

Mesmo em projeto individual, PR ajuda a:

- registrar histórico da feature;
- revisar alterações;
- organizar evolução do projeto;
- criar rastreabilidade;
- facilitar explicação didática em aula.

## Boas práticas adotadas

- uma feature por branch;
- pequenos commits;
- PR para `develop`;
- merge após teste manual;
- documentação atualizada após features importantes;
- testes HTTP versionados.
