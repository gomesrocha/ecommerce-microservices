# 05 — Fraud Detector com Machine Learning

## Objetivo

O `fraud-detector-api` passou a usar um modelo Tribuo para classificar pedidos como:

```text
LEGIT
FRAUD_RISK
```

## Tipo de modelo

O problema de fraude é um problema de classificação.

Queremos prever uma categoria.

## Dataset usado

Arquivo processado:

```text
data/fraud/olist/processed/olist_fraud_risk_training_tribuo.csv
```

Coluna alvo:

```text
label
```

Valores:

```text
LEGIT
FRAUD_RISK
```

## Atenção importante

A Olist não possui fraude real.

O label `FRAUD_RISK` foi criado por heurística.

Portanto, o modelo não aprende fraude real, mas sim uma regra simulada de risco operacional.

## Heurísticas usadas para gerar o label

O script adiciona pontos de risco para:

- valor total alto;
- valor no top 5% ou top 1%;
- muitos itens;
- item muito caro;
- muitos produtos distintos;
- rota rara;
- compra interestadual.

Depois:

```text
score >= limite => FRAUD_RISK
score < limite  => LEGIT
```

## Treinamento

Classe:

```text
FraudModelTrainer
```

Fluxo:

```text
1. lê CSV com CSVLoader;
2. usa LabelFactory;
3. divide treino e teste;
4. treina CARTClassificationTrainer;
5. avalia com LabelEvaluator;
6. salva fraud-tribuo-v1.model.
```

## Métricas obtidas

Exemplo:

```text
Accuracy = 0.999
Macro F1 = 0.994
Balanced Error Rate = 0.006
```

## Interpretação das métricas

Essas métricas ficaram muito altas porque o label foi gerado por regra. O modelo aprendeu a reproduzir a heurística.

Isso é aceitável para fins didáticos, mas não deve ser confundido com um antifraude real.

## Modelo salvo

```text
models/fraud/fraud-tribuo-v1.model
```

## Uso no microsserviço

No Docker Compose:

```yaml
volumes:
  - ./models/fraud:/models/fraud:ro
```

Variável:

```text
APP_FRAUD_ML_MODEL_PATH=/models/fraud/fraud-tribuo-v1.model
```

## Classe de inferência

Classe:

```text
FraudTribuoModelService
```

Responsabilidades:

```text
1. carregar fraud-tribuo-v1.model;
2. montar example a partir do pedido;
3. chamar model.predict;
4. retornar FraudPredictionResult.
```

## FraudMlFeatureBuilder

Cria features compatíveis com o treinamento:

```text
total_amount
items_quantity
avg_item_price
max_item_price
unique_products
origin_*
destination_*
```

## FraudPredictionResult

Record usado para transportar o resultado:

```java
public record FraudPredictionResult(
    String label,
    boolean fraudRisk,
    double riskScore,
    String reason,
    String modelVersion
) {}
```

## Integração com FraudAnalysisService

O service tenta primeiro:

```text
FraudTribuoModelService
```

Se o modelo estiver pronto, usa a predição.

Se não estiver, usa a regra anterior.

Fluxo:

```text
OrderCreatedEvent
  ↓
FraudAnalysisService
  ↓
FraudTribuoModelService
  ↓
FraudPredictionResult
  ↓
FraudAnalysis
  ↓
FraudEventPublisher
```

## Resultado esperado

Pedido legítimo:

```text
APPROVED
Pedido classificado como legítimo pelo modelo Tribuo.
```

Pedido de risco:

```text
REJECTED
Pedido classificado como risco de fraude pelo modelo Tribuo.
```

## Limitações

Para produção, seria necessário usar dados reais de:

- chargeback;
- contestação;
- bloqueio antifraude;
- revisão manual;
- histórico de cliente;
- endereço;
- forma de pagamento;
- dispositivo;
- IP;
- reputação do comprador.
