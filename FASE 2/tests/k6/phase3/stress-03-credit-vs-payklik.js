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

const SCENARIO = 'STRESS-03';
const MAX_VUS = numberEnv('STRESS_MAX_VUS', 160);
let creditCartId = null;
let controlCartId = null;

export const options = {
    scenarios: {
        credit_pressure: {
            executor: 'ramping-vus',
            exec: 'creditPressure',
            startVUs: 1,
            stages: [
                { duration: '30s', target: Math.ceil(MAX_VUS * 0.25) },
                { duration: '1m', target: MAX_VUS },
                { duration: '2m', target: MAX_VUS },
                { duration: '30s', target: 0 },
            ],
        },
        payklik_control: {
            executor: 'constant-vus',
            exec: 'payKlikControl',
            vus: numberEnv('PAYKLIK_CONTROL_VUS', 10),
            duration: '4m',
        },
    },
    thresholds: {
        'phase3_operation_errors{operation:payklik-control}': ['rate<0.01'],
        'phase3_operation_duration{operation:payklik-control}': ['p(95)<1500', 'p(99)<3000'],
        'phase3_operation_errors{operation:credit-pressure}': ['rate<0.10'],
    },
};

export function setup() {
    requireFlag('ALLOW_MUTATIONS', 'STRESS-03 crea carritos sintéticos.');
    requireFlag('ALLOW_STRESS', 'STRESS-03 satura el evaluador de crédito.');
    return { token: login('support') };
}

export function creditPressure(data) {
    const sequence = customerSequence(__VU);
    if (!creditCartId) {
        const cart = createCartWithItem(data.token, customerIdFor(sequence), sequence,
            { scenario_id: `${SCENARIO}-SETUP`, workload: 'credit-pressure' });
        if (!cart) return;
        creditCartId = cart.cartId;
    }
    const tags = { scenario_id: SCENARIO, operation: 'credit-pressure' };
    const response = requestJson('GET', `/api/v1/carts/${creditCartId}/payment-options`,
        data.token, null, tags);
    if (expectStatus(response, [200], `${SCENARIO} crédito`, tags)) {
        expectBody(response, (candidate) => Array.isArray(jsonOrNull(candidate)?.options),
            `${SCENARIO} crédito`, tags);
    }
    sleep(pauseSeconds());
}

export function payKlikControl(data) {
    const sequence = customerSequence(__VU + Math.floor(numberEnv('CUSTOMERS', 500) / 2));
    if (!controlCartId) {
        const cart = createCartWithItem(data.token, customerIdFor(sequence), sequence + 600000,
            { scenario_id: `${SCENARIO}-SETUP`, workload: 'payklik-control' });
        if (!cart) return;
        controlCartId = cart.cartId;
    }
    const tags = { scenario_id: SCENARIO, operation: 'payklik-control', payment_route: 'PAYKLIK' };
    const response = requestJson('GET', `/api/v1/carts/${controlCartId}/payment-options`,
        data.token, null, tags);
    if (expectStatus(response, [200], `${SCENARIO} PayKlik`, tags)) {
        expectBody(response, (candidate) => {
            const body = jsonOrNull(candidate);
            return Array.isArray(body?.options)
                && body.options.some((option) => option.route === 'PAYKLIK' && option.enabled === true);
        }, `${SCENARIO} PayKlik`, tags);
    }
    sleep(pauseSeconds());
}

