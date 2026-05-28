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
| Treinado em | 2026-05-28T14:15:25.684674017Z |
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
| rmse | 7.84791215767874 |
| mae | 5.413944182710921 |
| r2 | 0.20659260127041568 |

## Observações

Modelo de regressão para estimativa de prazo de entrega usando dados derivados da base Olist.
