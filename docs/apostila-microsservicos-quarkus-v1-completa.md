# 01 — Visão Geral do Projeto

## Objetivo

O objetivo do projeto é construir um sistema simples de ecommerce baseado em microsserviços, usando tecnologias modernas e práticas arquiteturais associadas a sistemas distribuídos.

O domínio escolhido é propositalmente simples: usuários, produtos, pedidos, estimativa de entrega, estoque e fraude.

Apesar da simplicidade do domínio, o projeto aplica conceitos importantes:

- API Gateway;
- comunicação REST;
- comunicação assíncrona com RabbitMQ;
- Saga;
- Transactional Outbox;
- idempotência;
- observabilidade;
- autenticação JWT;
- padronização de erros;
- testes HTTP versionados.

## Arquitetura geral

A arquitetura atual possui os seguintes componentes:

```text
Cliente / HTTP Client
  ↓
KrakenD API Gateway
  ↓
Microsserviços internos
  ├── user-api
  ├── product-api
  ├── order-api
  ├── delivery-estimator-api
  └── fraud-detector-api
  ↓
PostgreSQL + RabbitMQ
```

## Serviços

### user-api

Responsável por:

- cadastro e gestão de usuários;
- autenticação;
- geração de access token e refresh token;
- exposição de JWKS para validação do JWT no gateway.

### product-api

Responsável por:

- cadastro de produtos;
- controle de estoque;
- consumo de eventos de reserva de estoque;
- publicação de eventos de estoque reservado ou rejeitado;
- idempotência de reserva.

### delivery-estimator-api

Responsável por:

- cadastro de rotas de entrega;
- estimativa de prazo com base em origem e destino;
- fallback para prazo conservador em caso de indisponibilidade.

### order-api

Responsável por:

- criação de pedidos;
- integração com produto e entrega;
- controle de status do pedido;
- histórico de status;
- publicação de eventos via Outbox;
- consumo de eventos de estoque e fraude.

### fraud-detector-api

Responsável por:

- consumir eventos de pedido criado;
- analisar risco de fraude;
- publicar aprovação ou rejeição.

### api-gateway

Implementado com KrakenD.

Responsável por:

- expor a porta pública do sistema;
- encaminhar chamadas para microsserviços internos;
- validar JWT;
- impedir acesso direto aos microsserviços.

## Fluxo principal de pedido

```text
1. Cliente cria pedido pelo gateway.
2. order-api valida produto via product-api.
3. order-api consulta prazo no delivery-estimator-api.
4. order-api cria pedido como WAITING_STOCK.
5. order-api grava evento StockReservationRequested na Outbox.
6. worker da Outbox publica evento no RabbitMQ.
7. product-api consome evento e reserva estoque.
8. product-api publica stock.reserved ou stock.rejected.
9. order-api atualiza status.
10. Se estoque foi reservado, order-api publica order.created.
11. fraud-detector-api consome order.created.
12. fraud-detector-api publica fraud.approved ou fraud.rejected.
13. order-api atualiza pedido para CONFIRMED ou REJECTED.
```

## Principais decisões arquiteturais

### Monorepo

Todos os microsserviços ficam no mesmo repositório para facilitar o aprendizado, versionamento conjunto e execução local.

### PostgreSQL único com schemas por serviço

Embora em produção cada serviço possa ter seu próprio banco, neste projeto usamos um PostgreSQL único para simplificar o ambiente local.

Cada serviço possui seu schema:

```text
users
products
orders
delivery
fraud
```

### RabbitMQ

Usado para eventos de domínio e integração assíncrona entre serviços.

### KrakenD

Usado como API Gateway para centralizar acesso, autenticação e roteamento.

### OpenTelemetry

Usado para instrumentar os serviços e enviar traces para o Collector.

## Relação com os 12 fatores

O projeto aplica vários princípios dos 12 fatores:

- configuração por variáveis de ambiente;
- serviços independentes;
- logs para saída padrão;
- processos stateless;
- infraestrutura declarada em Docker Compose;
- separação entre build e execução;
- serviços de apoio como recursos anexados.


---

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


---

# 03 — Infraestrutura Local com Docker Compose

## Objetivo

A infraestrutura local permite executar o sistema completo em ambiente de desenvolvimento usando Docker Compose.

Componentes principais:

```text
PostgreSQL
RabbitMQ
KrakenD
OpenTelemetry Collector
Tempo
Prometheus
Grafana
```

## PostgreSQL

O PostgreSQL é usado como banco relacional do projeto.

Embora microsserviços normalmente tenham bancos independentes, neste ambiente local usamos um único PostgreSQL com schemas separados.

### Serviço no Docker Compose

```yaml
postgres:
  image: postgres:16
  container_name: ecommerce-postgres
  environment:
    POSTGRES_DB: ecommerce
    POSTGRES_USER: ecommerce
    POSTGRES_PASSWORD: ecommerce
  ports:
    - "5432:5432"
```

### Por que manter a porta 5432 exposta?

Para desenvolvimento local, é útil acessar o banco com:

- DataGrip;
- DBeaver;
- psql;
- ferramentas de administração.

Em produção, essa porta não deve ficar pública.

## RabbitMQ

O RabbitMQ é usado para comunicação assíncrona entre serviços.

### Portas

```text
5672  -> protocolo AMQP
15672 -> painel web de administração
```

### Painel

```text
http://localhost:15672
```

Usuário e senha:

```text
ecommerce / ecommerce
```

## KrakenD

O KrakenD é o API Gateway do projeto.

É o único ponto de entrada HTTP dos microsserviços.

```text
http://localhost:8099
```

Os microsserviços não possuem mais `ports` publicados no host. Eles usam apenas `expose`, ficando acessíveis somente pela rede interna do Compose.

## Expose vs Ports

### `ports`

Publica uma porta no host:

```yaml
ports:
  - "8096:8080"
```

Permite acesso via:

```text
http://localhost:8096
```

### `expose`

Disponibiliza a porta apenas para outros containers da mesma rede:

```yaml
expose:
  - "8080"
```

Isso impede acesso direto pelo host.

## OpenTelemetry Collector

Recebe traces dos microsserviços e encaminha para o Tempo.

Portas:

```text
4317 -> OTLP gRPC
4318 -> OTLP HTTP
8888 -> métricas do collector
```

## Tempo

Armazena traces distribuídos.

```text
http://localhost:3200
```

## Prometheus

Coleta métricas dos serviços.

```text
http://localhost:9090
```

## Grafana

Visualiza métricas e traces.

```text
http://localhost:3000
```

Credenciais locais:

```text
admin / admin
```

## Rede Docker

Todos os containers usam a rede:

```text
ecommerce-net
```

Dentro dessa rede, os serviços se comunicam pelo nome:

```text
user-api:8080
product-api:8080
order-api:8080
delivery-estimator-api:8080
fraud-detector-api:8080
rabbitmq:5672
postgres:5432
```

## Healthchecks

PostgreSQL e RabbitMQ possuem healthcheck.

Isso evita que serviços dependentes subam antes da infraestrutura estar pronta.

## Comandos úteis

Subir tudo:

```bash
docker compose up -d --build
```

Ver containers:

```bash
docker compose ps
```

Ver logs:

```bash
docker compose logs -f order-api
```

Recriar um serviço:

```bash
docker compose up -d --build order-api
```

Recriar gateway:

```bash
docker compose up -d --force-recreate api-gateway
```

Parar:

```bash
docker compose down
```

Parar sem apagar volumes:

```bash
docker compose down
```

Parar apagando volumes:

```bash
docker compose down -v
```

Use `-v` com cuidado, pois apaga dados do banco e RabbitMQ.


---

# 04 — User API, Autenticação e JWT

## Responsabilidade do user-api

O `user-api` é responsável por:

- autenticar usuários;
- gerar access token;
- gerar refresh token;
- validar refresh token;
- expor chave pública em JWKS;
- manter dados de usuário.

## Pacote principal

O serviço usa o pacote base:

```text
dev.fabiorocha
```

O recurso de autenticação fica em:

```text
dev.fabiorocha.auth
```

## AuthResource

Classe responsável pelos endpoints de autenticação.

### Caminho base

```java
@Path("/auth")
```

### Endpoints

```text
POST /auth/login
POST /auth/refresh-token
```

### `login(LoginRequest request)`

Função responsável por autenticar o usuário.

Fluxo:

```text
1. Recebe username e password.
2. Chama AuthService.autenticar.
3. Se usuário não existir ou senha estiver incorreta, retorna 401.
4. Se autenticação for válida, gera access token e refresh token.
```

Conceitos aplicados:

- autenticação;
- tokenização;
- separação entre resource e service;
- resposta padronizada de erro para credenciais inválidas.

### `refreshToken(RefreshTokenRequest request)`

Função responsável por renovar tokens.

Fluxo:

```text
1. Recebe refresh token.
2. Usa JWTParser para interpretar o token.
3. Verifica se claim tipo = refresh_token.
4. Busca usuário pelo subject.
5. Gera novo par de tokens.
```

### `erro401(String mensagem)`

Função auxiliar que cria uma `WebApplicationException` com status 401.

## AuthService

Classe responsável pela regra de autenticação e geração de tokens.

### Funções principais

#### `autenticar(username, password)`

Valida as credenciais do usuário.

Normalmente envolve:

- buscar usuário no banco;
- comparar senha com hash;
- verificar se usuário está ativo.

#### `gerarTokens(UserEntity user, boolean fresh)`

Gera:

```text
access_token
refresh_token
```

O `access_token` é usado para acessar rotas protegidas.

O `refresh_token` é usado para solicitar novos tokens.

#### `gerarToken(...)`

Função interna que constrói o JWT usando SmallRye JWT.

Campos comuns no token:

```text
iss    -> emissor
sub    -> usuário
upn    -> usuário principal
groups -> papéis/roles
tipo   -> access_token ou refresh_token
fresh  -> indica se veio de login direto
cpf    -> dado do usuário
exp    -> expiração
iat    -> data de emissão
jti    -> identificador único
```

## JwksResource

Classe criada para expor a chave pública em formato JWKS.

### Caminho

```text
GET /auth/.well-known/jwks.json
```

### Por que JWKS?

O KrakenD precisa validar o JWT gerado pelo `user-api`.

Para isso, ele precisa acessar a chave pública usada para verificar a assinatura do token.

JWKS é um formato JSON padronizado para expor chaves públicas.

Exemplo:

```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "kid": "ecommerce-key-1",
      "alg": "RS256",
      "n": "...",
      "e": "AQAB"
    }
  ]
}
```

### Campos importantes

#### `kid`

Identificador da chave.

O JWT gerado precisa ter o mesmo `kid` no header.

#### `alg`

Algoritmo de assinatura.

No projeto:

```text
RS256
```

#### `n` e `e`

Componentes públicos da chave RSA:

- `n`: módulo;
- `e`: expoente público.

## TokenResponse

Record responsável por devolver tokens ao cliente.

Campos:

```java
access_token
refresh_token
token_type
```

Exemplo:

```json
{
  "access_token": "...",
  "refresh_token": "...",
  "token_type": "bearer"
}
```

## LoginRequest

Record de entrada para login.

Campos esperados:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

## RefreshTokenRequest

Record usado para renovação de token.

```json
{
  "refresh_token": "..."
}
```

## JsonWebToken

Interface do MicroProfile JWT usada para acessar claims do token autenticado.

Exemplo de uso em `ProfileResource`:

```java
jwt.getSubject()
```

## JWTParser

Usado para interpretar manualmente um token recebido, como no fluxo de refresh token.

## Chaves pública e privada

O projeto usa:

```text
privateKey.pem
publicKey.pem
```

A chave privada assina tokens.

A chave pública verifica tokens.

## Testes

### JWKS

```bash
curl -i http://localhost:8099/api/auth/.well-known/jwks.json
```

### Login

```bash
curl -i -X POST http://localhost:8099/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## Relação com o Gateway

O `user-api` não valida as chamadas no gateway. Ele emite o token.

O KrakenD usa o JWKS do `user-api` para validar o token antes de encaminhar para os demais serviços.


---

# 05 — Product API

## Responsabilidade

O `product-api` é responsável por:

- cadastrar produtos;
- listar produtos;
- atualizar produtos;
- desativar produtos;
- controlar estoque;
- consumir eventos de reserva de estoque;
- publicar eventos de estoque reservado ou rejeitado;
- garantir idempotência na reserva de estoque.

## Estrutura principal

```text
product-api/
└── src/main/java/br/com/ecommerce/
    ├── domain/
    ├── dto/
    ├── repository/
    ├── service/
    ├── resource/
    └── messaging/
```

## Product

Entidade principal do domínio de produto.

Campos comuns:

```text
id
name
description
sku
price
stockQuantity
originState
status
createdAt
updatedAt
```

### Funções importantes

#### `isActive()`

Indica se o produto está ativo.

Usado na validação de pedido e reserva de estoque.

#### `hasStock(quantity)`

Verifica se há estoque suficiente para uma quantidade solicitada.

Exemplo conceitual:

```java
return stockQuantity >= quantity;
```

## ProductStatus

Enum que representa o status do produto.

Valores típicos:

```text
ACTIVE
INACTIVE
```

## DTOs

### CreateProductRequest

Usado para criar produto.

Campos:

```text
name
description
sku
price
stockQuantity
originState
```

### UpdateProductRequest

Usado para atualizar produto.

Campos comuns:

```text
name
description
price
stockQuantity
originState
status
```

### ProductResponse

DTO de saída.

Evita expor diretamente a entidade JPA.

Normalmente possui método:

```java
fromEntity(Product product)
```

Esse método converte a entidade em resposta da API.

## ProductRepository

Repository usando Panache.

Responsabilidades:

- persistir produto;
- buscar por id;
- buscar por SKU;
- listar todos;
- aplicar filtros futuros.

Exemplo conceitual:

```java
public Optional<Product> findBySku(String sku) {
    return find("sku", sku).firstResultOptional();
}
```

## ProductService

Camada de regra de negócio.

### `create(CreateProductRequest request)`

Cria um novo produto.

Responsabilidades:

```text
1. validar SKU único;
2. criar entidade Product;
3. persistir no banco;
4. retornar ProductResponse.
```

### `update(Long id, UpdateProductRequest request)`

Atualiza produto existente.

### `deactivate(Long id)`

Desativa produto.

Importante: desativar evita que o produto seja usado em novos pedidos.

### `findById(Long id)`

Busca produto por id.

Se não existir, lança exceção.

### `reserveStock(Long orderId, List<StockReservationItem> items)`

Função adicionada para reserva de estoque idempotente.

Responsabilidades:

```text
1. verificar se já existe reserva para orderId + productId;
2. se já existe e está RESERVED, não baixar estoque novamente;
3. se já existe e está REJECTED, retornar rejeição novamente;
4. se não existe, validar estoque;
5. baixar estoque;
6. registrar reserva;
7. retornar resultado.
```

## StockReservation

Entidade criada para controlar idempotência.

Campos:

```text
id
orderId
productId
quantity
status
reason
createdAt
updatedAt
```

## StockReservationStatus

Enum:

```text
RESERVED
REJECTED
```

## StockReservationRepository

Repository responsável por consultar reservas.

Funções importantes:

### `findByOrderIdAndProductId(Long orderId, Long productId)`

Busca reserva única por pedido e produto.

É a chave lógica da idempotência.

### `listByOrderId(Long orderId)`

Lista reservas de um pedido.

## StockReservationResult

Record usado para retornar resultado da reserva.

Campos:

```text
reserved
reason
```

Métodos auxiliares:

```java
reserved(String reason)
rejected(String reason)
```

## ProductResource

Resource REST do serviço.

Endpoints comuns:

```text
POST /products
GET /products
GET /products/{id}
PUT /products/{id}
PATCH /products/{id}/deactivate
```

## Messaging

### StockReservationConsumer

Consumidor do evento:

```text
product.stock.reserve
```

Responsabilidades:

```text
1. receber mensagem do RabbitMQ;
2. converter payload para StockReservationRequestedEvent;
3. chamar ProductService.reserveStock;
4. publicar stock.reserved ou stock.rejected;
5. confirmar ack da mensagem.
```

### StockEventPublisher

Publica eventos de resultado do estoque.

Eventos:

```text
stock.reserved
stock.rejected
```

## Conceito: idempotência

Idempotência significa que executar a mesma operação várias vezes produz o mesmo efeito final.

No projeto:

```text
mesmo orderId + productId
```

não pode baixar estoque duas vezes.

## Banco de dados

Schema:

```text
products
```

Tabela principal:

```text
products.products
```

Tabela de reserva:

```text
products.stock_reservations
```

## Testes HTTP

Arquivo relacionado:

```text
tests/http/02-products.http
tests/http/06-orders-stock-errors.http
```


---

# 06 — Delivery Estimator API

## Responsabilidade

O `delivery-estimator-api` é responsável por estimar o prazo de entrega de um pedido.

Na versão atual, ele trabalha com rotas cadastradas manualmente.

Exemplo:

```text
Origem: SP
Destino: SE
Prazo mínimo: 5 dias
Prazo estimado: 8 dias
Prazo máximo: 13 dias
```

## Endpoints principais

```text
PUT  /delivery-estimates/routes
GET  /delivery-estimates/routes
POST /delivery-estimates/estimate
```

## Conceitos

### Estimativa de entrega

Uma estimativa não é uma promessa exata, mas uma previsão baseada em dados ou regra.

Por isso o serviço retorna três valores:

```text
minDays
estimatedDays
maxDays
```

### Origem e destino

No projeto, os produtos saem de um estado de origem, por exemplo:

```text
SP
```

O cliente informa o estado de destino, por exemplo:

```text
SE
```

O serviço calcula prazo a partir da rota:

```text
SP -> SE
```

## DTOs principais

### DeliveryEstimateRequest

Entrada para estimar entrega.

Campos:

```text
originState
destinationState
```

### DeliveryEstimateResponse

Saída da estimativa.

Campos:

```text
id
originState
destinationState
minDays
estimatedDays
maxDays
source
modelVersion
```

### Route Request

Usado para cadastrar ou atualizar rota.

Campos:

```text
originState
destinationState
minDays
estimatedDays
maxDays
source
modelVersion
```

## Source e modelVersion

### `source`

Indica a origem da estimativa.

Exemplos:

```text
MANUAL_BASELINE
FALLBACK_RESILIENCE
OLIST_BASELINE
TRIBUO_MODEL
```

### `modelVersion`

Indica a versão da regra ou modelo usado.

Exemplo:

```text
baseline-routes-v1
fallback-resilience-v1
```

## Service

O service normalmente possui funções como:

### `upsertRoute(...)`

Cria ou atualiza uma rota.

Responsabilidades:

```text
1. verificar se rota já existe;
2. se existir, atualizar;
3. se não existir, criar;
4. retornar response.
```

### `estimate(...)`

Calcula prazo.

Fluxo:

```text
1. recebe originState e destinationState;
2. busca rota no banco;
3. se encontrar, retorna valores cadastrados;
4. se não encontrar, aplica fallback simples.
```

## Integração com order-api

O `order-api` chama o `delivery-estimator-api` via REST.

Se o serviço estiver indisponível, o `order-api` usa fallback de resiliência:

```text
minDeliveryDays = 7
estimatedDeliveryDays = 10
maxDeliveryDays = 15
deliverySource = FALLBACK_RESILIENCE
deliveryModelVersion = fallback-resilience-v1
```

## Futuro: Olist

O próximo passo de dados é usar a base da Olist para gerar estimativas mais realistas.

Possível fluxo:

```text
dados Olist
  ↓
tratamento
  ↓
cálculo de prazo real
  ↓
baseline estatístico
  ↓
carga no delivery-estimator-api
```

## Futuro: Tribuo

O Tribuo pode ser usado para treinar um modelo de regressão em Java.

Features possíveis:

```text
originState
destinationState
quantidade de itens
valor do pedido
distância aproximada
histórico de atrasos
```

Saída prevista:

```text
estimatedDays
```

## Testes HTTP

Arquivo relacionado:

```text
tests/http/03-delivery-estimates.http
```


---

# 07 — Order API

## Responsabilidade

O `order-api` é o serviço central do fluxo de pedido.

Ele coordena:

- validação de produtos;
- cálculo de valor total;
- estimativa de entrega;
- criação do pedido;
- transições de status;
- publicação de eventos via Outbox;
- consumo de eventos de estoque e fraude.

## Entidades principais

### Order

Representa o pedido.

Campos principais:

```text
id
userId
customerState
status
totalAmount
minDeliveryDays
estimatedDeliveryDays
maxDeliveryDays
deliverySource
deliveryModelVersion
fraudRiskScore
fraudReason
stockReason
items
createdAt
updatedAt
```

### OrderItem

Representa um item do pedido.

Campos:

```text
id
productId
productName
productSku
quantity
unitPrice
totalPrice
originState
```

### OrderStatus

Enum de status do pedido.

Valores atuais:

```text
CREATED
WAITING_STOCK
WAITING_FRAUD
CONFIRMED
CANCELED
REJECTED
```

### OrderStatusHistory

Representa o histórico de mudanças de status.

Campos:

```text
id
orderId
previousStatus
newStatus
triggerEvent
reason
createdAt
```

## DTOs

### CreateOrderRequest

Entrada para criar pedido.

Campos:

```text
userId
customerState
items
```

Validações:

```text
userId obrigatório
customerState obrigatório
customerState com 2 letras
items não pode ser vazio
```

### CreateOrderItemRequest

Campos:

```text
productId
quantity
```

Validações:

```text
productId obrigatório
quantity maior que zero
```

### OrderResponse

Resposta da API de pedidos.

Possui método:

```java
fromEntity(Order order)
```

Responsável por converter entidade em DTO.

### OrderItemResponse

Converte `OrderItem` em resposta.

### OrderStatusHistoryResponse

Converte histórico de status em resposta.

## OrderResource

Endpoints principais:

```text
POST /orders
GET /orders
GET /orders/{id}
GET /orders/{id}/history
PATCH /orders/{id}/cancel
```

### `create(@Valid CreateOrderRequest request)`

Cria pedido.

O `@Valid` aciona Bean Validation.

### `listAll(@QueryParam("userId") Long userId)`

Lista todos os pedidos ou filtra por usuário.

### `findById(Long id)`

Consulta pedido por id.

### `listStatusHistory(Long id)`

Retorna histórico de status.

### `cancel(Long id)`

Cancela pedido, quando permitido.

## OrderService

Camada de regra de negócio.

### `create(CreateOrderRequest request)`

Função mais importante do serviço.

Fluxo conceitual:

```text
1. recebe request;
2. para cada item, busca produto;
3. valida produto;
4. cria OrderItem;
5. soma total;
6. consulta estimativa de entrega;
7. cria Order;
8. define status WAITING_STOCK;
9. registra histórico;
10. salva evento StockReservationRequested na Outbox.
```

### `findProductOrThrow(Long productId)`

Busca produto via `ProductCatalogGateway`.

Responsabilidades:

```text
1. chamar product-api;
2. se produto não existir, lançar BadRequestException;
3. se product-api estiver indisponível, lançar ServiceUnavailableException.
```

### `validateProductForOrder(...)`

Valida se produto pode entrar no pedido.

Critérios:

```text
produto ativo
quantidade válida
preço válido
estoque inicial coerente
```

### `createOrderItem(...)`

Cria item do pedido a partir do produto retornado pelo `product-api`.

### `changeStatus(...)`

Altera status do pedido e registra histórico.

### `cancel(Long id)`

Cancela pedido.

Em uma evolução futura, se o estoque já estiver reservado, deve publicar evento de compensação.

## ProductCatalogGateway

Classe criada para resiliência na chamada ao `product-api`.

Usa:

```text
@Timeout
@Retry
@CircuitBreaker
@Fallback
```

Decisão:

```text
produto inexistente -> erro de negócio
product-api indisponível -> 503
```

## DeliveryEstimatorGateway

Classe criada para resiliência na chamada ao `delivery-estimator-api`.

Decisão:

```text
delivery-estimator-api indisponível -> usar fallback conservador
```

Fallback:

```text
minDays = 7
estimatedDays = 10
maxDays = 15
source = FALLBACK_RESILIENCE
modelVersion = fallback-resilience-v1
```

## Integração assíncrona

O `order-api` publica eventos via Outbox e consome eventos de:

```text
stock.reserved
stock.rejected
fraud.approved
fraud.rejected
```

## Testes HTTP

Arquivos relacionados:

```text
tests/http/04-orders-success.http
tests/http/05-orders-validation-errors.http
tests/http/06-orders-stock-errors.http
tests/http/07-orders-fraud-flow.http
```


---

# 08 — RabbitMQ e Eventos

## Objetivo

O RabbitMQ é usado para comunicação assíncrona entre microsserviços.

Enquanto REST é usado para consultas imediatas, eventos são usados para processos distribuídos e desacoplados.

## Exchange principal

```text
ecommerce.events
```

Tipo:

```text
topic
```

## Por que usar exchange topic?

Com exchange do tipo `topic`, os eventos são roteados por routing keys.

Exemplo:

```text
product.stock.reserve
stock.reserved
stock.rejected
order.created
order.canceled
fraud.approved
fraud.rejected
```

## Filas principais

Exemplos de filas usadas:

```text
product.stock-reservation
order.stock-reserved
order.stock-rejected
fraud.order-created
order.fraud-approved
order.fraud-rejected
orders.created
orders.canceled
```

## Eventos principais

### StockReservationRequested

Publicado pelo `order-api`.

Routing key:

```text
product.stock.reserve
```

Consumido por:

```text
product-api
```

Objetivo:

```text
Solicitar reserva de estoque para um pedido.
```

### StockReserved

Publicado pelo `product-api`.

Routing key:

```text
stock.reserved
```

Consumido por:

```text
order-api
```

Objetivo:

```text
Informar que o estoque foi reservado com sucesso.
```

### StockRejected

Publicado pelo `product-api`.

Routing key:

```text
stock.rejected
```

Consumido por:

```text
order-api
```

Objetivo:

```text
Informar que não foi possível reservar estoque.
```

### OrderCreated

Publicado pelo `order-api`.

Routing key:

```text
order.created
```

Consumido por:

```text
fraud-detector-api
```

Objetivo:

```text
Solicitar análise de fraude.
```

### FraudApproved

Publicado pelo `fraud-detector-api`.

Routing key:

```text
fraud.approved
```

Consumido por:

```text
order-api
```

Objetivo:

```text
Informar que pedido foi aprovado na análise de fraude.
```

### FraudRejected

Publicado pelo `fraud-detector-api`.

Routing key:

```text
fraud.rejected
```

Consumido por:

```text
order-api
```

Objetivo:

```text
Informar que pedido foi rejeitado na análise de fraude.
```

## Estrutura geral de evento

Um evento geralmente possui:

```text
eventId
eventType
sourceService
occurredAt
payload
```

## Por que eventId é importante?

O `eventId` permite:

- rastreabilidade;
- idempotência;
- correlação;
- auditoria;
- evitar processamento duplicado.

## Configuração no Quarkus

O projeto usa SmallRye Reactive Messaging com RabbitMQ.

Exemplo conceitual de canal outgoing:

```properties
mp.messaging.outgoing.order-created-out.connector=smallrye-rabbitmq
mp.messaging.outgoing.order-created-out.exchange.name=ecommerce.events
mp.messaging.outgoing.order-created-out.exchange.type=topic
mp.messaging.outgoing.order-created-out.default-routing-key=order.created
```

Exemplo conceitual de canal incoming:

```properties
mp.messaging.incoming.fraud-approved-in.connector=smallrye-rabbitmq
mp.messaging.incoming.fraud-approved-in.queue.name=order.fraud-approved
```

## Consumers

Um consumer recebe mensagens de um canal.

Exemplo conceitual:

```java
@Incoming("stock-reserved-in")
public CompletionStage<Void> consume(Message<JsonObject> message) {
    // processar evento
    return message.ack();
}
```

## Publishers

Um publisher envia eventos para um canal.

Exemplo conceitual:

```java
@Channel("fraud-approved-out")
Emitter<JsonObject> emitter;
```

## Painel RabbitMQ

Acesse:

```text
http://localhost:15672
```

Credenciais:

```text
ecommerce / ecommerce
```

## Comandos úteis

Listar filas:

```bash
docker exec -it ecommerce-rabbitmq rabbitmqctl list_queues name messages
```

Listar bindings:

```bash
docker exec -it ecommerce-rabbitmq rabbitmqctl list_bindings source_name destination_name routing_key
```

## Cuidados

Mensageria assíncrona exige atenção a:

- mensagens duplicadas;
- falhas temporárias;
- retries;
- dead letter;
- idempotência;
- observabilidade;
- versionamento de contratos.


---

# 09 — Saga do Pedido

## O que é Saga?

Saga é um padrão para coordenar transações distribuídas em sistemas de microsserviços.

Em vez de usar uma transação única envolvendo vários serviços, cada serviço executa sua transação local e publica eventos para continuar o fluxo.

## Por que usar Saga?

Em microsserviços, cada serviço deve ser autônomo.

Não é recomendado depender de uma transação distribuída global entre:

```text
order-api
product-api
fraud-detector-api
```

A Saga permite coordenar o processo por eventos.

## Saga neste projeto

Fluxo simplificado:

```text
CREATED
  ↓
WAITING_STOCK
  ↓
WAITING_FRAUD
  ↓
CONFIRMED
```

Fluxo de rejeição por estoque:

```text
CREATED
  ↓
WAITING_STOCK
  ↓
REJECTED
```

Fluxo de rejeição por fraude:

```text
CREATED
  ↓
WAITING_STOCK
  ↓
WAITING_FRAUD
  ↓
REJECTED
```

## Estados do pedido

### CREATED

Estado inicial conceitual.

### WAITING_STOCK

Pedido criado e aguardando reserva de estoque.

### WAITING_FRAUD

Estoque reservado e aguardando análise de fraude.

### CONFIRMED

Pedido confirmado.

### CANCELED

Pedido cancelado.

### REJECTED

Pedido rejeitado por estoque, fraude ou outra regra.

## OrderStatusHistory

Cada mudança de status é registrada.

Campos:

```text
orderId
previousStatus
newStatus
triggerEvent
reason
createdAt
```

## Triggers de mudança

Exemplos:

```text
ORDER_CREATED
STOCK_RESERVED
STOCK_REJECTED
FRAUD_APPROVED
FRAUD_REJECTED
ORDER_CANCELED
```

## Por que registrar histórico?

O histórico permite:

- auditoria;
- rastreabilidade;
- debug;
- explicação do fluxo para o cliente;
- monitoramento de pedidos presos.

## Endpoint de histórico

```text
GET /orders/{id}/history
```

Via gateway:

```text
GET /api/orders/{id}/history
```

## Função `changeStatus`

A função de mudança de status deve:

```text
1. validar transição;
2. alterar status;
3. registrar histórico;
4. atualizar data de alteração;
5. salvar motivo.
```

## Transições válidas

Exemplos:

```text
CREATED -> WAITING_STOCK
WAITING_STOCK -> WAITING_FRAUD
WAITING_STOCK -> REJECTED
WAITING_FRAUD -> CONFIRMED
WAITING_FRAUD -> REJECTED
CONFIRMED -> CANCELED
```

## Saga orquestrada ou coreografada?

A implementação atual é uma combinação:

- o `order-api` mantém o estado e decide transições;
- os outros serviços respondem por eventos.

Isso se aproxima de uma Saga orquestrada pelo `order-api`, mas com comunicação assíncrona.

## O que ainda falta?

A Saga atual ainda não possui:

- compensação completa;
- timeout por etapa;
- dead letter;
- tabela explícita de instância de Saga;
- painel de monitoramento;
- retry controlado por etapa.

## Compensação futura

Exemplo de compensação:

```text
Pedido rejeitado por fraude
  ↓
order-api publica stock.release.requested
  ↓
product-api devolve estoque
  ↓
product-api publica stock.released
```

## Testes HTTP

Arquivos relacionados:

```text
tests/http/04-orders-success.http
tests/http/06-orders-stock-errors.http
tests/http/07-orders-fraud-flow.http
```


---

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


---

# 11 — Transactional Outbox

## Problema

Em sistemas distribuídos, é comum precisar:

```text
1. salvar uma alteração no banco;
2. publicar um evento no broker.
```

Exemplo:

```text
salvar pedido
publicar product.stock.reserve
```

O problema ocorre quando uma operação funciona e a outra falha.

Exemplo:

```text
pedido salvo no banco
RabbitMQ indisponível
evento não publicado
```

Isso deixa o sistema inconsistente.

## Solução: Transactional Outbox

O padrão Transactional Outbox resolve esse problema salvando o evento no banco na mesma transação da alteração principal.

Fluxo:

```text
1. salvar pedido;
2. salvar evento na tabela outbox_events;
3. commit;
4. worker lê outbox;
5. worker publica evento no RabbitMQ;
6. worker marca evento como PUBLISHED.
```

## Implementação no order-api

Tabela:

```text
orders.outbox_events
```

Campos principais:

```text
id
eventId
aggregateType
aggregateId
eventType
routingKey
payload
status
attempts
lastError
createdAt
updatedAt
publishedAt
```

## OutboxStatus

Enum:

```text
PENDING
PUBLISHED
FAILED
```

## OutboxEvent

Entidade JPA que representa evento pendente ou publicado.

### Campos importantes

#### `eventId`

Identificador único do evento.

#### `aggregateType`

Tipo do agregado.

Exemplo:

```text
Order
```

#### `aggregateId`

Id do pedido.

#### `eventType`

Tipo do evento.

Exemplo:

```text
StockReservationRequested
OrderCreated
OrderCanceled
```

#### `routingKey`

Routing key do RabbitMQ.

Exemplo:

```text
product.stock.reserve
order.created
order.canceled
```

#### `payload`

JSON do evento.

#### `status`

Status da publicação.

## OutboxEventRepository

Repository Panache.

Função principal:

### `listPending(int limit)`

Lista eventos pendentes em ordem de criação.

## OutboxService

Camada de negócio da Outbox.

### `saveEvent(...)`

Salva um evento na tabela Outbox.

Usado dentro da transação do pedido.

### `listPending(int limit)`

Retorna eventos pendentes.

### `markPublished(Long id)`

Marca evento como publicado.

### `markFailed(Long id, Throwable throwable)`

Incrementa tentativas e registra erro.

Se atingir o máximo de tentativas, marca como `FAILED`.

## OrderEventPublisher

Antes, publicava diretamente no RabbitMQ.

Agora, salva eventos na Outbox.

Funções:

### `publishOrderCreated(Order order)`

Salva evento `OrderCreated`.

### `publishOrderCanceled(Order order)`

Salva evento `OrderCanceled`.

### `publishStockReservationRequested(Order order)`

Salva evento `StockReservationRequested`.

## OutboxPublisherWorker

Worker agendado que publica eventos pendentes.

Usa:

```java
@Scheduled(every = "2s")
```

Fluxo:

```text
1. buscar eventos PENDING;
2. identificar emitter pela routingKey;
3. publicar no RabbitMQ;
4. marcar como PUBLISHED;
5. em caso de erro, marcar falha.
```

## Vantagens

- evita perder eventos;
- melhora confiabilidade;
- permite retry;
- permite auditoria;
- desacopla transação do broker.

## Limitações atuais

Ainda falta:

- lock para múltiplas instâncias;
- status PROCESSING;
- dead letter;
- painel de Outbox;
- idempotência ponta a ponta.

## Teste de falha

1. Parar RabbitMQ.
2. Criar pedido.
3. Ver evento em `orders.outbox_events`.
4. Subir RabbitMQ.
5. Worker publica evento.

## Consulta SQL

```sql
SELECT id, event_type, routing_key, status, attempts, last_error
FROM orders.outbox_events
ORDER BY id DESC
LIMIT 10;
```


---

# 12 — Idempotência de Reserva de Estoque

## Problema

Em mensageria, a mesma mensagem pode ser entregue mais de uma vez.

Se o `product-api` processar duas vezes o mesmo evento de reserva, o estoque pode ser baixado duas vezes.

Exemplo:

```text
Estoque inicial: 10
Pedido reserva: 2

Primeiro processamento:
estoque = 8

Mensagem duplicada:
estoque = 6
```

Isso está errado.

## Solução

Criar uma tabela de reservas e usar uma chave lógica:

```text
orderId + productId
```

Se já existe reserva para aquele pedido e produto, o serviço não baixa o estoque novamente.

## Tabela

```text
products.stock_reservations
```

Campos:

```text
id
order_id
product_id
quantity
status
reason
created_at
updated_at
```

Índice único:

```text
order_id + product_id
```

## StockReservation

Entidade JPA da reserva.

## StockReservationStatus

Enum:

```text
RESERVED
REJECTED
```

## StockReservationRepository

Função principal:

```java
findByOrderIdAndProductId(Long orderId, Long productId)
```

Essa função garante a checagem idempotente.

## ProductService.reserveStock

Função principal da idempotência.

Fluxo:

```text
1. validar orderId e items;
2. verificar reservas existentes;
3. se todas já existem como RESERVED, retornar sucesso idempotente;
4. se alguma já existe como REJECTED, retornar rejeição;
5. validar estoque;
6. se estoque insuficiente, registrar REJECTED;
7. se estoque suficiente, baixar estoque e registrar RESERVED.
```

## StockReservationResult

Record que representa o resultado:

```text
reserved: true/false
reason: motivo
```

Métodos auxiliares:

```java
reserved(String reason)
rejected(String reason)
```

## Comportamento esperado

### Primeira mensagem

```text
Não existe reserva
  ↓
baixa estoque
  ↓
cria stock_reservations
  ↓
publica stock.reserved
```

### Mensagem duplicada

```text
Reserva já existe
  ↓
não baixa estoque
  ↓
retorna sucesso idempotente
  ↓
publica stock.reserved novamente, se necessário
```

### Estoque insuficiente

```text
não baixa estoque
  ↓
cria reserva REJECTED
  ↓
publica stock.rejected
```

### Reprocessamento de rejeição

```text
reserva REJECTED já existe
  ↓
não revalida
  ↓
retorna rejeição idempotente
```

## Por que publicar novamente stock.reserved?

Em alguns casos, o consumidor pode não ter recebido o evento de resposta.

Publicar novamente uma resposta idempotente ajuda o fluxo a continuar.

## Limitações atuais

Ainda falta:

- compensação de estoque;
- controle por eventId;
- métrica de duplicidade;
- locks mais fortes para concorrência extrema.

## Teste

Arquivo:

```text
tests/http/06-orders-stock-errors.http
```

Também é possível publicar manualmente uma mensagem duplicada pelo RabbitMQ Management API.


---

# 13 — API Gateway com KrakenD

## Objetivo

O KrakenD atua como ponto único de entrada do sistema.

Antes do gateway, cada microsserviço era acessado diretamente por portas diferentes:

```text
8094 user-api
8095 product-api
8096 order-api
8097 delivery-estimator-api
8098 fraud-detector-api
```

Depois da feature de gateway, o acesso externo passou a ser:

```text
http://localhost:8099
```

## Responsabilidades do Gateway

- expor rotas públicas;
- encaminhar chamadas para serviços internos;
- validar JWT;
- ocultar portas internas;
- centralizar políticas;
- facilitar futura aplicação de rate limit, logs e autenticação.

## Arquivo de configuração

```text
gateway/krakend.json
```

## Estrutura básica

```json
{
  "version": 3,
  "name": "ecommerce-api-gateway",
  "port": 8080,
  "endpoints": []
}
```

## Endpoints

Cada endpoint define:

```text
endpoint público
method
headers aceitos
backend interno
url_pattern
encoding
extra_config
```

## Exemplo: produtos

```json
{
  "endpoint": "/api/products",
  "method": "GET",
  "backend": [
    {
      "host": ["http://product-api:8080"],
      "url_pattern": "/products",
      "method": "GET"
    }
  ]
}
```

## No-op encoding

O projeto usa:

```json
"output_encoding": "no-op"
"encoding": "no-op"
```

Isso faz o gateway se comportar mais como proxy, preservando respostas do backend.

## Rotas principais

```text
/api/auth/login
/api/auth/refresh-token
/api/auth/.well-known/jwks.json

/api/products
/api/products/{id}

/api/orders
/api/orders/{id}
/api/orders/{id}/history
/api/orders/{id}/cancel

/api/delivery-estimates/estimate
/api/delivery-estimates/routes

/api/fraud-analyses
/api/fraud-analyses/order/{orderId}
```

## Acesso apenas pelo Gateway

Os microsserviços usam:

```yaml
expose:
  - "8080"
```

e não:

```yaml
ports:
  - "8096:8080"
```

Assim, o host não acessa diretamente os microsserviços.

## Teste

Acesso via gateway:

```bash
curl http://localhost:8099/api/products
```

Acesso direto esperado falhar:

```bash
curl http://localhost:8096/q/health
```

## Por que isso é importante?

Evita que clientes contornem:

- autenticação;
- políticas do gateway;
- logging centralizado;
- validação de token;
- futuras regras de rate limit.

## Limitações atuais

Ainda falta:

- proteger todas as rotas;
- rate limiting;
- headers de correlação;
- logs de acesso;
- políticas por ambiente;
- separar rotas públicas e administrativas.


---

# 14 — JWT no Gateway e JWKS

## Objetivo

Depois que o KrakenD virou ponto único de entrada, o próximo passo foi proteger rotas com JWT.

## Fluxo

```text
Cliente
  ↓
POST /api/auth/login
  ↓
user-api gera JWT
  ↓
Cliente chama API com Authorization: Bearer token
  ↓
KrakenD valida token usando JWKS
  ↓
KrakenD encaminha chamada ao microsserviço
```

## Rotas públicas

```text
POST /api/auth/login
POST /api/auth/refresh-token
GET  /api/auth/.well-known/jwks.json
```

## Rotas protegidas

Exemplo inicial:

```text
GET /api/orders
```

Depois o mesmo padrão pode ser aplicado às demais rotas.

## Access Token

Token usado para acessar APIs.

Tempo de vida menor.

## Refresh Token

Token usado para renovar access token.

Tempo de vida maior.

## Claims usadas

Exemplo de claims:

```text
iss
sub
upn
groups
tipo
fresh
cpf
exp
iat
jti
```

## Header do JWT

O token precisa ter:

```json
{
  "kid": "ecommerce-key-1",
  "typ": "JWT",
  "alg": "RS256"
}
```

## JWKS

Endpoint:

```text
GET /auth/.well-known/jwks.json
```

Via gateway:

```text
GET /api/auth/.well-known/jwks.json
```

Retorna:

```json
{
  "keys": [
    {
      "kty": "RSA",
      "kid": "ecommerce-key-1",
      "use": "sig",
      "alg": "RS256",
      "n": "...",
      "e": "AQAB"
    }
  ]
}
```

## KrakenD auth/validator

Exemplo:

```json
"extra_config": {
  "auth/validator": {
    "alg": "RS256",
    "jwk_url": "http://user-api:8080/auth/.well-known/jwks.json",
    "issuer": "user-api",
    "cache": true,
    "cache_duration": 900,
    "disable_jwk_security": true,
    "propagate_claims": [
      ["sub", "X-User-Id"],
      ["groups", "X-User-Roles"]
    ]
  }
}
```

## `disable_jwk_security`

Usado em desenvolvimento local porque o JWKS está em HTTP.

Em produção, o ideal é:

```text
HTTPS
disable_jwk_security = false
```

## Propagação de claims

O gateway pode repassar claims para os serviços internos.

Exemplo:

```text
sub -> X-User-Id
groups -> X-User-Roles
```

Isso permite que os microsserviços saibam quem fez a chamada, sem revalidar o token.

## Testes

### JWKS

```bash
curl -i http://localhost:8099/api/auth/.well-known/jwks.json
```

### Login

```bash
curl -i -X POST http://localhost:8099/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### Rota protegida sem token

```bash
curl -i http://localhost:8099/api/orders
```

Esperado:

```text
401 Unauthorized
```

### Rota protegida com token

```bash
TOKEN="..."

curl -i http://localhost:8099/api/orders \
  -H "Authorization: Bearer $TOKEN"
```

Esperado:

```text
200 OK
```

## Observação de segurança

Tokens colados em logs, chats ou documentação devem ser considerados expostos.

Em ambiente real, é necessário rotacionar chaves ou invalidar tokens.


---

# 15 — Observabilidade

## Objetivo

Observabilidade permite entender o comportamento do sistema em execução.

O projeto usa:

```text
OpenTelemetry Collector
Tempo
Prometheus
Grafana
```

## Pilares

### Traces

Mostram o caminho de uma requisição entre serviços.

Exemplo:

```text
KrakenD -> order-api -> product-api -> delivery-estimator-api
```

### Métricas

Valores numéricos coletados ao longo do tempo.

Exemplos:

```text
requisições por segundo
latência
erros HTTP
uso de memória
```

### Logs

Mensagens emitidas pelos serviços.

No projeto atual, logs ainda não estão centralizados com Loki.

## OpenTelemetry

OpenTelemetry é usado para instrumentação.

Cada serviço recebe variáveis:

```yaml
OTEL_ENABLED: "true"
OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4317
OTEL_SERVICE_NAME: order-api
```

## OpenTelemetry Collector

Recebe traces via OTLP.

Configuração:

```text
infra/observability/otel/otel-collector-config.yml
```

O Collector encaminha traces para o Tempo.

## Tempo

Backend de traces.

Configuração:

```text
infra/observability/tempo/tempo.yml
```

Porta:

```text
3200
```

## Prometheus

Coleta métricas.

Configuração:

```text
infra/observability/prometheus/prometheus.yml
```

Porta:

```text
9090
```

## Grafana

Interface visual.

Porta:

```text
3000
```

Login:

```text
admin / admin
```

## Datasources

Configurados em:

```text
infra/observability/grafana/provisioning/datasources/datasources.yml
```

Datasources:

```text
Prometheus
Tempo
```

## Métricas Quarkus

Os serviços usam Micrometer/Prometheus.

Endpoint interno:

```text
/q/metrics
```

Como os microsserviços não são expostos no host, o Prometheus acessa pela rede Docker.

## Traces

Para gerar traces:

```bash
curl http://localhost:8099/api/products
curl http://localhost:8099/api/orders
```

Depois acessar Grafana:

```text
Explore -> Tempo
```

## Problemas enfrentados

### Tempo com erro de permissão

Erro:

```text
mkdir /tmp/tempo/blocks: permission denied
```

Solução no Compose:

```yaml
user: "0:0"
```

### Collector com logging exporter depreciado

Erro:

```text
logging exporter has been deprecated
```

Solução:

```yaml
exporters:
  debug:
    verbosity: basic
```

## Próximos passos

- dashboards customizados;
- métricas de negócio;
- logs centralizados com Loki;
- correlationId;
- alertas;
- métricas de Outbox;
- métricas de Saga;
- métricas de fallback.


---

# 16 — Tratamento Padronizado de Erros

## Objetivo

Padronizar respostas de erro evita inconsistências entre endpoints e facilita testes.

Formato adotado:

```json
{
  "timestamp": "2026-05-25T21:37:03",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Dados inválidos na requisição",
  "path": "/orders",
  "details": []
}
```

## ApiErrorResponse

Record que representa a resposta de erro.

Campos:

```text
timestamp
status
error
message
path
details
```

## ErrorResponseFactory

Classe utilitária para construir respostas.

Funções:

### `build(status, error, message, uriInfo)`

Cria resposta sem detalhes.

### `build(status, error, message, uriInfo, details)`

Cria resposta com detalhes.

### `getPath(uriInfo)`

Extrai caminho da requisição.

## BadRequestExceptionMapper

Mapeia:

```text
BadRequestException -> 400 BAD_REQUEST
```

Usado em casos como produto inexistente no pedido.

## NotFoundExceptionMapper

Mapeia:

```text
NotFoundException -> 404 NOT_FOUND
```

## ServiceUnavailableExceptionMapper

Mapeia:

```text
ServiceUnavailableException -> 503 SERVICE_UNAVAILABLE
```

Usado quando um serviço dependente está indisponível.

## ConstraintViolationExceptionMapper

Mapeia erros de validação Bean Validation.

Exemplo:

```text
userId obrigatório
items vazio
quantity menor que 1
```

Retorna:

```text
400 VALIDATION_ERROR
```

## ResteasyReactiveViolationExceptionMapper

Mapper específico necessário para validações no Quarkus REST/RESTEasy Reactive.

Sem ele, algumas validações poderiam cair em erro genérico.

## PersistenceExceptionMapper

Mapeia erros de persistência.

Exemplo:

- unique constraint;
- violação de chave;
- falha de banco.

Retorna:

```text
409 PERSISTENCE_ERROR
```

## WebApplicationExceptionMapper

Mapper para exceções HTTP genéricas.

Evita que exceções REST não específicas caiam no erro genérico.

## GenericExceptionMapper

Última barreira para erros não tratados.

Mapeia:

```text
Throwable -> 500 INTERNAL_SERVER_ERROR
```

Importante: deve logar o erro, mas não expor stack trace ao cliente.

## Validações no CreateOrderRequest

Exemplo:

```text
userId obrigatório
customerState obrigatório
customerState com 2 letras
items não vazio
```

## Validações no CreateOrderItemRequest

Exemplo:

```text
productId obrigatório
quantity maior que zero
```

## Exemplo de erro de validação

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Dados inválidos na requisição",
  "details": [
    "create.request.userId: O ID do usuário é obrigatório"
  ]
}
```

## Exemplo de produto inexistente

```json
{
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Produto não encontrado: 999999"
}
```

## Testes HTTP

Arquivo:

```text
tests/http/05-orders-validation-errors.http
```

## Próximos passos

- replicar padrão para todos os serviços;
- criar biblioteca comum;
- adicionar códigos internos de erro;
- criar testes automatizados para mappers.


---

# 17 — Suíte de Testes HTTP/JSON

## Objetivo

A suíte HTTP/JSON permite validar o sistema manualmente, mas de forma versionada e repetível.

Arquivos:

```text
tests/http/00-auth.http
tests/http/01-health.http
tests/http/02-products.http
tests/http/03-delivery-estimates.http
tests/http/04-orders-success.http
tests/http/05-orders-validation-errors.http
tests/http/06-orders-stock-errors.http
tests/http/07-orders-fraud-flow.http
tests/http/08-gateway-security.http
```

## Por que usar `.http`?

Vantagens:

- versionado no Git;
- fácil de executar no IntelliJ;
- ótimo para aulas;
- documenta exemplos reais;
- substitui comandos curl longos;
- facilita regressão manual antes de PR.

## 00-auth.http

Responsável por:

- testar JWKS;
- fazer login;
- salvar access_token;
- salvar refresh_token;
- testar rota protegida sem token;
- testar token inválido;
- testar refresh token.

Variáveis salvas automaticamente:

```text
access_token
refresh_token
```

## 01-health.http

Valida disponibilidade via gateway.

Testes:

```text
JWKS
GET /api/products
GET /api/orders
```

## 02-products.http

Testa:

```text
criar produto
listar produtos
buscar produto por id
atualizar produto
desativar produto
```

Variáveis salvas:

```text
product_id
product_sku
product_to_deactivate_id
```

## 03-delivery-estimates.http

Testa:

```text
criar rota SP -> SE
criar rota SP -> RJ
listar rotas
estimar entrega
```

## 04-orders-success.http

Testa fluxo feliz:

```text
criar rota
criar produto
criar pedido
consultar pedido
consultar histórico
```

Como o fluxo é assíncrono, é necessário aguardar alguns segundos antes de consultar status final.

## 05-orders-validation-errors.http

Testa erros:

```text
pedido inválido
produto inexistente
quantidade inválida
```

Valida padrão:

```text
VALIDATION_ERROR
BAD_REQUEST
```

## 06-orders-stock-errors.http

Testa rejeição por estoque.

Fluxo:

```text
criar produto com estoque 1
criar pedido com quantidade 10
consultar pedido
esperado: REJECTED
```

## 07-orders-fraud-flow.http

Testa fluxo de fraude.

Cria produto de alto valor e pedido com valor alto.

Resultado esperado depende da regra atual:

```text
CONFIRMED ou REJECTED
```

## 08-gateway-security.http

Testa segurança:

```text
JWKS público
login público
rota protegida sem token
rota protegida com token inválido
rota protegida com token válido
acesso direto aos microsserviços deve falhar
```

## Ordem recomendada

```text
00-auth
01-health
02-products
03-delivery-estimates
04-orders-success
05-orders-validation-errors
06-orders-stock-errors
07-orders-fraud-flow
08-gateway-security
```

## Limitações

São testes manuais, não automatizados em pipeline.

Próxima evolução:

```text
feature/automated-tests
```

Com:

- JUnit;
- REST Assured;
- Quarkus Test;
- Testcontainers;
- Dev Services.


---

# 18 — Roadmap e Débitos Técnicos

## Objetivo

Este capítulo resume o que já foi concluído e o que ainda falta para evoluir o projeto.

## Concluído na V1

```text
Monorepo
Git Flow por feature
user-api com JWT
product-api
delivery-estimator-api
order-api
fraud-detector-api
RabbitMQ
Saga básica
Transactional Outbox no order-api
Idempotência de estoque
Docker Compose completo
KrakenD API Gateway
Acesso apenas pelo Gateway
JWT validado no KrakenD
JWKS no user-api
OpenTelemetry + Tempo + Prometheus + Grafana
Tratamento padronizado de erros no order-api
Suíte HTTP/JSON
```

## Próximas features recomendadas

### 1. automated-tests

Criar testes automatizados.

```text
feature/automated-tests
```

Escopo:

- Quarkus Test;
- JUnit;
- REST Assured;
- testes de resources;
- testes de services;
- testes de validação;
- testes de autenticação.

### 2. database-migrations-all-services

Migrar todos os serviços para Flyway/Liquibase.

```text
feature/database-migrations-all-services
```

### 3. fraud-outbox-pattern

Implementar Outbox no `fraud-detector-api`.

```text
feature/fraud-outbox-pattern
```

### 4. event-idempotency

Criar idempotência geral por `eventId`.

```text
feature/event-idempotency
```

### 5. stock-compensation

Devolver estoque em cancelamento ou fraude rejeitada.

```text
feature/stock-compensation
```

### 6. order-saga-compensation

Adicionar compensações e timeouts na Saga.

```text
feature/order-saga-compensation
```

### 7. business-metrics

Adicionar métricas de negócio.

```text
feature/business-metrics
```

Exemplos:

```text
pedidos criados
pedidos confirmados
pedidos rejeitados
fallbacks de entrega
outbox pendente
```

### 8. observability-dashboards

Criar dashboards Grafana.

```text
feature/observability-dashboards
```

### 9. logs-centralized-loki

Adicionar Loki.

```text
feature/logs-centralized-loki
```

### 10. delivery-estimator-tribuo

Refatorar estimativa de entrega para usar ML com Tribuo.

```text
feature/delivery-estimator-tribuo
```

### 11. ai-chat-bff-langchain4j

Criar BFF conversacional com LangChain4j.

```text
feature/ai-chat-bff-langchain4j
```

## Roadmap por tema

### Confiabilidade

```text
fraud-outbox-pattern
outbox-other-services
event-idempotency
stock-compensation
order-saga-compensation
```

### Qualidade

```text
automated-tests
api-event-contracts
architecture-docs
architecture-decision-records
```

### Produção

```text
database-migrations-all-services
service-to-service-security
kubernetes-deployment
native-dockerfiles
krakend-hardening
```

### IA e Dados

```text
olist-delivery-baseline
delivery-data-pipeline
delivery-estimator-tribuo
ai-chat-bff-langchain4j
```

### Observabilidade

```text
business-metrics
observability-dashboards
logs-centralized-loki
```

## Considerações finais

A V1 do projeto já demonstra um fluxo completo e rico de microsserviços.

O principal ganho didático é que cada etapa foi construída incrementalmente:

```text
serviço simples
  ↓
integração REST
  ↓
eventos
  ↓
Saga
  ↓
Outbox
  ↓
idempotência
  ↓
Gateway
  ↓
JWT
  ↓
Observabilidade
  ↓
testes HTTP
```

A próxima etapa natural é transformar validações manuais em testes automatizados.


---

# Apostila V1 — Microsserviços na Prática com Quarkus, RabbitMQ, PostgreSQL e KrakenD

Esta apostila documenta a primeira grande etapa do projeto de ecommerce baseado em microsserviços.

O objetivo é registrar, de forma didática, tudo que foi construído até aqui:

- Monorepo com múltiplos microsserviços;
- Java 21 com Quarkus;
- PostgreSQL com schema por serviço;
- RabbitMQ para comunicação assíncrona;
- Saga básica para pedido;
- Transactional Outbox no `order-api`;
- Idempotência na reserva de estoque;
- API Gateway com KrakenD;
- Autenticação JWT validada no gateway;
- JWKS exposto pelo `user-api`;
- Observabilidade com OpenTelemetry, Tempo, Prometheus e Grafana;
- Tratamento padronizado de erros;
- Suíte de testes HTTP/JSON.

## Público-alvo

Esta apostila foi pensada para estudantes, desenvolvedores e arquitetos que desejam aprender microsserviços por meio de um projeto prático e incremental.

## Pré-requisitos

Conhecimentos recomendados:

- Java básico/intermediário;
- REST APIs;
- Docker e Docker Compose;
- Git e GitHub;
- noções de banco de dados relacional;
- noções de mensageria.

## Organização dos capítulos

| Capítulo | Arquivo |
|---|---|
| 01 | `01-visao-geral.md` |
| 02 | `02-monorepo-git-flow.md` |
| 03 | `03-infraestrutura-local.md` |
| 04 | `04-user-api-auth-jwt.md` |
| 05 | `05-product-api.md` |
| 06 | `06-delivery-estimator-api.md` |
| 07 | `07-order-api.md` |
| 08 | `08-rabbitmq-eventos.md` |
| 09 | `09-saga-pedido.md` |
| 10 | `10-fraud-detector-api.md` |
| 11 | `11-transactional-outbox.md` |
| 12 | `12-idempotencia-estoque.md` |
| 13 | `13-api-gateway-krakend.md` |
| 14 | `14-jwt-gateway-jwks.md` |
| 15 | `15-observabilidade.md` |
| 16 | `16-tratamento-erros.md` |
| 17 | `17-testes-http-json.md` |
| 18 | `18-roadmap-debitos-tecnicos.md` |

## Como usar esta apostila

Clone o projeto, suba a infraestrutura com Docker Compose e siga os capítulos em ordem.

```bash
docker compose up -d --build
```

O acesso principal ao sistema deve ser feito pelo API Gateway:

```text
http://localhost:8099
```

Os microsserviços não devem ser acessados diretamente pelo host. Eles ficam disponíveis apenas na rede interna do Docker Compose.

## Convenções usadas

- `user-api`: serviço de usuários e autenticação;
- `product-api`: serviço de produtos e estoque;
- `delivery-estimator-api`: serviço de estimativa de entrega;
- `order-api`: serviço de pedidos e orquestração do fluxo;
- `fraud-detector-api`: serviço de análise de fraude;
- `api-gateway`: KrakenD;
- `ecommerce.events`: exchange principal do RabbitMQ;
- `orders`, `products`, `delivery`, `fraud`: schemas lógicos no PostgreSQL.

## Status da apostila

Esta é a versão V1, cobrindo o projeto até a suíte de testes HTTP/JSON.

Uma versão V2 pode ser criada depois com:

- testes automatizados;
- Kubernetes;
- métricas de negócio;
- dashboards Grafana;
- logs centralizados;
- Outbox nos demais serviços;
- Tribuo no `delivery-estimator-api`;
- BFF conversacional com LangChain4j.


---

