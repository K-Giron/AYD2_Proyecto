import { sleep } from 'k6';
import {
    deterministicUuid,
    expectStatus,
    iterationNumber,
    login,
    numberEnv,
    pathFromTemplate,
    pauseSeconds,
    requestJson,
} from './lib/common.js';

const SCENARIO = 'STRESS-01';
const PATH_TEMPLATE = __ENV.CREDIT_OPTIONS_PATH_TEMPLATE
    || '/api/v1/credit/eligibility?customerId={customerId}&amount={amount}';
const MAX_VUS = numberEnv('STRESS_MAX_VUS', 160);

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
    return { token: login('client') };
}

export default function (data) {
    const sequence = iterationNumber();
    const path = pathFromTemplate(PATH_TEMPLATE, {
        customerId: deterministicUuid((sequence % numberEnv('CUSTOMERS', 500)) + 1, 100000),
        amount: 500 + (sequence % 50) * 25,
    });
    const tags = { scenario_id: SCENARIO, operation: 'progressive-credit-pressure' };
    const response = requestJson('GET', path, data.token, null, tags);
    expectStatus(response, [200], SCENARIO, tags);
    sleep(pauseSeconds());
}

