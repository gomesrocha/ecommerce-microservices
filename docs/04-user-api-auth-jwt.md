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
