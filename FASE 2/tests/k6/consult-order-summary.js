import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * QA-02 / DEC-02: línea base T y carga 8T sobre la consulta consolidada
 * (QRY-01, EaC-01). El conjunto de datos se carga con tests/data/seed-load.sql.
 */
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER = __ENV.CLIENT_USER || 'cliente@megastore.com';
const PASSWORD = __ENV.CLIENT_PASSWORD || 'Cliente123';
const ORDERS = Number(__ENV.ORDERS || 500);
const PAUSE = Number(__ENV.PAUSE || 0.5);

export const options = {
    scenarios: {
        load: {
            executor: 'constant-vus',
            vus: Number(__ENV.VUS || 10),
            duration: __ENV.DURATION || '1m',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<5000'],
    },
};

export function setup() {
    const response = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ username: USER, password: PASSWORD }),
        { headers: { 'Content-Type': 'application/json' } },
    );
    if (response.status !== 200) {
        throw new Error(`Login falló con HTTP ${response.status}`);
    }
    return { token: response.json('accessToken') };
}

export default function (data) {
    const sequence = ((__VU * 100000 + __ITER) % ORDERS) + 1;
    const orderId = `00000000-0000-0000-0000-${String(sequence).padStart(12, '0')}`;
    const response = http.get(`${BASE_URL}/api/v1/orders/${orderId}/summary`, {
        headers: { Authorization: `Bearer ${data.token}` },
        tags: { endpoint: 'order-summary' },
    });
    check(response, {
        'consulta 200': (r) => r.status === 200,
        'respuesta consolidada': (r) => r.body.includes('orderStatus'),
    });
    sleep(PAUSE);
}
