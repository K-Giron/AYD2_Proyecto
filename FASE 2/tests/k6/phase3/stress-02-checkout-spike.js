import { sleep } from 'k6';
import {
    checkoutCart,
    createCartWithItem,
    customerIdFor,
    customerSequence,
    expectBody,
    expectStatus,
    iterationNumber,
    jsonOrNull,
    login,
    numberEnv,
    pauseSeconds,
    requireFlag,
} from './lib/common.js';

const SCENARIO = 'STRESS-02';
const SPIKE_VUS = numberEnv('SPIKE_VUS', 200);

export const options = {
    scenarios: {
        checkout_spike: {
            executor: 'ramping-vus',
            startVUs: 5,
            stages: [
                { duration: '30s', target: 10 },
                { duration: '10s', target: SPIKE_VUS },
                { duration: '1m', target: SPIKE_VUS },
                { duration: '20s', target: 10 },
                { duration: '30s', target: 0 },
            ],
            gracefulRampDown: '15s',
        },
    },
    thresholds: {
        'phase3_operation_errors{scenario_id:STRESS-02}': ['rate<0.10'],
        'phase3_operation_duration{scenario_id:STRESS-02}': ['p(95)<5000'],
    },
};

export function setup() {
    requireFlag('ALLOW_MUTATIONS', 'STRESS-02 crea carritos y órdenes sintéticas.');
    requireFlag('ALLOW_STRESS', 'STRESS-02 genera un pico súbito de checkout.');
    return { token: login('support') };
}

export default function (data) {
    const sequence = iterationNumber();
    const cart = createCartWithItem(data.token,
        customerIdFor(customerSequence(sequence)), sequence,
        { scenario_id: `${SCENARIO}-SETUP` });
    if (!cart) return;
    const tags = { scenario_id: SCENARIO, operation: 'checkout-spike' };
    const response = checkoutCart(data.token, cart.cartId, sequence, tags);
    if (expectStatus(response, [201], SCENARIO, tags)) {
        expectBody(response, (candidate) => Boolean(jsonOrNull(candidate)?.id), SCENARIO, tags);
    }
    sleep(pauseSeconds());
}

