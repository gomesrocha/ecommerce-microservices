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
