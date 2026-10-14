import { sleep } from 'k6';
import {
    deterministicUuid,
    expectBody,
    expectStatus,
    idempotencyKey,
    iterationNumber,
    jsonOrNull,
    login,
    numberEnv,
    pauseSeconds,
    requestJson,
} from './lib/common.js';

const SCENARIO = 'LOAD-02';
const CHECKOUT_PATH = __ENV.CHECKOUT_PATH || '/api/v1/checkout';

export const options = {
    scenarios: {
        expected_mixed_checkout: {
            executor: 'constant-vus',
            vus: numberEnv('VUS', 10),
            duration: __ENV.DURATION || '2m',
        },
    },
    thresholds: {
        'phase3_operation_errors{scenario_id:LOAD-02}': ['rate<0.01'],
        'phase3_operation_duration{scenario_id:LOAD-02}': ['p(95)<1500', 'p(99)<3000'],
    },
};

export function setup() {
    return { token: login('client') };
}

export default function (data) {
    const sequence = iterationNumber();
    const paymentMethod = sequence % 2 === 0 ? 'CREDIT_30_DAYS' : 'PAYKLIK';
    const requestKey = idempotencyKey('checkout', sequence);
    const tags = { scenario_id: SCENARIO, operation: 'mixed-checkout', payment_method: paymentMethod };
    const response = requestJson('POST', CHECKOUT_PATH, data.token, {
        requestId: deterministicUuid(sequence, 200000),
        customerId: deterministicUuid((sequence % numberEnv('CUSTOMERS', 500)) + 1, 100000),
        amount: 500 + (sequence % 30) * 25,
        currency: 'GTQ',
        paymentMethod,
    }, tags, { 'Idempotency-Key': requestKey, 'X-Actor': 'CUSTOMER' },
    [200, 201, 202, 409, 422]);

    if (expectStatus(response, [200, 201, 202, 409, 422], SCENARIO, tags)) {
        expectBody(response, (candidate) => jsonOrNull(candidate) !== null, SCENARIO, tags);
    }
    sleep(pauseSeconds());
}

