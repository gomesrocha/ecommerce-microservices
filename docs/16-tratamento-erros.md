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
