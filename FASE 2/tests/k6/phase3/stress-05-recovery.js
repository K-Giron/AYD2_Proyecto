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
    requestJson,
    requireFlag,
} from './lib/common.js';

const SCENARIO = 'STRESS-05';
const READINESS_PATH = __ENV.READINESS_PATH || '/actuator/health/readiness';
const MAX_VUS = numberEnv('STRESS_MAX_VUS', 160);
let cartId = null;

export const options = {
    scenarios: {
        overload: {
            executor: 'ramping-vus',
            exec: 'overload',
            startVUs: 1,
            stages: [
                { duration: '30s', target: Math.ceil(MAX_VUS * 0.50) },
                { duration: '1m', target: MAX_VUS },
                { duration: '2m', target: MAX_VUS },
                { duration: '30s', target: 0 },
            ],
        },
        recovery_window: {
            executor: 'constant-vus',
            exec: 'recoveryWindow',
            startTime: '4m15s',
            vus: 5,
            duration: '2m',
        },
    },
    thresholds: {
        'phase3_operation_errors{phase:recovery}': ['rate<0.01'],
        'phase3_operation_duration{phase:recovery}': ['p(95)<1000', 'p(99)<2000'],
    },
};

export function setup() {
    requireFlag('ALLOW_MUTATIONS', 'STRESS-05 crea carritos sintéticos.');
    requireFlag('ALLOW_STRESS', 'STRESS-05 sobrecarga el evaluador y mide recuperación.');
    return { token: login('support') };
}

function ensureCart(token) {
    if (cartId) return cartId;
    const sequence = customerSequence(__VU);
    const cart = createCartWithItem(token, customerIdFor(sequence), sequence + 700000,
        { scenario_id: `${SCENARIO}-SETUP` });
    cartId = cart?.cartId || null;
    return cartId;
}

export function overload(data) {
    const selectedCart = ensureCart(data.token);
    if (!selectedCart) return;
    const tags = { scenario_id: SCENARIO, operation: 'recovery-overload', phase: 'overload' };
    const response = requestJson('GET', `/api/v1/carts/${selectedCart}/payment-options`,
        data.token, null, tags);
    expectStatus(response, [200], `${SCENARIO} sobrecarga`, tags);
}

export function recoveryWindow(data) {
    const selectedCart = ensureCart(data.token);
    if (!selectedCart) return;
    const healthTags = { scenario_id: SCENARIO, operation: 'readiness-after-load', phase: 'recovery' };
    const health = requestJson('GET', READINESS_PATH, data.token, null, healthTags);
    if (expectStatus(health, [200], `${SCENARIO} health`, healthTags)) {
        expectBody(health, (candidate) => {
            const body = jsonOrNull(candidate);
            return body !== null && body.status === 'UP';
        }, `${SCENARIO} health`, healthTags);
    }

    const creditTags = { scenario_id: SCENARIO, operation: 'eligibility-after-load', phase: 'recovery' };
    const credit = requestJson('GET', `/api/v1/carts/${selectedCart}/payment-options`,
        data.token, null, creditTags);
    if (expectStatus(credit, [200], `${SCENARIO} elegibilidad`, creditTags)) {
        expectBody(credit, (candidate) => Array.isArray(jsonOrNull(candidate)?.options),
            `${SCENARIO} elegibilidad`, creditTags);
    }
    sleep(1);
}

