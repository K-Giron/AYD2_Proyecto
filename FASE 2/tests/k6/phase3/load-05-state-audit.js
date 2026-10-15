import { sleep } from 'k6';
import {
    customerIdFor,
    eligibleCustomerSequence,
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
    return { supportToken: login('support'), financeToken: login('finance') };
}

export default function (data) {
    const sequence = eligibleCustomerSequence(iterationNumber());
    const lookupTags = { scenario_id: `${SCENARIO}-SETUP`, operation: 'order-list' };
    const ordersResponse = requestJson('GET',
        `/api/v1/orders?customerId=${customerIdFor(sequence)}`,
        data.supportToken, null, lookupTags);
    if (!expectStatus(ordersResponse, [200], `${SCENARIO} lista`, lookupTags)) return;
    const orders = jsonOrNull(ordersResponse);
    if (!expectBody(ordersResponse, () => Array.isArray(orders) && orders.length > 0,
        `${SCENARIO} requiere órdenes de LOAD-02`, lookupTags)) return;
    const orderId = orders[0].id;

    const summaryTags = { scenario_id: SCENARIO, operation: 'order-summary' };
    const summary = requestJson(
        'GET',
        pathFromTemplate(SUMMARY_TEMPLATE, { orderId }),
        data.supportToken,
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

