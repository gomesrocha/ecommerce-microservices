# 04 — Delivery Estimator com Machine Learning

## Objetivo

O `delivery-estimator-api` passou a usar Machine Learning para prever o prazo de entrega.

Antes:

```text
rotas manuais
OLIST_BASELINE
fallback
```

Depois:

```text
TRIBUO_MODEL
OLIST_BASELINE
fallback
```

## Tipo de modelo

O problema de entrega é um problema de regressão. Queremos prever:

```text
delivery_days
```

Ou seja, um número.

## Dataset de treino

Arquivo usado:

```text
delivery-estimator-api/src/main/resources/ml/olist_delivery_training_tribuo.csv
```

Coluna alvo:

```text
delivery_days
```

Features iniciais:

```text
items_quantity
origin_*
destination_*
```

## Treinamento

O treinamento é feito no projeto:

```text
ml-model-builder
```

Classe:

```text
DeliveryModelTrainer
```

Fluxo:

```text
1. lê CSV com CSVLoader;
2. separa treino e teste;
3. treina CARTRegressionTrainer;
4. avalia RMSE, MAE e R2;
5. salva delivery-tribuo-v1.model.
```

## Métricas obtidas

Exemplo de saída:

```text
averageRMSE=7.848
averageMAE=5.414
averageR2=0.207
```

## Interpretação

### MAE

O erro médio absoluto ficou em torno de 5 dias.

### R2

O R2 ficou modesto. Isso significa que o modelo ainda explica pouco da variação real do prazo de entrega.

## Por que o modelo ainda é limitado?

As primeiras features usadas foram simples:

```text
origem
destino
quantidade de itens
```

Mas prazo de entrega depende de mais fatores:

- transportadora;
- frete;
- peso;
- volume;
- categoria;
- vendedor;
- distância real;
- região;
- datas sazonais;
- feriados;
- atrasos logísticos.

## Modelo salvo

O modelo treinado é salvo em:

```text
models/delivery/delivery-tribuo-v1.model
```

## Uso no microsserviço

No Docker Compose, o modelo é montado no container:

```yaml
volumes:
  - ./models/delivery:/models/delivery:ro
```

Variável:

```text
APP_DELIVERY_ML_MODEL_PATH=/models/delivery/delivery-tribuo-v1.model
```

## Classe de inferência

Classe:

```text
DeliveryTribuoModelService
```

Responsabilidades:

```text
1. carregar modelo no startup;
2. verificar se o modelo existe;
3. montar example com DeliveryMlFeatureBuilder;
4. executar model.predict;
5. converter a predição em minDays, estimatedDays e maxDays.
```

## Endpoint não mudou

O endpoint continua:

```text
POST /delivery-estimates/estimate
```

Payload:

```json
{
  "originState": "BA",
  "destinationState": "AL",
  "totalItems": 1
}
```

Resposta esperada:

```json
{
  "originState": "BA",
  "destinationState": "AL",
  "totalItems": 1,
  "minDays": 23,
  "estimatedDays": 25,
  "maxDays": 29,
  "source": "TRIBUO_MODEL",
  "modelVersion": "delivery-tribuo-v1"
}
```

## Fallback

Se o modelo não for carregado, o service continua funcionando com:

```text
OLIST_BASELINE
FALLBACK_RULE
```

Isso é importante para resiliência.
