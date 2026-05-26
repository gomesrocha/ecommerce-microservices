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
