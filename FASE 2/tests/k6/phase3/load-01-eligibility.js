import { sleep } from 'k6';
import {
    deterministicUuid,
    expectBody,
    expectStatus,
    iterationNumber,
    jsonOrNull,
    login,
    numberEnv,
    pathFromTemplate,
    pauseSeconds,
    requestJson,
} from './lib/common.js';

const SCENARIO = 'LOAD-01';
const PATH_TEMPLATE = __ENV.CREDIT_OPTIONS_PATH_TEMPLATE
    || '/api/v1/credit/eligibility?customerId={customerId}&amount={amount}';

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
    return { token: login('client') };
}

export default function (data) {
    const sequence = iterationNumber();
    const customerId = deterministicUuid((sequence % numberEnv('CUSTOMERS', 500)) + 1, 100000);
    const amount = 250 + (sequence % 20) * 50;
    const path = pathFromTemplate(PATH_TEMPLATE, { customerId, amount });
    const tags = { scenario_id: SCENARIO, operation: 'credit-options' };
    const response = requestJson('GET', path, data.token, null, tags);

    if (expectStatus(response, [200], SCENARIO, tags)) {
        expectBody(response, (candidate) => {
            const body = jsonOrNull(candidate);
            return body !== null && (
                typeof body.eligible === 'boolean'
                || ['APPROVED', 'REJECTED'].includes(body.decision)
                || Array.isArray(body.paymentOptions)
            );
        }, SCENARIO, tags);
    }
    sleep(pauseSeconds());
}

