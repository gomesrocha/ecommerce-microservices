# Observabilidade com Loki e Grafana Alloy

O projeto usa Grafana Loki para armazenamento de logs e Grafana Alloy para coleta dos logs dos containers Docker.

## Componentes

- Grafana: visualização
- Loki: armazenamento e consulta de logs
- Alloy: coleta de logs dos containers Docker
- Prometheus: métricas
- Tempo: traces

## Acessos

- Grafana: http://localhost:3000
- Loki: http://localhost:3100
- Alloy: http://localhost:12345

## Validar Loki

```bash
curl -i http://localhost:3100/ready
```
