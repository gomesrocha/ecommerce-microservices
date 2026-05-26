# 02 — Tribuo: Conceitos, Funcionamento e Uso

## O que é Tribuo?

Tribuo é uma biblioteca de Machine Learning escrita em Java. Ela permite criar, treinar, avaliar, serializar e usar modelos de Machine Learning diretamente na JVM.

Tribuo suporta vários tipos de tarefa:

- classificação;
- regressão;
- clustering;
- detecção de anomalias;
- classificação multi-label.

## Por que usar Tribuo neste projeto?

O projeto é baseado em Java e Quarkus. Usar Tribuo permite manter a etapa de inferência dentro do ecossistema Java, sem precisar criar um serviço Python apenas para predição.

Vantagens:

- integração natural com Java;
- tipagem forte;
- API orientada a objetos;
- suporte a avaliação de modelos;
- suporte a serialização;
- possibilidade de carregar modelos prontos no microsserviço;
- bom encaixe com Quarkus.

## Conceitos principais

### Feature

Uma feature é uma variável de entrada usada pelo modelo.

Exemplos no delivery:

```text
items_quantity
origin_BA
destination_AL
```

Exemplos na fraude:

```text
total_amount
items_quantity
max_item_price
origin_SP
destination_SE
```

No Tribuo, uma feature pode ser criada assim:

```java
new Feature("total_amount", 1000.00)
```

### Example

Um `Example` representa uma amostra. Ele contém um output esperado, no treino, e um conjunto de features.

Exemplo conceitual:

```java
ArrayExample<Label> example = new ArrayExample<>(
    new Label("FRAUD_RISK"),
    features
);
```

Durante inferência, usamos um label placeholder, como:

```java
new Label("UNKNOWN")
```

### Output

O tipo de saída muda conforme a tarefa.

Para classificação:

```java
Label
```

Para regressão:

```java
Regressor
```

### Dataset

Um dataset é um conjunto de exemplos. Durante o treinamento, normalmente o dataset é dividido em treino e teste.

### Trainer

O trainer é o algoritmo usado para treinar o modelo.

Neste projeto usamos árvores CART:

```java
CARTRegressionTrainer
CARTClassificationTrainer
```

### Model

O modelo é o resultado do treinamento. Ele contém os parâmetros aprendidos e pode gerar predições.

```java
Prediction<Label> prediction = model.predict(example);
```

### Prediction

A predição é o resultado da chamada ao modelo.

Para classificação, retorna um `Label`.

Para regressão, retorna um `Regressor`.

## Regressão

Regressão é usada quando queremos prever um número.

No projeto:

```text
delivery_days
```

Exemplo:

```text
entrada: BA -> AL, 1 item
saída: 25 dias
```

## Classificação

Classificação é usada quando queremos prever uma categoria.

No projeto:

```text
LEGIT
FRAUD_RISK
```

Exemplo:

```text
entrada: pedido de alto valor
saída: FRAUD_RISK
```

## Treinamento com CSV

O projeto usa `CSVLoader`.

A ideia é ter um CSV com colunas de features e uma coluna alvo.

Delivery:

```text
delivery_days
```

Fraude:

```text
label
```

## Avaliação

Para delivery, usamos métricas de regressão:

- RMSE;
- MAE;
- R2.

Para fraude, usamos métricas de classificação:

- accuracy;
- precision;
- recall;
- f1;
- balanced error rate.

## Serialização do modelo

Depois de treinar, o modelo é salvo em arquivo:

```text
delivery-tribuo-v1.model
fraud-tribuo-v1.model
```

No projeto, usamos serialização Java:

```java
ObjectOutputStream
```

Para carregar:

```java
ObjectInputStream
```

## Por que serializar?

Porque o microsserviço não deve treinar modelo no startup. Ele deve apenas carregar um modelo pronto e executar:

```java
model.predict(example)
```

## Fluxo completo no projeto

```text
CSV processado
  ↓
CSVLoader
  ↓
TrainTestSplitter
  ↓
MutableDataset
  ↓
Trainer
  ↓
Model
  ↓
Evaluator
  ↓
ObjectOutputStream
  ↓
.model
```

## Cuidado importante

As features usadas na inferência precisam ter os mesmos nomes das features usadas no treinamento.

Exemplo:

```text
Treino: origin_BA
Inferência: origin_BA
```

Se o nome mudar, o modelo não receberá o sinal esperado.

## Tribuo no projeto

Usamos Tribuo em três lugares:

```text
ml-model-builder
delivery-estimator-api
fraud-detector-api
```

### No ml-model-builder

Treina e salva modelos.

### No delivery-estimator-api

Carrega modelo de regressão e prevê prazo.

### No fraud-detector-api

Carrega modelo de classificação e prevê risco.
