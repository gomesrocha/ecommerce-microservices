# 06 — Projeto ml-model-builder

## Objetivo

O `ml-model-builder` é um projeto Java separado, dentro do monorepo, responsável por treinar modelos de Machine Learning.

Ele evita que os microsserviços treinem modelos durante o startup.

## Estrutura

```text
ml-model-builder/
├── pom.xml
├── README.md
└── src/main/java/br/com/ecommerce/mlbuilder/
    ├── ModelBuilderMain.java
    ├── common/
    │   └── ModelIO.java
    ├── delivery/
    │   └── DeliveryModelTrainer.java
    └── fraud/
        └── FraudModelTrainer.java
```

## ModelBuilderMain

Classe principal.

Comandos suportados:

```text
train-delivery
train-fraud
train-all
```

Exemplo:

```bash
mvn -f ml-model-builder/pom.xml clean compile exec:java -Dexec.args="train-all"
```

Também funciona dentro da pasta:

```bash
cd ml-model-builder
mvn clean compile exec:java -Dexec.args="train-all"
```

## resolveRepoRoot

O `ModelBuilderMain` detecta se está sendo executado:

- da raiz do monorepo;
- de dentro da pasta `ml-model-builder`.

Isso evita erros de caminho relativo.

## DeliveryModelTrainer

Responsável por treinar o modelo de entrega.

Entrada:

```text
delivery-estimator-api/src/main/resources/ml/olist_delivery_training_tribuo.csv
```

Saída:

```text
models/delivery/delivery-tribuo-v1.model
```

Algoritmo:

```text
CARTRegressionTrainer
```

Métricas:

```text
averageRMSE
averageMAE
averageR2
```

## FraudModelTrainer

Responsável por treinar o modelo de risco/fraude.

Entrada:

```text
data/fraud/olist/processed/olist_fraud_risk_training_tribuo.csv
```

Saída:

```text
models/fraud/fraud-tribuo-v1.model
```

Algoritmo:

```text
CARTClassificationTrainer
```

Métricas:

```text
accuracy
precision
recall
f1
balanced error rate
```

## ModelIO

Classe utilitária para salvar modelos.

Usa:

```java
ObjectOutputStream
```

Responsabilidade:

```text
1. criar diretório de saída;
2. abrir stream;
3. serializar objeto Model;
4. salvar arquivo .model.
```

## Por que separar o treinamento?

Porque treinamento:

- depende de datasets;
- é mais pesado;
- calcula métricas;
- pode variar de versão para versão;
- deve ser executado por pipeline ou processo controlado.

Inferência:

- precisa ser rápida;
- deve iniciar com modelo pronto;
- não deve depender de CSV grande;
- não deve retreinar a cada deploy.

## Comandos principais

Treinar todos:

```bash
mvn -f ml-model-builder/pom.xml clean compile exec:java -Dexec.args="train-all"
```

Treinar delivery:

```bash
mvn -f ml-model-builder/pom.xml clean compile exec:java -Dexec.args="train-delivery"
```

Treinar fraude:

```bash
mvn -f ml-model-builder/pom.xml clean compile exec:java -Dexec.args="train-fraud"
```

## Artefatos gerados

```text
models/delivery/delivery-tribuo-v1.model
models/fraud/fraud-tribuo-v1.model
```

## Versionamento

Nesta etapa, os modelos `.model` não são commitados no Git.

Em um ambiente real, eles deveriam ser publicados em:

- object storage;
- repositório de artefatos;
- registry de modelos;
- pipeline CI/CD;
- volume versionado.
