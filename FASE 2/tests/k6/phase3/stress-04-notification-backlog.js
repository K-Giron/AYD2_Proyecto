import { sleep } from 'k6';
import {
    expectBody,
    expectStatus,
    iterationNumber,
    jsonOrNull,
    login,
    numberEnv,
    requestJson,
} from './lib/common.js';

const SCENARIO = 'STRESS-04';
const NOTIFICATIONS_PATH = __ENV.NOTIFICATIONS_PATH || '/api/v1/notifications';
const SOURCE_ID = __ENV.FIXED_SOURCE_ID || '00000000-0000-4000-8000-000000900001';
const MAX_VUS = numberEnv('STRESS_MAX_VUS', 160);

export const options = {
    scenarios: {
        create_backlog: {
            executor: 'ramping-vus',
            exec: 'createBacklog',
            startVUs: 1,
            stages: [
                { duration: '30s', target: Math.ceil(MAX_VUS * 0.25) },
                { duration: '1m', target: MAX_VUS },
                { duration: '1m', target: MAX_VUS },
                { duration: '30s', target: 0 },
            ],
        },
        observe_drain: {
            executor: 'constant-vus',
            exec: 'observeDrain',
            startTime: '3m15s',
            vus: 2,
            duration: '2m',
        },
    },
    thresholds: {
        'phase3_operation_errors{operation:notification-backlog-create}': ['rate<0.10'],
        'phase3_operation_errors{operation:notification-backlog-drain}': ['rate<0.01'],
        'phase3_operation_duration{operation:notification-backlog-drain}': ['p(95)<1000'],
    },
};

export function setup() {
    return { supportToken: login('support'), clientToken: login('client') };
}

export function createBacklog(data) {
    const sequence = iterationNumber();
    const tags = { scenario_id: SCENARIO, operation: 'notification-backlog-create' };
    const response = requestJson('POST', NOTIFICATIONS_PATH, data.supportToken, {
        eventType: __ENV.CREDIT_NOTIFICATION_EVENT || 'ORDER_CONFIRMED',
        channel: 'EMAIL',
        recipient: `stress-${__VU}-${sequence}@example.invalid`,
        sourceAggregateId: SOURCE_ID,
    }, tags);
    expectStatus(response, [202], `${SCENARIO} creación`, tags);
}

export function observeDrain(data) {
    const tags = { scenario_id: SCENARIO, operation: 'notification-backlog-drain' };
    const response = requestJson(
        'GET',
        `${NOTIFICATIONS_PATH}?sourceAggregateId=${SOURCE_ID}`,
        data.clientToken,
        null,
        tags,
    );
    if (expectStatus(response, [200], `${SCENARIO} drenaje`, tags)) {
        expectBody(response, (candidate) => {
            const body = jsonOrNull(candidate);
            return Array.isArray(body) && body.length > 0
                && body.some((notification) => ['SENT', 'FAILED'].includes(notification.status));
        }, `${SCENARIO} drenaje`, tags);
    }
    sleep(1);
}

