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
