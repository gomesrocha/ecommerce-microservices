# Relatório de Treinamento - delivery-tribuo-v1

## Identificação

| Campo | Valor |
|---|---|
| Modelo | delivery-tribuo |
| Versão | delivery-tribuo-v1 |
| Domínio | DELIVERY |
| Algoritmo | Tribuo CART Regression |
| Framework | Tribuo |
| Status | ACTIVE |
| Treinado em | 2026-06-03T12:39:19.000184947Z |
| Dataset | olist_delivery_training_tribuo.csv |
| Arquivo de entrada | `../delivery-estimator-api/src/main/resources/ml/olist_delivery_training_tribuo.csv` |
| Modelo gerado | `../models/delivery/delivery-tribuo-v1.model` |

## Dados

| Métrica | Valor |
|---|---:|
| Registros de treino | 77114 |
| Registros de teste | 19279 |
| Total de registros | 96393 |

## Features

- `items_quantity`
- `log_items_quantity`
- `same_state`
- `same_region`
- `is_interstate`
- `route_distance_km`
- `distance_local`
- `distance_short`
- `distance_medium`
- `distance_long`
- `distance_very_long`
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

Target: `delivery_days`

## Métricas

| Métrica | Valor |
|---|---:|
| rmse | 7.7333319066485116 |
| mae | 5.313625128899415 |
| r2 | 0.22959112074307886 |

## Observações

Modelo de regressão para estimativa de prazo de entrega usando dados derivados da base Olist.
