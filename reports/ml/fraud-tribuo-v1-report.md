# Relatório de Treinamento - fraud-tribuo-v1

## Identificação

| Campo | Valor |
|---|---|
| Modelo | fraud-tribuo |
| Versão | fraud-tribuo-v1 |
| Domínio | FRAUD |
| Algoritmo | Tribuo Classification |
| Framework | Tribuo |
| Status | ACTIVE |
| Treinado em | 2026-05-28T14:15:28.666674848Z |
| Dataset | olist_fraud_risk_training_tribuo.csv |
| Arquivo de entrada | `../data/fraud/olist/processed/olist_fraud_risk_training_tribuo.csv` |
| Modelo gerado | `../models/fraud/fraud-tribuo-v1.model` |

## Dados

| Métrica | Valor |
|---|---:|
| Registros de treino | 0 |
| Registros de teste | 0 |
| Total de registros | 80000 |

## Features

- `totalAmount`
- `totalItems`
- `customerState`
- `estimatedDeliveryDays`

Target: `riskLabel`

## Métricas

| Métrica | Valor |
|---|---:|
| accuracy | not_available |
| recall | not_available |
| f1 | not_available |
| precision | not_available |

## Observações

Modelo de classificação de risco de fraude usando dataset sintético baseado em atributos do ecommerce e da base Olist. Métricas detalhadas serão extraídas em uma evolução do FraudModelTrainer.
