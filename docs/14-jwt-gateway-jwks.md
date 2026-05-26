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
