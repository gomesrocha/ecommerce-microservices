# Relatório de Treinamento - fraud-tribuo-v2

## Identificação

| Campo | Valor |
|---|---|
| Modelo | fraud-tribuo |
| Versão | fraud-tribuo-v2 |
| Domínio | FRAUD |
| Algoritmo | Tribuo CART Classification |
| Framework | Tribuo |
| Status | ACTIVE |
| Treinado em | 2026-06-16T19:45:42.090137674Z |
| Dataset | kaggle_fraud_training_tribuo.csv |
| Arquivo de entrada | `../data/fraud/kaggle/processed/kaggle_fraud_training_tribuo.csv` |
| Modelo gerado | `../models/fraud/fraud-tribuo-v2.model` |

## Dados

| Métrica | Valor |
|---|---:|
| Registros de treino | 38604 |
| Registros de teste | 9651 |
| Total de registros | 48255 |

## Features

- `total_amount`
- `log_total_amount`
- `items_quantity`
- `log_items_quantity`
- `avg_item_price`
- `log_avg_item_price`
- `max_item_price`
- `log_max_item_price`
- `unique_products`
- `amount_per_item`
- `price_spread`
- `max_to_avg_price_ratio`
- `transaction_hour`
- `transaction_day_of_week`
- `transaction_is_weekend`
- `amount_low`
- `amount_medium`
- `amount_high`
- `amount_very_high`
- `items_single`
- `items_few`
- `items_many`
- `hour_night`
- `hour_morning`
- `hour_afternoon`
- `hour_evening`

Target: `label`

## Métricas

| Métrica | Valor |
|---|---:|
| accuracy | 0.950056988913066 |
| precision | 0.8942708333333333 |
| recall | 0.8602204408817635 |
| f1 | 0.8769152196118488 |
| tp | 1717 |
| fp | 203 |
| fn | 279 |
| tn | 7452 |

## Observações

Modelo de classificação de risco de fraude treinado com dataset público Kaggle Credit Card Transactions Fraud Detection, adaptado para um schema canônico compatível com OrderCreatedEvent. As métricas usam FRAUD_RISK como classe positiva.
