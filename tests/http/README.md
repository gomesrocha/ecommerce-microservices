# HTTP JSON Test Suite

Esta pasta contém testes HTTP versionados para validar o ecommerce via API Gateway KrakenD.

## Pré-requisitos

Subir a stack:

```bash
docker compose up -d --build
```

Gateway:

```text
http://localhost:8099
```

## Ordem recomendada

Execute primeiro:

```text
00-auth.http
```

O arquivo `00-auth.http` salva automaticamente:

```text
access_token
refresh_token
```

Depois execute os demais arquivos:

```text
01-health.http
02-products.http
03-delivery-estimates.http
04-orders-success.http
05-orders-validation-errors.http
06-orders-stock-errors.http
07-orders-fraud-flow.http
08-gateway-security.http
```

## Observação sobre variáveis

Os arquivos usam variáveis globais do HTTP Client do IntelliJ/JetBrains:

```http
Authorization: Bearer {{access_token}}
```

Portanto, sempre execute o login no `00-auth.http` antes dos demais testes.

## Observação sobre fluxos assíncronos

Alguns testes envolvem RabbitMQ, Saga, estoque e fraude. Depois de criar pedidos, aguarde alguns segundos antes de consultar o pedido e o histórico.

## Observação sobre segurança do gateway

Os testes de acesso direto aos microsserviços devem falhar se a feature `api-gateway-only-access` estiver ativa, pois os serviços usam `expose` e não `ports`.
