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

const SCENARIO = 'LOAD-03';
const ERP_HISTORY_PATH = __ENV.ERP_HISTORY_PATH || '/api/v1/integrations/erp/customer-history';

export const options = {
    scenarios: {
        sustained_erp_history: {
            executor: 'constant-vus',
            vus: numberEnv('VUS', 10),
            duration: __ENV.DURATION || '2m',
        },
    },
    thresholds: {
        'phase3_operation_errors{scenario_id:LOAD-03}': ['rate<0.01'],
        'phase3_operation_duration{scenario_id:LOAD-03}': ['p(95)<1000', 'p(99)<2000'],
    },
};

export function setup() {
    return { token: login('finance') };
}

export default function (data) {
    const sequence = iterationNumber();
    // Cada par de iteraciones repite eventId para verificar idempotencia bajo carga.
    const eventSequence = Math.floor((sequence - 1) / 2) + 1;
    const eventId = deterministicUuid(eventSequence, 300000);
    const tags = { scenario_id: SCENARIO, operation: 'erp-history-ingestion' };
    const response = requestJson('POST', ERP_HISTORY_PATH, data.token, {
        eventId,
        customerId: deterministicUuid((eventSequence % numberEnv('CUSTOMERS', 500)) + 1, 100000),
        purchaseCount: 5 + (eventSequence % 20),
        totalPurchased: 1000 + (eventSequence % 50) * 100,
        onTimePaymentRate: 0.75 + (eventSequence % 25) / 100,
        returnCount: eventSequence % 3,
        incidentCount: eventSequence % 2,
        occurredAt: new Date().toISOString(),
    }, tags, { 'Idempotency-Key': idempotencyKey('erp-history', eventSequence) });

    if (expectStatus(response, [200, 201, 202], SCENARIO, tags)) {
        expectBody(response, (candidate) => jsonOrNull(candidate) !== null, SCENARIO, tags);
    }
    sleep(pauseSeconds());
}

