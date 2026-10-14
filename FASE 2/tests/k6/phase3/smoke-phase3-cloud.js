import http from 'k6/http';
import { sleep } from 'k6';
import {
    BASE_URL,
    checkoutCart,
    createCartWithItem,
    expectBody,
    expectStatus,
    jsonOrNull,
    loginSession,
    numberEnv,
    pathFromTemplate,
    requestJson,
    requireFlag,
    runSaga,
} from './lib/common.js';

const ORDER_PATH = __ENV.ORDER_PATH_TEMPLATE || '/api/v1/orders/{orderId}';
// Producción no publica Actuator: nginx expone únicamente las rutas /api/.
// Este endpoint entrega el mismo estado UP sin abrir la administración interna.
const READINESS_PATH = __ENV.READINESS_PATH || '/api/v1/platform/status';

export const options = {
    vus: 1,
    iterations: 1,
    thresholds: {
        phase3_operation_errors: ['rate==0'],
        http_req_failed: ['rate==0'],
    },
};

export function setup() {
    requireFlag('ALLOW_MUTATIONS', 'El smoke crea una orden real en el ambiente objetivo.');
    const session = loginSession('client');
    if (!session.customerId) {
        throw new Error('La cuenta CLIENT no devolvió customerId firmado.');
    }
    return session;
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

    const sequence = 900001;
    const cart = createCartWithItem(data.token, data.customerId, sequence,
        { scenario_id: 'SMOKE-CLOUD', operation: 'cart' });
    if (!cart) throw new Error('SMOKE no pudo preparar el carrito.');

    const options = requestJson('GET', `/api/v1/carts/${cart.cartId}/payment-options`,
        data.token, null, { scenario_id: 'SMOKE-CLOUD', operation: 'payment-options' });
    if (expectStatus(options, [200], 'SMOKE opciones', { scenario_id: 'SMOKE-CLOUD' })) {
        expectBody(options, (candidate) => {
            const body = jsonOrNull(candidate);
            return Array.isArray(body?.options)
                && body.options.some((option) => option.route === 'PAYKLIK' && option.enabled === true);
        }, 'SMOKE opciones', { scenario_id: 'SMOKE-CLOUD' });
    }

    const checkout = checkoutCart(data.token, cart.cartId, sequence,
        { scenario_id: 'SMOKE-CLOUD', operation: 'checkout' });
    expectStatus(checkout, [201], 'SMOKE checkout', { scenario_id: 'SMOKE-CLOUD' });

    const checkoutBody = jsonOrNull(checkout) || {};
    const orderId = checkoutBody.id;
    if (!orderId) {
        throw new Error('SMOKE checkout no devolvió id de orden.');
    }

    const paymentRoute = __ENV.SMOKE_PAYMENT_ROUTE || 'PAYKLIK';
    let sagaResponse;
    let sagaBody;
    for (let attempt = 0; attempt < numberEnv('SAGA_MAX_POLLS', 12); attempt += 1) {
        sagaResponse = runSaga(data.token, orderId, paymentRoute, sequence,
            { scenario_id: 'SMOKE-CLOUD', operation: 'saga', payment_route: paymentRoute });
        if (!expectStatus(sagaResponse, [200], 'SMOKE Saga', { scenario_id: 'SMOKE-CLOUD' })) break;
        sagaBody = jsonOrNull(sagaResponse);
        if (!['WAITING_FISCAL', 'WAITING_PAYMENT'].includes(sagaBody?.sagaStatus)) break;
        sleep(0.5);
    }
    expectBody(sagaResponse, () =>
        ['COMPLETED', 'CREDIT_REJECTED'].includes(sagaBody?.sagaStatus),
    'SMOKE Saga terminal', { scenario_id: 'SMOKE-CLOUD' });

    const order = requestJson('GET', pathFromTemplate(ORDER_PATH, { orderId }), data.token, null,
        { scenario_id: 'SMOKE-CLOUD', operation: 'order-query' });
    expectStatus(order, [200], 'SMOKE consulta de orden', { scenario_id: 'SMOKE-CLOUD' });
}

