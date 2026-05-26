# Ecommerce Microservices

Projeto de estudo para construção de um sistema simples de pedidos usando arquitetura de microsserviços.

A proposta inicial é usar **Java 21 com Quarkus** para os serviços principais e permitir o uso de **Python** em serviços mais aderentes a dados e machine learning, como a predição de tempo de entrega com a base pública da Olist.

## Objetivos do projeto

- Criar um sistema simples de ecommerce/pedidos.
- Praticar arquitetura de microsserviços.
- Aplicar os princípios do **12-Factor App**.
- Usar padrões conhecidos de microsserviços, especialmente os padrões documentados em microservices.io.
- Usar PostgreSQL e RabbitMQ em ambiente local com Docker Compose.
- Evoluir o projeto por branches e Pull Requests.
- Manter o projeto organizado como monorepo.

## Serviços planejados

| Serviço | Tecnologia sugerida | Responsabilidade |
|---|---|---|
| `user-api` | Java 21 + Quarkus | Usuários, autenticação, JWT e perfis |
| `product-api` | Java 21 + Quarkus | Produtos, catálogo e estoque |
| `shopping-api` ou `order-api` | Java 21 + Quarkus | Carrinho, criação de pedidos e status |
| `estimated-delivery-api` | Python + FastAPI | Predição de prazo de entrega usando dados da Olist |
| `fraud-detector-api` | Python ou Java | Análise assíncrona de risco/fraude |
| `api-gateway` ou `bff-api` | Java ou Python | Entrada única para clientes/frontend, em etapa futura |

> Observação: o nome `shopping-api` pode ser mantido no início, mas uma evolução natural é renomear para `order-api`, caso o serviço fique focado em pedidos.

## Estrutura esperada do monorepo

```text
ecommerce/
├── docker-compose.yml
├── .env.example
├── .gitignore
├── README.md
├── Makefile
├── infra/
│   ├── postgres/
│   │   └── init.sql
│   └── rabbitmq/
│       ├── rabbitmq.conf
│       └── definitions.json
├── user-api/
│   ├── pom.xml
│   └── src/
├── product-api/
├── shopping-api/
├── estimated-delivery-api/
└── fraud-detector-api/
```

## Decisões iniciais de arquitetura

### Monorepo

O projeto será mantido em um único repositório para facilitar o estudo, a organização das aulas e a evolução incremental dos serviços.

### Banco de dados

Neste momento, será utilizado **um único PostgreSQL no Docker Compose**, com separação lógica por schema.

Schemas iniciais:

```text
users
products
orders
delivery
fraud
```

Essa abordagem facilita o desenvolvimento local e mantém a ideia de isolamento por serviço. A regra principal é:

> Um serviço não deve acessar diretamente as tabelas ou schemas de outro serviço.

A comunicação entre serviços deve acontecer por API HTTP ou eventos via RabbitMQ.

### Multi-tenancy

Neste momento, não será adotado schema por cliente. A sugestão inicial é usar schema por serviço e, futuramente, quando necessário, adicionar `tenant_id` nas tabelas.

Schema por cliente pode ser estudado depois como evolução de multi-tenancy.

### Mensageria

O RabbitMQ será usado para eventos assíncronos entre serviços.

Exchange inicial sugerida:

```text
ecommerce.events
```

Filas iniciais sugeridas:

```text
orders.created
products.stock-reservation
delivery.estimate-requested
```

Eventos iniciais sugeridos:

```text
order.created
product.stock.reserve
delivery.estimate.requested
```

## Padrões de microsserviços previstos

| Padrão | Aplicação no projeto |
|---|---|
| Database per Service | Schema por serviço no mesmo PostgreSQL local |
| API Gateway / BFF | Futuramente, para expor uma entrada única para o frontend |
| Saga | Fluxo de criação de pedido, reserva de estoque, análise de fraude e confirmação |
| Transactional Outbox | Futuramente, para garantir persistência do pedido e publicação confiável de eventos |
| API Composition | Futuramente, para montar visões como detalhes completos do pedido |
| Circuit Breaker | Futuramente, para chamadas HTTP entre serviços |
| Service per Container | Futuramente, cada serviço terá seu próprio container |

## Infraestrutura local

A foundation inicial usa:

- PostgreSQL
- RabbitMQ com Management UI
- Docker Compose

## Configuração de ambiente

Crie o arquivo `.env` a partir do exemplo:

```bash
cp .env.example .env
```

Exemplo de `.env.example`:

```env
# PostgreSQL
POSTGRES_DB=ecommerce
POSTGRES_USER=ecommerce
POSTGRES_PASSWORD=ecommerce

# User API
USER_API_PORT=8094
USER_API_DB_URL=jdbc:postgresql://localhost:5432/ecommerce
USER_API_DB_USERNAME=ecommerce
USER_API_DB_PASSWORD=ecommerce
USER_API_JWT_ISSUER=user-api
USER_API_ACCESS_TOKEN_MINUTES=30
USER_API_REFRESH_TOKEN_MINUTES=10080

# RabbitMQ
RABBITMQ_DEFAULT_USER=ecommerce
RABBITMQ_DEFAULT_PASS=ecommerce
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
```

## Subindo a infraestrutura local

```bash
docker compose up -d
```

Verificar containers:

```bash
docker compose ps
```

Ver logs do PostgreSQL:

```bash
docker compose logs -f postgres
```

Ver logs do RabbitMQ:

```bash
docker compose logs -f rabbitmq
```

## Acessos locais

| Recurso | URL |
|---|---|
| PostgreSQL | `localhost:5432` |
| RabbitMQ AMQP | `localhost:5672` |
| RabbitMQ Management | `http://localhost:15672` |
| User API | `http://localhost:8094` |
| Swagger User API | `http://localhost:8094/swagger-ui` |
| Health User API | `http://localhost:8094/q/health` |

Credenciais padrão do RabbitMQ:

```text
Usuário: ecommerce
Senha: ecommerce
```

## Rodando o `user-api` em modo desenvolvimento

Enquanto a containerização dos serviços não for implementada, rode o `user-api` localmente:

```bash
cd user-api
./mvnw quarkus:dev
```

Ou, se estiver usando Maven instalado globalmente:

```bash
cd user-api
mvn quarkus:dev
```

## Configuração do `user-api`

O `application.properties` deve usar variáveis de ambiente, evitando credenciais fixas no código.

Exemplo:

```properties
quarkus.http.port=${PORT:8094}

quarkus.application.name=user-api

quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=${DB_USERNAME:ecommerce}
quarkus.datasource.password=${DB_PASSWORD:ecommerce}
quarkus.datasource.jdbc.url=${DB_URL:jdbc:postgresql://localhost:5432/ecommerce}

quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-orm.log.sql=${HIBERNATE_LOG_SQL:false}
quarkus.hibernate-orm.database.default-schema=users

quarkus.datasource.devservices.enabled=false

mp.jwt.verify.publickey.location=${JWT_PUBLIC_KEY_LOCATION:publicKey.pem}
mp.jwt.verify.issuer=${JWT_ISSUER:user-api}

smallrye.jwt.sign.key.location=${JWT_PRIVATE_KEY_LOCATION:privateKey.pem}

app.jwt.issuer=${JWT_ISSUER:user-api}
app.jwt.access-token-minutes=${JWT_ACCESS_TOKEN_MINUTES:30}
app.jwt.refresh-token-minutes=${JWT_REFRESH_TOKEN_MINUTES:10080}

quarkus.swagger-ui.always-include=true
quarkus.swagger-ui.path=/swagger-ui

quarkus.smallrye-health.root-path=/q/health
```

## Dockerfile dos serviços

A criação dos Dockerfiles dos serviços ficará como débito técnico controlado.

Neste momento, a foundation prioriza:

- organização do monorepo;
- infraestrutura local;
- variáveis de ambiente;
- schemas iniciais;
- RabbitMQ;
- documentação;
- fluxo de branches.

### Débito técnico registrado

```text
TD-001: Criar Dockerfiles dos microsserviços
```

Descrição:

> Criar Dockerfiles para os serviços Quarkus e Python. Para os serviços Quarkus, avaliar a criação de imagens JVM inicialmente e, depois, imagens nativas usando `Dockerfile.native-micro`.

### Dockerfile nativo do Quarkus

O Quarkus gera exemplos de Dockerfile em:

```text
src/main/docker/
```

O arquivo `Dockerfile.native-micro` pode ser usado quando o serviço for compilado como executável nativo.

Exemplo de build nativo futuro:

```bash
cd user-api
./mvnw package -Dnative
docker build -f src/main/docker/Dockerfile.native-micro -t ecommerce/user-api:native .
```

Esse Dockerfile espera que exista um binário nativo em:

```text
target/*-runner
```

Portanto, ele não deve ser usado para build JVM comum.

## Makefile sugerido

Arquivo `Makefile` na raiz:

```makefile
up:
	docker compose up -d

down:
	docker compose down

logs:
	docker compose logs -f

ps:
	docker compose ps

restart:
	docker compose down
	docker compose up -d

clean:
	docker compose down -v

user-dev:
	cd user-api && ./mvnw quarkus:dev

test-user:
	cd user-api && ./mvnw test
```

Uso:

```bash
make up
make ps
make logs
make user-dev
```

## Estratégia de branches

```text
main        -> produção / versão estável
develop     -> integração
feature/*   -> novas funcionalidades
hotfix/*    -> correções urgentes
```

## Branch atual sugerida

```bash
git checkout develop
git pull origin develop

git checkout -b feature/foundation-monorepo
```

## Checklist do PR `feature/foundation-monorepo`

```markdown
## Objetivo

Organizar a base do monorepo para desenvolvimento dos microsserviços de ecommerce.

## Alterações

- [ ] Mover `docker-compose.yml` para a raiz do projeto
- [ ] Adicionar PostgreSQL no Docker Compose
- [ ] Adicionar RabbitMQ com Management UI
- [ ] Criar schemas iniciais por serviço
- [ ] Criar `.env.example`
- [ ] Criar `.gitignore` da raiz
- [ ] Remover arquivos locais do versionamento: `.idea`, `target`, `.env`, chaves
- [ ] Ajustar `application.properties` do `user-api` para variáveis de ambiente
- [ ] Adicionar README inicial
- [ ] Adicionar Makefile com comandos úteis
- [ ] Registrar Dockerfile nativo como débito técnico

## Serviços impactados

- user-api
- infraestrutura local

## Como testar

```bash
cp .env.example .env
docker compose up -d
docker compose ps
cd user-api
./mvnw quarkus:dev
```

Depois, em outro terminal:

```bash
curl http://localhost:8094/q/health
```

## Próximos PRs sugeridos

- `feature/product-api`
- `feature/order-api`
- `feature/rabbitmq-events`
- `feature/delivery-estimator-api`
- `feature/native-dockerfiles`
```

## Comandos para commit

```bash
git status
git add .
git commit -m "chore: organize monorepo foundation"
git push -u origin feature/foundation-monorepo
```

Criar PR com GitHub CLI:

```bash
gh pr create \
  --base develop \
  --head feature/foundation-monorepo \
  --title "chore: organize monorepo foundation" \
  --body "Organiza a base do monorepo com Docker Compose, PostgreSQL, RabbitMQ, variáveis de ambiente, schemas iniciais e documentação."
```

## Próxima etapa

Após a foundation, a próxima branch recomendada é:

```text
feature/product-api
```

Objetivo da próxima etapa:

- criar o serviço `product-api`;
- configurar schema `products`;
- criar entidade de produto;
- criar endpoints CRUD;
- preparar eventos de estoque para uso futuro com RabbitMQ.
