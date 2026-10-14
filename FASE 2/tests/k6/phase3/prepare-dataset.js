import {
    TEST_PRODUCT,
    creditHistoryBatch,
    customerIdFor,
    expectBody,
    expectStatus,
    jsonOrNull,
    login,
    numberEnv,
    requestJson,
    requireFlag,
} from './lib/common.js';

const ERP_HISTORY_PATH = __ENV.ERP_HISTORY_PATH || '/api/v1/credit/history';
const CUSTOMERS = numberEnv('CUSTOMERS', 500);

export const options = {
    scenarios: {
        deterministic_seed: {
            executor: 'shared-iterations',
            vus: 1,
            iterations: CUSTOMERS,
            maxDuration: '15m',
        },
    },
    thresholds: {
        'phase3_operation_errors{scenario_id:DATASET}': ['rate<0.01'],
    },
};

export function setup() {
    requireFlag('ALLOW_MUTATIONS',
        'La preparación crea historial crediticio sintético y puede agregar inventario.');
    const financeToken = login('finance');
    const inventoryTopUp = numberEnv('INVENTORY_TOPUP', 0);
    if (inventoryTopUp > 0) {
        const supportToken = login('support');
        const locations = requestJson('GET', '/api/v1/inventory/locations', supportToken, null,
            { scenario_id: 'DATASET', operation: 'inventory-locations' });
        if (locations.status !== 200) {
            throw new Error(`No se pudieron consultar ubicaciones: HTTP ${locations.status}`);
        }
        const location = (jsonOrNull(locations) || []).find((candidate) => candidate.code === 'CD-01');
        if (!location?.id) {
            throw new Error('No existe la ubicación CD-01 requerida para el inventario de pruebas.');
        }
        const movement = requestJson('POST', '/api/v1/inventory/stock-movements', supportToken, {
            productId: TEST_PRODUCT.id,
            locationId: location.id,
            type: 'INBOUND',
            quantity: inventoryTopUp,
            reason: `Dataset rendimiento Fase 3 (${__ENV.RUN_ID || 'manual'})`,
        }, { scenario_id: 'DATASET', operation: 'inventory-top-up' }, {}, [201]);
        if (movement.status !== 201) {
            throw new Error(`No se pudo agregar inventario sintético: HTTP ${movement.status}`);
        }
    }
    return { financeToken };
}

export default function (data) {
    const sequence = __ITER + 1;
    const cohort = (sequence - 1) % 5;
    const tags = { scenario_id: 'DATASET', operation: 'credit-history-seed', cohort: String(cohort) };
    const batch = creditHistoryBatch(sequence, customerIdFor(sequence));
    const response = requestJson('POST', ERP_HISTORY_PATH, data.financeToken, batch, tags, {}, [200]);

    if (expectStatus(response, [200], 'DATASET historial', tags)) {
        expectBody(response, (candidate) => {
            const body = jsonOrNull(candidate);
            return body !== null
                && body.received === batch.entries.length
                && body.accepted + body.duplicates === body.received;
        }, 'DATASET respuesta', tags);
    }
}

