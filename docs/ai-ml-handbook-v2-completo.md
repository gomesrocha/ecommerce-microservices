# 01 — Visão Geral da Trilha de IA/ML

## Por que adicionar IA ao projeto?

Na primeira etapa, o projeto já tinha um fluxo funcional de ecommerce com microsserviços. No entanto, algumas decisões ainda eram baseadas em regras simples:

- prazo de entrega baseado em rota manual;
- análise de fraude baseada em score fixo;
- regras estáticas para aprovação ou rejeição.

A trilha de IA/ML melhora isso usando dados históricos para gerar predições.

## O que foi implementado

Foram criadas três frentes principais:

```text
1. Delivery com Olist + Tribuo
2. Fraude/Risco com Olist + Tribuo
3. Projeto separado para treinamento de modelos
```

## Fluxo geral

```text
Base Olist
  ↓
Scripts Python de ETL
  ↓
CSVs processados para treino
  ↓
ml-model-builder
  ↓
Arquivos .model
  ↓
delivery-estimator-api e fraud-detector-api
```

## Separação entre treinamento e inferência

Um ponto central desta etapa é separar treinamento de inferência.

Treinamento:

- usa datasets;
- pode ser mais lento;
- calcula métricas;
- gera modelo;
- executa fora do microsserviço.

Inferência:

- recebe uma requisição ou evento;
- monta features;
- chama `model.predict`;
- retorna decisão;
- precisa ser rápida e estável.

## Antes

Inicialmente, o `delivery-estimator-api` treinava o modelo durante o startup. Isso era útil para aprendizado, mas não é ideal para produção.

Problemas:

- startup mais lento;
- dataset precisa estar dentro do serviço;
- treinamento se repete a cada deploy;
- dificulta controle de versão do modelo;
- aumenta responsabilidade do microsserviço.

## Depois

Agora o treinamento fica no projeto:

```text
ml-model-builder
```

Os modelos treinados são salvos em:

```text
models/delivery/delivery-tribuo-v1.model
models/fraud/fraud-tribuo-v1.model
```

Os microsserviços apenas carregam os arquivos prontos.

## Microsserviços impactados

### delivery-estimator-api

Passou a carregar:

```text
/models/delivery/delivery-tribuo-v1.model
```

E responder:

```json
{
  "source": "TRIBUO_MODEL",
  "modelVersion": "delivery-tribuo-v1"
}
```

### fraud-detector-api

Passou a carregar:

```text
/models/fraud/fraud-tribuo-v1.model
```

E usar a predição para aprovar ou rejeitar pedidos.

## Importante sobre fraude

A Olist não possui uma coluna real de fraude. Por isso, a parte de fraude nesta etapa é chamada de risco de fraude simulado.

O label foi criado por heurísticas de risco, como:

- valor alto;
- quantidade alta de itens;
- produto caro;
- rota rara;
- pedido interestadual.

Essa abordagem é útil para fins didáticos, mas não substitui dados reais de chargeback, contestação, bloqueio antifraude ou revisão manual.


---

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


---

# 03 — Datasets Olist e Preparação dos Dados

## Por que usar Olist?

A base Olist representa dados reais de ecommerce brasileiro. Ela possui informações sobre pedidos, clientes, vendedores, produtos, itens, pagamentos, reviews e datas de compra e entrega.

No projeto, usamos a Olist para gerar dois datasets:

```text
1. dataset de entrega;
2. dataset de risco/fraude simulado.
```

## Organização das pastas

```text
data/
└── olist/
    ├── raw/
    └── processed/
```

Dados brutos:

```text
data/olist/raw
```

Dados processados:

```text
data/olist/processed
```

## Arquivos principais da Olist

### olist_orders_dataset.csv

Contém informações do pedido:

- order_id;
- customer_id;
- status;
- data de compra;
- data real de entrega;
- data estimada de entrega.

### olist_order_items_dataset.csv

Contém os itens do pedido:

- order_id;
- product_id;
- seller_id;
- price;
- freight_value;
- quantidade de itens.

### olist_customers_dataset.csv

Contém dados do cliente. Usado para derivar:

```text
destination_state
```

### olist_sellers_dataset.csv

Contém dados do vendedor. Usado para derivar:

```text
origin_state
```

### olist_products_dataset.csv

Contém dados do produto:

- peso;
- dimensão;
- categoria.

## ETL de delivery

Script:

```text
tools/olist/prepare_delivery_dataset.py
```

Gera:

```text
data/olist/processed/olist_delivery_training.csv
data/olist/processed/olist_delivery_route_baseline.csv
```

Depois, outro script gera o CSV numérico para Tribuo:

```text
tools/olist/prepare_tribuo_delivery_training.py
```

Saída:

```text
delivery-estimator-api/src/main/resources/ml/olist_delivery_training_tribuo.csv
```

## ETL de fraude/risco

Script:

```text
tools/fraud/prepare_olist_fraud_risk_dataset.py
```

Gera:

```text
data/fraud/olist/processed/olist_fraud_risk_training_tribuo.csv
```

## Por que gerar CSV numérico?

O modelo precisa receber features numéricas.

Estados como:

```text
BA
AL
SP
SE
```

são convertidos em one-hot encoding:

```text
origin_BA
origin_SP
destination_AL
destination_SE
```

Exemplo:

```text
origin_BA = 1
origin_SP = 0
destination_AL = 1
destination_SE = 0
```

## Por que não versionar os CSVs grandes?

Os CSVs brutos e processados podem ser grandes.

Por isso, eles ficam no `.gitignore`.

Versionamos:

- scripts;
- README;
- estrutura de pastas;
- código de treinamento.

Não versionamos:

- CSV bruto;
- CSV processado grande;
- modelos `.model`, nesta etapa.

## Comandos úteis

Gerar dataset de delivery:

```bash
uv run --with pandas python tools/olist/prepare_delivery_dataset.py
uv run --with pandas python tools/olist/prepare_tribuo_delivery_training.py
```

Gerar dataset de fraude/risco:

```bash
uv run --with pandas python tools/fraud/prepare_olist_fraud_risk_dataset.py
```

## Importante

O dataset de fraude é simulado.

Ele não representa fraude real confirmada. O objetivo é didático: treinar um classificador que aprenda padrões de risco compatíveis com os dados que o sistema consegue calcular em runtime.


---

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


---

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


---

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


---

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


---

# 08 — Testes, Operação e Debug

## Testar geração dos modelos

Na raiz do monorepo:

```bash
mvn -f ml-model-builder/pom.xml clean compile exec:java -Dexec.args="train-all"
```

Esperado:

```text
Modelo salvo em: models/delivery/delivery-tribuo-v1.model
Modelo salvo em: models/fraud/fraud-tribuo-v1.model
```

Verificar arquivos:

```bash
ls -lh models/delivery
ls -lh models/fraud
```

## Testar delivery-estimator-api

Subir serviço:

```bash
docker compose up -d --build delivery-estimator-api
```

Ver logs:

```bash
docker compose logs -f delivery-estimator-api
```

Esperado:

```text
Carregando modelo Tribuo de entrega
Modelo Tribuo de entrega carregado com sucesso
```

Testar endpoint:

```bash
curl -s -X POST http://localhost:8099/api/delivery-estimates/estimate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "originState": "BA",
    "destinationState": "AL",
    "totalItems": 1
  }' | jq
```

Esperado:

```text
source = TRIBUO_MODEL
modelVersion = delivery-tribuo-v1
```

## Testar fraud-detector-api

Subir serviço:

```bash
docker compose up -d --build fraud-detector-api
```

Logs:

```bash
docker compose logs -f fraud-detector-api
```

Esperado:

```text
Carregando modelo Tribuo de fraude
Modelo Tribuo de fraude carregado com sucesso
```

## Teste de fluxo completo

Criar produto:

```bash
PRODUCT_ID=$(curl -s -X POST http://localhost:8099/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Produto ML",
    "description": "Produto para testar ML",
    "sku": "ML-001",
    "price": 100.00,
    "stockQuantity": 10,
    "originState": "SP"
  }' | jq -r '.id')
```

Criar pedido:

```bash
ORDER_ID=$(curl -s -X POST http://localhost:8099/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"userId\": 1,
    \"customerState\": \"SE\",
    \"items\": [
      {
        \"productId\": $PRODUCT_ID,
        \"quantity\": 1
      }
    ]
  }" | jq -r '.id')
```

Consultar fraude:

```bash
curl -s http://localhost:8099/api/fraud-analyses/order/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN" | jq
```

## Problemas comuns

### Modelo não encontrado

Mensagem:

```text
Modelo Tribuo não encontrado
```

Verifique:

```bash
ls -lh models/delivery
ls -lh models/fraud
```

Verifique volume no Docker Compose.

### Feature mismatch

Sintoma:

```text
modelo carrega, mas predições estranhas
```

Causa provável:

```text
nomes de features diferentes entre treino e inferência
```

### Gateway fora do ar

Erro:

```text
Failed to connect to localhost port 8099
```

Solução:

```bash
docker compose up -d api-gateway
docker compose ps api-gateway
```

### Token vazio

Se:

```bash
echo $TOKEN
```

não mostra nada, o login falhou.

Teste:

```bash
curl -i -X POST http://localhost:8099/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## Comandos úteis

Listar containers:

```bash
docker compose ps
```

Logs de delivery:

```bash
docker compose logs --tail=200 delivery-estimator-api
```

Logs de fraude:

```bash
docker compose logs --tail=200 fraud-detector-api
```

Validar modelos:

```bash
ls -lh models/delivery/delivery-tribuo-v1.model
ls -lh models/fraud/fraud-tribuo-v1.model
```


---

# 09 — Limitações e Roadmap

## Limitações atuais

### Delivery

O modelo de entrega usa poucas features.

Atualmente:

```text
origin_state
destination_state
items_quantity
```

Isso explica apenas parte do prazo de entrega.

Faltam:

- peso;
- volume;
- frete;
- categoria;
- vendedor;
- distância;
- data da compra;
- sazonalidade;
- feriados;
- transportadora.

## Fraude/Risco

A fraude é simulada.

A Olist não possui label real de fraude.

O modelo aprende uma heurística criada por nós.

Isso é útil para demonstração, mas não é um antifraude real.

## Métricas muito altas em fraude

A acurácia do modelo de fraude ficou muito alta porque o label foi gerado por regra.

O modelo aprendeu a reproduzir a regra.

Isso é esperado, mas precisa ser explicado em aula.

## Modelos não versionados

Os arquivos `.model` não são commitados no Git.

Em produção, eles deveriam ser gerenciados por:

- pipeline;
- storage versionado;
- registry de modelos;
- controle de checksum;
- política de promoção por ambiente.

## Próximas melhorias

### 1. Melhorar modelo de delivery

Adicionar features:

```text
freight_value
avg_product_weight_g
max_product_weight_g
avg_product_volume_cm3
product_category_name
```

### 2. Melhorar fraude

Adicionar dados mais próximos de antifraude real:

```text
payment_type
payment_installments
freight_ratio
customer history
seller risk
review_score
delay_days
```

### 3. Model registry simples

Criar uma tabela ou arquivo de metadados:

```text
model_name
version
path
created_at
metrics
dataset_hash
active
```

### 4. Automatizar pipeline

Criar comando:

```bash
make train-models
```

ou pipeline CI/CD para:

```text
gerar dataset
treinar modelos
avaliar métricas
publicar artefatos
```

### 5. Testes automatizados

Criar testes para:

- feature builders;
- carregamento de modelos;
- fallback sem modelo;
- inferência de delivery;
- inferência de fraude.

### 6. Métricas de negócio

Expor métricas como:

```text
delivery_predictions_total
fraud_predictions_total
fraud_risk_total
model_load_success_total
model_load_failure_total
fallback_prediction_total
```

### 7. Dashboards

Criar dashboards no Grafana para:

- volume de predições;
- uso de fallback;
- pedidos rejeitados por fraude;
- versão de modelo em uso;
- distribuição dos prazos previstos.

### 8. BFF com LangChain4j + Ollama

Depois da trilha ML, o próximo passo de IA generativa é:

```text
ai-chat-bff
```

Objetivo:

```text
usuário conversa com o ecommerce
consulta produtos
estima entrega
cria pedido
consulta status
```

## Roadmap sugerido

```text
feature/ai-ml-handbook-v2
feature/automated-tests-ml
feature/business-metrics-ml
feature/observability-dashboards-ml
feature/ai-chat-bff-langchain4j-ollama
```

## Conclusão

A trilha V2 introduziu IA de forma incremental e arquiteturalmente limpa.

O principal aprendizado foi separar treinamento de inferência.

Essa separação é fundamental para aplicar Machine Learning em microsserviços de forma sustentável.


---

# Apostila V2 — IA e Machine Learning em Microsserviços com Quarkus, Tribuo e Olist

Esta apostila documenta a segunda etapa do projeto de ecommerce baseado em microsserviços: a introdução de Machine Learning com Java, Quarkus e Tribuo.

A etapa V1 construiu a base arquitetural: Quarkus, PostgreSQL, RabbitMQ, Saga, Transactional Outbox, KrakenD, JWT, observabilidade e testes HTTP.

A etapa V2 adiciona uma camada de IA/ML:

- uso da base Olist;
- preparação de datasets;
- predição de prazo de entrega;
- simulação de risco de fraude;
- treinamento de modelos com Tribuo;
- projeto separado `ml-model-builder`;
- microsserviços carregando modelos prontos;
- separação entre treino e inferência.

## Objetivo da apostila

Explicar, de forma prática e conceitual, como aplicar Machine Learning em um sistema de microsserviços Java sem transformar os microsserviços em pipelines de treinamento.

Fluxo geral:

```text
Dados históricos
  ↓
ETL
  ↓
Dataset processado
  ↓
ml-model-builder
  ↓
Modelo treinado
  ↓
Microsserviço carrega modelo
  ↓
Predição em runtime
```

## Capítulos

| Capítulo | Arquivo |
|---|---|
| 01 | `01-visao-geral-ia-ml.md` |
| 02 | `02-tribuo-conceitos.md` |
| 03 | `03-datasets-olist.md` |
| 04 | `04-delivery-estimator-ml.md` |
| 05 | `05-fraud-detector-ml.md` |
| 06 | `06-ml-model-builder.md` |
| 07 | `07-inferencia-nos-microsservicos.md` |
| 08 | `08-testes-operacao-e-debug.md` |
| 09 | `09-limitacoes-roadmap.md` |

## Tecnologias usadas

- Java 21;
- Quarkus;
- Tribuo;
- Python;
- Pandas;
- Docker Compose;
- PostgreSQL;
- RabbitMQ;
- KrakenD.

## Resultado final

Ao final desta etapa:

- o `delivery-estimator-api` usa um modelo Tribuo de regressão para estimar prazo de entrega;
- o `fraud-detector-api` usa um modelo Tribuo de classificação para classificar risco de fraude;
- o `ml-model-builder` treina os modelos separadamente;
- os microsserviços apenas carregam modelos prontos em runtime.


---

