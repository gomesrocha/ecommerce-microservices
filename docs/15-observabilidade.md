# 15 — Observabilidade

## Objetivo

Observabilidade permite entender o comportamento do sistema em execução.

O projeto usa:

```text
OpenTelemetry Collector
Tempo
Prometheus
Grafana
```

## Pilares

### Traces

Mostram o caminho de uma requisição entre serviços.

Exemplo:

```text
KrakenD -> order-api -> product-api -> delivery-estimator-api
```

### Métricas

Valores numéricos coletados ao longo do tempo.

Exemplos:

```text
requisições por segundo
latência
erros HTTP
uso de memória
```

### Logs

Mensagens emitidas pelos serviços.

No projeto atual, logs ainda não estão centralizados com Loki.

## OpenTelemetry

OpenTelemetry é usado para instrumentação.

Cada serviço recebe variáveis:

```yaml
OTEL_ENABLED: "true"
OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4317
OTEL_SERVICE_NAME: order-api
```

## OpenTelemetry Collector

Recebe traces via OTLP.

Configuração:

```text
infra/observability/otel/otel-collector-config.yml
```

O Collector encaminha traces para o Tempo.

## Tempo

Backend de traces.

Configuração:

```text
infra/observability/tempo/tempo.yml
```

Porta:

```text
3200
```

## Prometheus

Coleta métricas.

Configuração:

```text
infra/observability/prometheus/prometheus.yml
```

Porta:

```text
9090
```

## Grafana

Interface visual.

Porta:

```text
3000
```

Login:

```text
admin / admin
```

## Datasources

Configurados em:

```text
infra/observability/grafana/provisioning/datasources/datasources.yml
```

Datasources:

```text
Prometheus
Tempo
```

## Métricas Quarkus

Os serviços usam Micrometer/Prometheus.

Endpoint interno:

```text
/q/metrics
```

Como os microsserviços não são expostos no host, o Prometheus acessa pela rede Docker.

## Traces

Para gerar traces:

```bash
curl http://localhost:8099/api/products
curl http://localhost:8099/api/orders
```

Depois acessar Grafana:

```text
Explore -> Tempo
```

## Problemas enfrentados

### Tempo com erro de permissão

Erro:

```text
mkdir /tmp/tempo/blocks: permission denied
```

Solução no Compose:

```yaml
user: "0:0"
```

### Collector com logging exporter depreciado

Erro:

```text
logging exporter has been deprecated
```

Solução:

```yaml
exporters:
  debug:
    verbosity: basic
```

## Próximos passos

- dashboards customizados;
- métricas de negócio;
- logs centralizados com Loki;
- correlationId;
- alertas;
- métricas de Outbox;
- métricas de Saga;
- métricas de fallback.
