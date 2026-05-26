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
