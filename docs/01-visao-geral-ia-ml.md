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
