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
