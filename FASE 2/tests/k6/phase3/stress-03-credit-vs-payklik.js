import { sleep } from 'k6';
import {
    deterministicUuid,
    expectStatus,
    idempotencyKey,
    iterationNumber,
    login,
    numberEnv,
    pathFromTemplate,
    pauseSeconds,
    requestJson,
} from './lib/common.js';

const SCENARIO = 'STRESS-03';
const CREDIT_PATH = __ENV.CREDIT_OPTIONS_PATH_TEMPLATE
    || '/api/v1/credit/eligibility?customerId={customerId}&amount={amount}';
const CHECKOUT_PATH = __ENV.CHECKOUT_PATH || '/api/v1/checkout';
const MAX_VUS = numberEnv('STRESS_MAX_VUS', 160);

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
    return { token: login('client') };
}

export function creditPressure(data) {
    const sequence = iterationNumber();
    const path = pathFromTemplate(CREDIT_PATH, {
        customerId: deterministicUuid((sequence % numberEnv('CUSTOMERS', 500)) + 1, 100000),
        amount: 1000 + (sequence % 30) * 50,
    });
    const tags = { scenario_id: SCENARIO, operation: 'credit-pressure' };
    const response = requestJson('GET', path, data.token, null, tags);
    expectStatus(response, [200], `${SCENARIO} crédito`, tags);
    sleep(pauseSeconds());
}

export function payKlikControl(data) {
    const sequence = iterationNumber(600000);
    const tags = { scenario_id: SCENARIO, operation: 'payklik-control', payment_method: 'PAYKLIK' };
    const response = requestJson('POST', CHECKOUT_PATH, data.token, {
        requestId: deterministicUuid(sequence, 600000),
        customerId: deterministicUuid((sequence % numberEnv('CUSTOMERS', 500)) + 1, 100000),
        amount: 900 + (sequence % 10) * 50,
        currency: 'GTQ',
        paymentMethod: 'PAYKLIK',
    }, tags, {
        'Idempotency-Key': idempotencyKey('payklik-control', sequence),
        'X-Actor': 'CUSTOMER',
    });
    expectStatus(response, [200, 201, 202], `${SCENARIO} PayKlik`, tags);
    sleep(pauseSeconds());
}

