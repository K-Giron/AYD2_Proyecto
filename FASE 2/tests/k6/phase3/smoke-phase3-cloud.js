import http from 'k6/http';
import {
    BASE_URL,
    deterministicUuid,
    expectBody,
    expectStatus,
    idempotencyKey,
    jsonOrNull,
    login,
    pathFromTemplate,
    requestJson,
} from './lib/common.js';

const CREDIT_PATH = __ENV.CREDIT_OPTIONS_PATH_TEMPLATE
    || '/api/v1/credit/eligibility?customerId={customerId}&amount={amount}';
const CHECKOUT_PATH = __ENV.CHECKOUT_PATH || '/api/v1/checkout';
const ORDER_PATH = __ENV.ORDER_PATH_TEMPLATE || '/api/v1/orders/{orderId}';
const READINESS_PATH = __ENV.READINESS_PATH || '/actuator/health/readiness';

export const options = {
    vus: 1,
    iterations: 1,
    thresholds: {
        phase3_operation_errors: ['rate==0'],
        http_req_failed: ['rate==0'],
    },
};

export function setup() {
    return { token: login('client') };
}

export default function (data) {
    const readiness = http.get(`${BASE_URL}${READINESS_PATH}`, {
        tags: { scenario_id: 'SMOKE-CLOUD', operation: 'readiness' },
    });
    expectStatus(readiness, [200], 'SMOKE readiness', { scenario_id: 'SMOKE-CLOUD' });
    expectBody(readiness, (candidate) => {
        const body = jsonOrNull(candidate);
        return body !== null && body.status === 'UP';
    }, 'SMOKE readiness', { scenario_id: 'SMOKE-CLOUD' });

    const customerId = __ENV.SMOKE_CUSTOMER_ID || deterministicUuid(1, 100000);
    const eligibility = requestJson('GET', pathFromTemplate(CREDIT_PATH, {
        customerId,
        amount: 1000,
    }), data.token, null, { scenario_id: 'SMOKE-CLOUD', operation: 'eligibility' });
    expectStatus(eligibility, [200], 'SMOKE elegibilidad', { scenario_id: 'SMOKE-CLOUD' });

    const checkoutKey = idempotencyKey('smoke-cloud', 1);
    const checkout = requestJson('POST', CHECKOUT_PATH, data.token, {
        requestId: deterministicUuid(1, 900000),
        customerId,
        amount: 1000,
        currency: 'GTQ',
        paymentMethod: __ENV.SMOKE_PAYMENT_METHOD || 'PAYKLIK',
    }, { scenario_id: 'SMOKE-CLOUD', operation: 'checkout' }, {
        'Idempotency-Key': checkoutKey,
        'X-Actor': 'CUSTOMER',
    });
    expectStatus(checkout, [200, 201, 202], 'SMOKE checkout', { scenario_id: 'SMOKE-CLOUD' });

    const checkoutBody = jsonOrNull(checkout) || {};
    const orderId = __ENV.SMOKE_ORDER_ID || checkoutBody.orderId || checkoutBody.id;
    if (!orderId) {
        throw new Error('SMOKE checkout no devolvió orderId/id y SMOKE_ORDER_ID no fue definido');
    }
    const order = requestJson('GET', pathFromTemplate(ORDER_PATH, { orderId }), data.token, null,
        { scenario_id: 'SMOKE-CLOUD', operation: 'order-query' });
    expectStatus(order, [200], 'SMOKE consulta de orden', { scenario_id: 'SMOKE-CLOUD' });
}

