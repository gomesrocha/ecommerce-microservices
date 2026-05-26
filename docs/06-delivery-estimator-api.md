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
