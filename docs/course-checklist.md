# Checklist Final do Projeto — Ecommerce Microservices

## Ambiente

- [ ] Docker instalado.
- [ ] Docker Compose instalado.
- [ ] Java 21 instalado.
- [ ] Maven disponível.
- [ ] jq instalado.
- [ ] GitHub CLI instalado.

## Infraestrutura local

- [ ] PostgreSQL sobe corretamente.
- [ ] RabbitMQ sobe corretamente.
- [ ] Mailpit sobe corretamente.
- [ ] WireMock sobe corretamente.
- [ ] Prometheus sobe corretamente.
- [ ] Grafana sobe corretamente.
- [ ] Tempo sobe corretamente.
- [ ] Loki sobe corretamente.
- [ ] Alloy sobe corretamente.
- [ ] API Gateway sobe corretamente.

## Serviços

- [ ] user-api está UP.
- [ ] product-api está UP.
- [ ] order-api está UP.
- [ ] delivery-estimator-api está UP.
- [ ] fraud-detector-api está UP.
- [ ] payment-service está UP.
- [ ] notification-service está UP.
- [ ] ai-chat-bff está UP.

## Segurança

- [ ] Login retorna JWT.
- [ ] Endpoint protegido sem token retorna 401.
- [ ] KrakenD valida token com JWKS.
- [ ] `X-User-Id` é propagado para serviços internos.
- [ ] Serviços internos não precisam expor porta pública.

## Produto

- [ ] Produto pode ser criado.
- [ ] Produto pode ser consultado.
- [ ] Estoque é exibido corretamente.
- [ ] Produto possui `originState`.

## Pedido e Saga

- [ ] Pedido pode ser criado.
- [ ] Pedido inicia em `WAITING_STOCK`.
- [ ] Estoque é reservado.
- [ ] Fraude é analisada.
- [ ] Pagamento é aprovado.
- [ ] Notificação é enviada.
- [ ] Pedido chega em `CONFIRMED`.
- [ ] Histórico do pedido pode ser consultado.

## Outbox

- [ ] Eventos são gravados em `orders.outbox_events`.
- [ ] Eventos são publicados.
- [ ] Eventos mudam para `PUBLISHED`.
- [ ] Eventos mantêm `correlation_id`.

## RabbitMQ

- [ ] Exchange `ecommerce.events` existe.
- [ ] Filas principais existem.
- [ ] Bindings estão corretos.
- [ ] DLQs existem.
- [ ] Plugin Prometheus está habilitado, se usado.
- [ ] Métricas RabbitMQ aparecem no Prometheus.

## Delivery ML

- [ ] CSV de treino existe.
- [ ] Modelo `delivery-tribuo-v1.model` existe.
- [ ] Relatório de treino existe.
- [ ] API usa `TRIBUO_MODEL`.
- [ ] Endpoint `/delivery-estimates/estimate` responde.

## Fraud ML

- [ ] CSV de treino existe.
- [ ] Modelo `fraud-tribuo-v1.model` existe.
- [ ] Relatório de treino existe.
- [ ] Métricas do relatório aparecem corretamente.
- [ ] API analisa eventos de pedido.

## MLOps

- [ ] `ml-model-builder` compila.
- [ ] `train-delivery` funciona.
- [ ] `train-fraud` funciona.
- [ ] `train-all` funciona.
- [ ] `training-summary.json` é gerado.
- [ ] Relatórios Markdown são gerados.

## AI Chat BFF

- [ ] Chat responde consultas gerais.
- [ ] Chat lista produtos.
- [ ] Chat consulta produto por ID.
- [ ] Chat consulta pedido por ID.
- [ ] Chat estima entrega.
- [ ] Chat gera prévia de pedido sem criar.
- [ ] Chat cria pedido apenas com confirmação explícita.
- [ ] Chat bloqueia desconto, frete grátis e alteração de regra.
- [ ] Chat propaga correlationId.

## Observabilidade

- [ ] Prometheus possui targets UP.
- [ ] Grafana exibe dashboard APM.
- [ ] Loki recebe logs.
- [ ] Alloy coleta logs dos containers.
- [ ] Tempo recebe traces.
- [ ] Busca por correlationId funciona.
- [ ] Logs de erro aparecem no dashboard.
- [ ] Métricas HTTP aparecem.
- [ ] Métricas JVM aparecem.
- [ ] Métricas RabbitMQ aparecem, se configuradas.

## Performance

- [ ] Script k6 existe.
- [ ] Teste k6 executa via Docker.
- [ ] Checks k6 acima de 95%.
- [ ] Falhas HTTP abaixo de 5%.
- [ ] p95 abaixo do threshold configurado.
- [ ] Dashboard mostra impacto do teste.

## Documentação

- [ ] Apostila final criada.
- [ ] Laboratórios práticos criados.
- [ ] Checklist final criado.
- [ ] Roteiro de demonstração criado.
- [ ] README principal atualizado.
- [ ] Issue #48 fechada.
