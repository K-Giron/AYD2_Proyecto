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
    requestJson,
} from './lib/common.js';

const SCENARIO = 'STRESS-05';
const CREDIT_PATH = __ENV.CREDIT_OPTIONS_PATH_TEMPLATE
    || '/api/v1/credit/eligibility?customerId={customerId}&amount={amount}';
const READINESS_PATH = __ENV.READINESS_PATH || '/actuator/health/readiness';
const MAX_VUS = numberEnv('STRESS_MAX_VUS', 160);

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
    return { token: login('client') };
}

function eligibilityPath(sequence) {
    return pathFromTemplate(CREDIT_PATH, {
        customerId: deterministicUuid((sequence % numberEnv('CUSTOMERS', 500)) + 1, 100000),
        amount: 1000 + (sequence % 20) * 50,
    });
}

export function overload(data) {
    const tags = { scenario_id: SCENARIO, operation: 'recovery-overload', phase: 'overload' };
    const response = requestJson('GET', eligibilityPath(iterationNumber()), data.token, null, tags);
    expectStatus(response, [200], `${SCENARIO} sobrecarga`, tags);
}

export function recoveryWindow(data) {
    const healthTags = { scenario_id: SCENARIO, operation: 'readiness-after-load', phase: 'recovery' };
    const health = requestJson('GET', READINESS_PATH, data.token, null, healthTags);
    if (expectStatus(health, [200], `${SCENARIO} health`, healthTags)) {
        expectBody(health, (candidate) => {
            const body = jsonOrNull(candidate);
            return body !== null && body.status === 'UP';
        }, `${SCENARIO} health`, healthTags);
    }

    const creditTags = { scenario_id: SCENARIO, operation: 'eligibility-after-load', phase: 'recovery' };
    const credit = requestJson('GET', eligibilityPath(iterationNumber(700000)), data.token, null, creditTags);
    expectStatus(credit, [200], `${SCENARIO} elegibilidad`, creditTags);
    sleep(1);
}

