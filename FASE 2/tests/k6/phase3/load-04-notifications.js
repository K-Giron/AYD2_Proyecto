import { sleep } from 'k6';
import {
    deterministicUuid,
    expectBody,
    expectStatus,
    iterationNumber,
    jsonOrNull,
    login,
    numberEnv,
    pauseSeconds,
    requestJson,
    requireFlag,
} from './lib/common.js';

const SCENARIO = 'LOAD-04';
const NOTIFICATIONS_PATH = __ENV.NOTIFICATIONS_PATH || '/api/v1/notifications';

export const options = {
    scenarios: {
        sustained_notifications: {
            executor: 'constant-vus',
            vus: numberEnv('VUS', 10),
            duration: __ENV.DURATION || '2m',
        },
    },
    thresholds: {
        'phase3_operation_errors{scenario_id:LOAD-04}': ['rate<0.01'],
        'phase3_operation_duration{scenario_id:LOAD-04}': ['p(95)<1000', 'p(99)<2000'],
    },
};

export function setup() {
    requireFlag('ALLOW_MUTATIONS', 'LOAD-04 crea solicitudes de notificación sintéticas.');
    return { token: login('support') };
}

export default function (data) {
    const sequence = iterationNumber();
    const sourceAggregateId = deterministicUuid(
        (sequence % numberEnv('ORDERS', 500)) + 1,
        400000,
    );
    const tags = { scenario_id: SCENARIO, operation: 'notification-create' };
    const response = requestJson('POST', NOTIFICATIONS_PATH, data.token, {
        eventType: __ENV.CREDIT_NOTIFICATION_EVENT || 'CREDIT_GRANTED',
        channel: 'EMAIL',
        recipient: `carga-${sourceAggregateId}@example.invalid`,
        sourceAggregateId,
    }, tags);

    if (expectStatus(response, [202], SCENARIO, tags)) {
        expectBody(response, (candidate) => {
            const body = jsonOrNull(candidate);
            return body !== null && body.id !== undefined;
        }, SCENARIO, tags);
    }
    sleep(pauseSeconds());
}

