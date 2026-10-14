import {
    deterministicUuid,
    expectStatus,
    idempotencyKey,
    login,
    numberEnv,
    requestJson,
} from './lib/common.js';

const ERP_HISTORY_PATH = __ENV.ERP_HISTORY_PATH || '/api/v1/integrations/erp/customer-history';
const CUSTOMERS = numberEnv('CUSTOMERS', 500);

export const options = {
    scenarios: {
        deterministic_seed: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: CUSTOMERS,
            maxDuration: '10m',
        },
    },
    thresholds: {
        'phase3_operation_errors{scenario_id:DATASET}': ['rate<0.01'],
    },
};

export function setup() {
    return { token: login('finance') };
}

export default function (data) {
    const sequence = __ITER + 1;
    const cohort = sequence % 4;
    const profiles = [
        { purchaseCount: 18, totalPurchased: 14000, onTimePaymentRate: 0.98, returnCount: 0, incidentCount: 0 },
        { purchaseCount: 9, totalPurchased: 6200, onTimePaymentRate: 0.90, returnCount: 1, incidentCount: 0 },
        { purchaseCount: 4, totalPurchased: 1900, onTimePaymentRate: 0.60, returnCount: 1, incidentCount: 2 },
        { purchaseCount: 1, totalPurchased: 450, onTimePaymentRate: 0.00, returnCount: 0, incidentCount: 0 },
    ];
    const profile = profiles[cohort];
    const tags = { scenario_id: 'DATASET', operation: 'dataset-seed', cohort: String(cohort) };
    const response = requestJson('POST', ERP_HISTORY_PATH, data.token, {
        eventId: deterministicUuid(sequence, 800000),
        customerId: deterministicUuid(sequence, 100000),
        ...profile,
        occurredAt: '2026-10-13T12:00:00Z',
    }, tags, { 'Idempotency-Key': idempotencyKey('dataset', sequence) });
    expectStatus(response, [200, 201, 202], 'DATASET', tags);
}

