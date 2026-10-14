import { sleep } from 'k6';
import {
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

const SCENARIO = 'LOAD-05';
const SUMMARY_TEMPLATE = __ENV.ORDER_SUMMARY_PATH_TEMPLATE || '/api/v1/orders/{orderId}/summary';
const AUDIT_TEMPLATE = __ENV.AUDIT_PATH_TEMPLATE || '/api/v1/audit/events?orderId={orderId}';

export const options = {
    scenarios: {
        state_and_audit_queries: {
            executor: 'constant-vus',
            vus: numberEnv('VUS', 10),
            duration: __ENV.DURATION || '2m',
        },
    },
    thresholds: {
        'phase3_operation_errors{scenario_id:LOAD-05}': ['rate<0.01'],
        'phase3_operation_duration{operation:order-summary}': ['p(95)<1000'],
        'phase3_operation_duration{operation:audit-events}': ['p(95)<1000'],
    },
};

export function setup() {
    return { clientToken: login('client'), financeToken: login('finance') };
}

function seededOrderId(sequence) {
    return `00000000-0000-0000-0000-${String(sequence).padStart(12, '0')}`;
}

export default function (data) {
    const sequence = (iterationNumber() % numberEnv('ORDERS', 500)) + 1;
    const orderId = seededOrderId(sequence);

    const summaryTags = { scenario_id: SCENARIO, operation: 'order-summary' };
    const summary = requestJson(
        'GET',
        pathFromTemplate(SUMMARY_TEMPLATE, { orderId }),
        data.clientToken,
        null,
        summaryTags,
    );
    if (expectStatus(summary, [200], `${SCENARIO} resumen`, summaryTags)) {
        expectBody(summary, (candidate) => jsonOrNull(candidate) !== null,
            `${SCENARIO} resumen`, summaryTags);
    }

    const auditTags = { scenario_id: SCENARIO, operation: 'audit-events' };
    const audit = requestJson(
        'GET',
        pathFromTemplate(AUDIT_TEMPLATE, { orderId }),
        data.financeToken,
        null,
        auditTags,
    );
    if (expectStatus(audit, [200], `${SCENARIO} auditoría`, auditTags)) {
        expectBody(audit, (candidate) => Array.isArray(jsonOrNull(candidate)),
            `${SCENARIO} auditoría`, auditTags);
    }
    sleep(pauseSeconds());
}

