import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

export const options = {
  scenarios: {
    order_flow_smoke: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 2 },
        { duration: '1m', target: 5 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
    checks: ['rate>0.95'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8099';

const createdOrders = new Counter('created_orders');
const orderCreationDuration = new Trend('order_creation_duration');

function login() {
  const response = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({
      username: __ENV.USERNAME || 'admin',
      password: __ENV.PASSWORD || 'admin123',
    }),
    {
      headers: {
        'Content-Type': 'application/json',
      },
      tags: {
        name: 'POST /api/auth/login',
      },
    }
  );

  check(response, {
    'login status is 200': (r) => r.status === 200,
    'login has access token': (r) => Boolean(r.json('access_token')),
  });

  return response.json('access_token');
}

function createProduct(token) {
  const sku = `K6-PRODUCT-${__VU}-${__ITER}-${Date.now()}`;

  const response = http.post(
    `${BASE_URL}/api/products`,
    JSON.stringify({
      name: 'Produto k6 Performance',
      description: 'Produto criado pelo teste de performance k6',
      sku,
      price: 100.0,
      stockQuantity: 100,
      originState: 'SP',
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      tags: {
        name: 'POST /api/products',
      },
    }
  );

  check(response, {
    'create product status is 201 or 200': (r) => r.status === 201 || r.status === 200,
    'create product has id': (r) => Boolean(r.json('id')),
  });

  return response.json('id');
}

function createOrder(token, productId) {
  const correlationId = `k6-order-flow-${__VU}-${__ITER}-${Date.now()}`;

  const startedAt = Date.now();

  const response = http.post(
    `${BASE_URL}/api/orders`,
    JSON.stringify({
      userId: 1,
      customerState: 'SE',
      items: [
        {
          productId,
          quantity: 1,
        },
      ],
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
        'X-Correlation-Id': correlationId,
      },
      tags: {
        name: 'POST /api/orders',
      },
    }
  );

  orderCreationDuration.add(Date.now() - startedAt);

  check(response, {
    'create order status is 201 or 200': (r) => r.status === 201 || r.status === 200,
    'create order has id': (r) => Boolean(r.json('id')),
    'create order uses delivery model': (r) => r.json('deliverySource') === 'TRIBUO_MODEL',
  });

  if (response.status === 201 || response.status === 200) {
    createdOrders.add(1);
  }

  return {
    orderId: response.json('id'),
    correlationId,
  };
}

function getOrder(token, orderId) {
  const response = http.get(
    `${BASE_URL}/api/orders/${orderId}`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
      tags: {
        name: 'GET /api/orders/{id}',
      },
    }
  );

  check(response, {
    'get order status is 200': (r) => r.status === 200,
    'get order has id': (r) => Boolean(r.json('id')),
  });

  return response;
}

export default function () {
  const token = login();

  if (!token) {
    return;
  }

  const productId = createProduct(token);

  if (!productId) {
    return;
  }

  const order = createOrder(token, productId);

  if (order.orderId) {
    sleep(2);
    getOrder(token, order.orderId);
  }

  sleep(1);
}
