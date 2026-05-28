#!/usr/bin/env bash

set -euo pipefail

RABBITMQ_CONTAINER="${RABBITMQ_CONTAINER:-ecommerce-rabbitmq}"
RABBITMQ_USER="${RABBITMQ_USER:-ecommerce}"
RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-ecommerce}"

echo "Configurando DLX ecommerce.dlx..."

docker exec -i "$RABBITMQ_CONTAINER" rabbitmqadmin \
  --username="$RABBITMQ_USER" \
  --password="$RABBITMQ_PASSWORD" \
  declare exchange \
  name=ecommerce.dlx \
  type=topic \
  durable=true

declare -a QUEUES=(
  "product.stock-reservation.dlq"
  "order.stock-reserved.dlq"
  "order.stock-rejected.dlq"
  "fraud.order-created.dlq"
  "order.fraud-approved.dlq"
  "order.fraud-rejected.dlq"
  "payment.requested.dlq"
  "order.payment-approved.dlq"
  "order.payment-rejected.dlq"
  "notifications.requested.dlq"
)

for queue in "${QUEUES[@]}"; do
  echo "Criando fila DLQ: $queue"

  docker exec -i "$RABBITMQ_CONTAINER" rabbitmqadmin \
    --username="$RABBITMQ_USER" \
    --password="$RABBITMQ_PASSWORD" \
    declare queue \
    name="$queue" \
    durable=true
done

declare -A BINDINGS=(
  ["product.stock-reservation.dlq"]="product.stock.reserve"
  ["order.stock-reserved.dlq"]="stock.reserved"
  ["order.stock-rejected.dlq"]="stock.rejected"
  ["fraud.order-created.dlq"]="order.created"
  ["order.fraud-approved.dlq"]="fraud.approved"
  ["order.fraud-rejected.dlq"]="fraud.rejected"
  ["payment.requested.dlq"]="payment.requested"
  ["order.payment-approved.dlq"]="payment.approved"
  ["order.payment-rejected.dlq"]="payment.rejected"
  ["notifications.requested.dlq"]="notifications.requested"
)

for queue in "${!BINDINGS[@]}"; do
  routing_key="${BINDINGS[$queue]}"

  echo "Criando binding DLQ: ecommerce.dlx -> $queue [$routing_key]"

  docker exec -i "$RABBITMQ_CONTAINER" rabbitmqadmin \
    --username="$RABBITMQ_USER" \
    --password="$RABBITMQ_PASSWORD" \
    declare binding \
    source=ecommerce.dlx \
    destination_type=queue \
    destination="$queue" \
    routing_key="$routing_key"
done

echo "Aplicando policy ecommerce-dlx..."

docker exec -i "$RABBITMQ_CONTAINER" rabbitmqctl set_policy ecommerce-dlx \
  "^(product\.stock-reservation|order\.stock-reserved|order\.stock-rejected|fraud\.order-created|order\.fraud-approved|order\.fraud-rejected|payment\.requested|order\.payment-approved|order\.payment-rejected|notifications\.requested)$" \
  '{"dead-letter-exchange":"ecommerce.dlx"}' \
  --apply-to queues \
  --priority 10

echo "DLQ configurada com sucesso."
