import { sleep } from 'k6';
import {
    deterministicUuid,
    expectStatus,
    idempotencyKey,
    iterationNumber,
    login,
    numberEnv,
    pauseSeconds,
    requestJson,
} from './lib/common.js';

const SCENARIO = 'STRESS-02';
const CHECKOUT_PATH = __ENV.CHECKOUT_PATH || '/api/v1/checkout';
const SPIKE_VUS = numberEnv('SPIKE_VUS', 200);

export const options = {
    scenarios: {
        checkout_spike: {
            executor: 'ramping-vus',
            startVUs: 5,
            stages: [
                { duration: '30s', target: 10 },
                { duration: '10s', target: SPIKE_VUS },
                { duration: '1m', target: SPIKE_VUS },
                { duration: '20s', target: 10 },
                { duration: '30s', target: 0 },
            ],
            gracefulRampDown: '15s',
        },
    },
    thresholds: {
        'phase3_operation_errors{scenario_id:STRESS-02}': ['rate<0.10'],
        'phase3_operation_duration{scenario_id:STRESS-02}': ['p(95)<5000'],
    },
};

export function setup() {
    return { token: login('client') };
}

export default function (data) {
    const sequence = iterationNumber();
    const paymentMethod = sequence % 2 === 0 ? 'CREDIT_30_DAYS' : 'PAYKLIK';
    const tags = { scenario_id: SCENARIO, operation: 'checkout-spike', payment_method: paymentMethod };
    const response = requestJson('POST', CHECKOUT_PATH, data.token, {
        requestId: deterministicUuid(sequence, 500000),
        customerId: deterministicUuid((sequence % numberEnv('CUSTOMERS', 500)) + 1, 100000),
        amount: 750 + (sequence % 50) * 20,
        currency: 'GTQ',
        paymentMethod,
    }, tags, {
        'Idempotency-Key': idempotencyKey('spike-checkout', sequence),
        'X-Actor': 'CUSTOMER',
    }, [200, 201, 202, 409, 422]);
    expectStatus(response, [200, 201, 202, 409, 422], SCENARIO, tags);
    sleep(pauseSeconds());
}

