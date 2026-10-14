import { sleep } from 'k6';
import {
    creditHistoryBatch,
    customerIdFor,
    customerSequence,
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

const SCENARIO = 'LOAD-03';
const ERP_HISTORY_PATH = __ENV.ERP_HISTORY_PATH || '/api/v1/credit/history';

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
    requireFlag('ALLOW_MUTATIONS', 'LOAD-03 ingresa historial crediticio sintético.');
    return { token: login('finance') };
}

export default function (data) {
    const sequence = iterationNumber();
    // Cada par repite sourceReference; esa es la llave del contrato credit-history-v1.
    const eventSequence = Math.floor((sequence - 1) / 2) + 1;
    const customerSequenceNumber = customerSequence(eventSequence);
    const batch = creditHistoryBatch(eventSequence,
        customerIdFor(customerSequenceNumber), null, `load03-${__ENV.RUN_ID || 'manual'}`);
    const tags = { scenario_id: SCENARIO, operation: 'erp-history-ingestion' };
    const response = requestJson('POST', ERP_HISTORY_PATH, data.token, batch, tags, {}, [200]);

    if (expectStatus(response, [200], SCENARIO, tags)) {
        expectBody(response, (candidate) => {
            const body = jsonOrNull(candidate);
            return body !== null && body.received === batch.entries.length
                && body.accepted + body.duplicates === body.received;
        }, SCENARIO, tags);
    }
    sleep(pauseSeconds());
}

