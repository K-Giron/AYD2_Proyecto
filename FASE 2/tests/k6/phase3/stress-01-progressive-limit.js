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

const SCENARIO = 'STRESS-01';
const MAX_VUS = numberEnv('STRESS_MAX_VUS', 160);
let cartId = null;

export const options = {
    scenarios: {
        progressive_limit: {
            executor: 'ramping-vus',
            startVUs: 1,
            stages: [
                { duration: '30s', target: Math.ceil(MAX_VUS * 0.10) },
                { duration: '1m', target: Math.ceil(MAX_VUS * 0.25) },
                { duration: '1m', target: Math.ceil(MAX_VUS * 0.50) },
                { duration: '1m', target: Math.ceil(MAX_VUS * 0.75) },
                { duration: '1m', target: MAX_VUS },
                { duration: '30s', target: 0 },
            ],
            gracefulRampDown: '15s',
        },
    },
    thresholds: {
        'phase3_operation_errors{scenario_id:STRESS-01}': ['rate<0.10'],
        'phase3_operation_duration{scenario_id:STRESS-01}': ['p(95)<3000'],
    },
};

export function setup() {
    requireFlag('ALLOW_MUTATIONS', 'STRESS-01 crea carritos sintéticos.');
    requireFlag('ALLOW_STRESS', 'STRESS-01 genera una rampa de sobrecarga.');
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
    const tags = { scenario_id: SCENARIO, operation: 'progressive-credit-pressure' };
    const response = requestJson('GET', `/api/v1/carts/${cartId}/payment-options`,
        data.token, null, tags);
    if (expectStatus(response, [200], SCENARIO, tags)) {
        expectBody(response, (candidate) => Array.isArray(jsonOrNull(candidate)?.options), SCENARIO, tags);
    }
    sleep(pauseSeconds());
}

