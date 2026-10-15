import { sleep } from 'k6';
import {
    createCartWithItem,
    customerIdFor,
    customerSequence,
    expectBody,
    expectStatus,
    jsonOrNull,
    login,
    numberEnv,
    pauseSeconds,
    requestJson,
    requireFlag,
} from './lib/common.js';

const SCENARIO = 'LOAD-01';
let cartId = null;

export const options = {
    scenarios: {
        expected_eligibility_load: {
            executor: 'constant-vus',
            vus: numberEnv('VUS', 10),
            duration: __ENV.DURATION || '2m',
        },
    },
    thresholds: {
        'phase3_operation_errors{scenario_id:LOAD-01}': ['rate<0.01'],
        'phase3_operation_duration{scenario_id:LOAD-01}': ['p(95)<500', 'p(99)<1000'],
    },
};

export function setup() {
    requireFlag('ALLOW_MUTATIONS', 'LOAD-01 crea carritos sintéticos para evaluar opciones reales.');
    return { token: login('support') };
}

export default function (data) {
    const sequence = customerSequence(__VU);
    if (!cartId) {
        const cart = createCartWithItem(data.token, customerIdFor(sequence), sequence,
            { scenario_id: `${SCENARIO}-SETUP` });
        if (!cart) return;
        cartId = cart.cartId;
    }
    const tags = { scenario_id: SCENARIO, operation: 'credit-options' };
    const response = requestJson('GET', `/api/v1/carts/${cartId}/payment-options`,
        data.token, null, tags);

    if (expectStatus(response, [200], SCENARIO, tags)) {
        expectBody(response, (candidate) => {
            const body = jsonOrNull(candidate);
            return body !== null && body.cartId === cartId
                && Array.isArray(body.options)
                && body.options.some((option) => option.route === 'PAYKLIK' && option.enabled === true);
        }, SCENARIO, tags);
    }
    sleep(pauseSeconds());
}

