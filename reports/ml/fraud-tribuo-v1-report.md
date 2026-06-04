# Relatório de Treinamento - fraud-tribuo-v1

## Identificação

| Campo | Valor |
|---|---|
| Modelo | fraud-tribuo |
| Versão | fraud-tribuo-v1 |
| Domínio | FRAUD |
| Algoritmo | Tribuo CART Classification |
| Framework | Tribuo |
| Status | ACTIVE |
| Treinado em | 2026-06-03T17:51:30.902239776Z |
| Dataset | olist_fraud_risk_training_tribuo.csv |
| Arquivo de entrada | `../data/fraud/olist/processed/olist_fraud_risk_training_tribuo.csv` |
| Modelo gerado | `../models/fraud/fraud-tribuo-v1.model` |

## Dados

| Métrica | Valor |
|---|---:|
| Registros de treino | 64000 |
| Registros de teste | 16000 |
| Total de registros | 80000 |

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
- `same_state`
- `same_region`
- `is_interstate`
- `route_distance_km`
- `distance_local`
- `distance_short`
- `distance_medium`
- `distance_long`
- `distance_very_long`
- `amount_low`
- `amount_medium`
- `amount_high`
- `amount_very_high`
- `items_single`
- `items_few`
- `items_many`
- `origin_region_NORTE`
- `origin_region_NORDESTE`
- `origin_region_CENTRO_OESTE`
- `origin_region_SUDESTE`
- `origin_region_SUL`
- `destination_region_NORTE`
- `destination_region_NORDESTE`
- `destination_region_CENTRO_OESTE`
- `destination_region_SUDESTE`
- `destination_region_SUL`
- `origin_AC`
- `origin_AL`
- `origin_AM`
- `origin_AP`
- `origin_BA`
- `origin_CE`
- `origin_DF`
- `origin_ES`
- `origin_GO`
- `origin_MA`
- `origin_MG`
- `origin_MS`
- `origin_MT`
- `origin_PA`
- `origin_PB`
- `origin_PE`
- `origin_PI`
- `origin_PR`
- `origin_RJ`
- `origin_RN`
- `origin_RO`
- `origin_RR`
- `origin_RS`
- `origin_SC`
- `origin_SE`
- `origin_SP`
- `origin_TO`
- `destination_AC`
- `destination_AL`
- `destination_AM`
- `destination_AP`
- `destination_BA`
- `destination_CE`
- `destination_DF`
- `destination_ES`
- `destination_GO`
- `destination_MA`
- `destination_MG`
- `destination_MS`
- `destination_MT`
- `destination_PA`
- `destination_PB`
- `destination_PE`
- `destination_PI`
- `destination_PR`
- `destination_RJ`
- `destination_RN`
- `destination_RO`
- `destination_RR`
- `destination_RS`
- `destination_SC`
- `destination_SE`
- `destination_SP`
- `destination_TO`

Target: `label`

## Métricas

| Métrica | Valor |
|---|---:|
| accuracy | 0.998875 |
| precision | 0.997090203685742 |
| recall | 0.9941972920696325 |
| f1 | 0.9956416464891041 |
| tp | 2056 |
| fp | 6 |
| fn | 12 |
| tn | 13926 |

## Observações

Modelo de classificação de risco de fraude usando dataset sintético baseado em atributos do ecommerce e da base Olist. As métricas usam FRAUD_RISK como classe positiva.
