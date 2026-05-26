# 07 — Inferência nos Microsserviços

## O que é inferência?

Inferência é o uso de um modelo treinado para fazer predições sobre novos dados.

No projeto:

```text
delivery-estimator-api recebe origem/destino/itens
  ↓
modelo prevê prazo

fraud-detector-api recebe pedido
  ↓
modelo classifica risco
```

## Diferença entre treino e inferência

Treino:

```text
dataset histórico
algoritmo
métricas
modelo salvo
```

Inferência:

```text
entrada nova
feature builder
model.predict
resposta
```

## Carregamento dos modelos

Os modelos são montados por volume no Docker Compose.

Delivery:

```yaml
volumes:
  - ./models/delivery:/models/delivery:ro
```

Fraude:

```yaml
volumes:
  - ./models/fraud:/models/fraud:ro
```

## Variáveis de ambiente

Delivery:

```text
APP_DELIVERY_ML_ENABLED=true
APP_DELIVERY_ML_PREFER_MODEL=true
APP_DELIVERY_ML_MODEL_PATH=/models/delivery/delivery-tribuo-v1.model
```

Fraude:

```text
APP_FRAUD_ML_ENABLED=true
APP_FRAUD_ML_MODEL_PATH=/models/fraud/fraud-tribuo-v1.model
```

## Carregamento em StartupEvent

Os microsserviços carregam o modelo no startup.

Exemplo conceitual:

```java
void onStart(@Observes StartupEvent event) {
    loadModel();
}
```

## Leitura com ObjectInputStream

O modelo é carregado com:

```java
ObjectInputStream
```

Fluxo:

```text
1. verifica se arquivo existe;
2. abre input stream;
3. lê objeto;
4. valida se é Model;
5. guarda em memória.
```

## Por que manter em memória?

Para evitar ler o arquivo a cada requisição.

O modelo é carregado uma vez e usado várias vezes.

## Feature Builder

Cada microsserviço possui um builder para montar features.

### DeliveryMlFeatureBuilder

Gera:

```text
items_quantity
origin_*
destination_*
```

### FraudMlFeatureBuilder

Gera:

```text
total_amount
items_quantity
avg_item_price
max_item_price
unique_products
origin_*
destination_*
```

## Regra essencial

Os nomes das features na inferência precisam ser iguais aos nomes usados no treino.

Exemplo:

```text
Treino: origin_BA
Inferência: origin_BA
```

Se a inferência gerar:

```text
originState_BA
```

o modelo não reconhecerá corretamente.

## Fallbacks

Os dois serviços mantêm fallback.

### Delivery

Se o modelo não carregar:

```text
OLIST_BASELINE
FALLBACK_RULE
```

### Fraude

Se o modelo não carregar:

```text
regra fixa de score
```

Isso evita que o sistema pare por ausência do modelo.

## Benefício arquitetural

Com essa separação:

```text
ml-model-builder
  treina

microsserviços
  predizem
```

o sistema fica mais limpo e mais próximo de produção.
