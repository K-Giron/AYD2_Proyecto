import { sleep } from 'k6';
import {
    checkoutCart,
    createCartWithItem,
    customerIdFor,
    customerSequence,
    eligibleCustomerSequence,
    expectBody,
    expectStatus,
    iterationNumber,
    jsonOrNull,
    login,
    numberEnv,
    pauseSeconds,
    requireFlag,
    runSaga,
} from './lib/common.js';

const SCENARIO = 'LOAD-02';

export const options = {
    scenarios: {
        expected_mixed_checkout: {
            executor: 'constant-vus',
            vus: numberEnv('VUS', 10),
            duration: __ENV.DURATION || '2m',
        },
    },
    thresholds: {
        'phase3_operation_errors{scenario_id:LOAD-02}': ['rate<0.01'],
        'phase3_operation_duration{scenario_id:LOAD-02}': ['p(95)<1500', 'p(99)<3000'],
    },
};

export function setup() {
    requireFlag('ALLOW_MUTATIONS', 'LOAD-02 crea órdenes y ejecuta la Saga dual.');
    return { token: login('support') };
}

export default function (data) {
    const sequence = iterationNumber();
    const paymentRoute = sequence % 2 === 0 ? 'OWN_CREDIT' : 'PAYKLIK';
    const selectedCustomer = paymentRoute === 'OWN_CREDIT'
        ? eligibleCustomerSequence(sequence)
        : customerSequence(sequence);
    const supportingTags = { scenario_id: `${SCENARIO}-SETUP`, payment_route: paymentRoute };
    const cart = createCartWithItem(data.token, customerIdFor(selectedCustomer), sequence, supportingTags);
    if (!cart) return;

    const checkout = checkoutCart(data.token, cart.cartId, sequence,
        { ...supportingTags, operation: 'checkout' });
    if (!expectStatus(checkout, [201], `${SCENARIO} checkout`, supportingTags)) return;
    const orderId = jsonOrNull(checkout)?.id;
    if (!orderId) return;

    const tags = { scenario_id: SCENARIO, operation: 'mixed-saga', payment_route: paymentRoute };
    let response;
    let body;
    const maxPolls = numberEnv('SAGA_MAX_POLLS', 12);
    for (let attempt = 0; attempt < maxPolls; attempt += 1) {
        response = runSaga(data.token, orderId, paymentRoute, sequence, tags);
        if (!expectStatus(response, [200], `${SCENARIO} Saga`, tags)) return;
        body = jsonOrNull(response);
        if (!['WAITING_FISCAL', 'WAITING_PAYMENT'].includes(body?.sagaStatus)) break;
        sleep(0.5);
    }
    expectBody(response, () => body !== null
        && ['COMPLETED', 'CREDIT_REJECTED'].includes(body.sagaStatus), SCENARIO, tags);
    sleep(pauseSeconds());
}

