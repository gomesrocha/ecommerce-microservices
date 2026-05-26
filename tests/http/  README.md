# HTTP Test Suite

Esta pasta contém testes manuais versionados para validar o fluxo do ecommerce via API Gateway KrakenD.

Os testes devem ser executados preferencialmente pela porta do gateway:

```text
http://localhost:8099
```
Os microsserviços não devem ser acessados diretamente pelo host.

Ordem sugerida
00-auth.http
01-health.http
02-products.http
03-delivery-estimates.http
04-orders-success.http
05-orders-validation-errors.http
06-orders-stock-errors.http
07-orders-fraud-flow.http
08-gateway-security.http
Variáveis principais

```text
@gateway = http://localhost:8099
@token = cole_o_access_token_aqui
```