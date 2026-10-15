import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

export const operationDuration = new Trend('phase3_operation_duration', true);
export const operationErrors = new Rate('phase3_operation_errors');
export const operationRequests = new Counter('phase3_operation_requests');

export const BASE_URL = (__ENV.BASE_URL || 'http://host.docker.internal:8080').replace(/\/+$/, '');

export const TEST_PRODUCT = {
    id: __ENV.TEST_PRODUCT_ID || '11111111-1111-1111-1111-111111111102',
    name: __ENV.TEST_PRODUCT_NAME || 'Pantalón de lona negro',
    price: Number(__ENV.TEST_PRODUCT_PRICE || 399),
};

const ROLE_DEFAULTS = {
    client: {
        user: 'cliente@megastore.com',
        password: 'Cliente123',
        userEnv: 'CLIENT_USER',
        passwordEnv: 'CLIENT_PASSWORD',
    },
    finance: {
        user: 'finanzas@megastore.com',
        password: 'Finanzas123',
        userEnv: 'FINANCE_USER',
        passwordEnv: 'FINANCE_PASSWORD',
    },
    support: {
        user: 'staff@megastore.com',
        password: 'Staff123',
        userEnv: 'SUPPORT_USER',
        passwordEnv: 'SUPPORT_PASSWORD',
    },
};

export function loginSession(role = 'client') {
    const selected = ROLE_DEFAULTS[role];
    if (!selected) {
        throw new Error(`Rol k6 no soportado: ${role}`);
    }
    const username = __ENV[selected.userEnv] || selected.user;
    const password = __ENV[selected.passwordEnv] || selected.password;
    const response = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ username, password }),
        { headers: { 'Content-Type': 'application/json' }, tags: { operation: 'login', role } },
    );
    if (response.status !== 200) {
        throw new Error(`Login ${role} falló con HTTP ${response.status}: ${response.body}`);
    }
    const token = response.json('accessToken');
    if (!token) {
        throw new Error(`Login ${role} no devolvió accessToken`);
    }
    return {
        token,
        customerId: response.json('customerId'),
        roles: response.json('roles') || [],
    };
}

export function login(role = 'client') {
    return loginSession(role).token;
}

export function requestJson(method, path, token, body, tags = {}, extraHeaders = {}, acceptedStatuses = null) {
    const url = path.startsWith('http://') || path.startsWith('https://')
        ? path
        : `${BASE_URL}${path.startsWith('/') ? path : `/${path}`}`;
    const headers = {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
        ...extraHeaders,
    };
    const params = { headers, tags };
    if (acceptedStatuses !== null) {
        params.responseCallback = http.expectedStatuses(...acceptedStatuses);
    }
    const response = http.request(
        method,
        url,
        body === undefined || body === null ? null : JSON.stringify(body),
        params,
    );
    operationRequests.add(1, tags);
    operationDuration.add(response.timings.duration, tags);
    return response;
}

export function expectStatus(response, allowedStatuses, label, tags = {}) {
    const ok = check(response, {
        [`${label}: estado esperado`]: (candidate) => allowedStatuses.includes(candidate.status),
    });
    operationErrors.add(!ok, tags);
    return ok;
}

export function expectBody(response, predicate, label, tags = {}) {
    const ok = check(response, {
        [`${label}: respuesta válida`]: predicate,
    });
    operationErrors.add(!ok, tags);
    return ok;
}

export function jsonOrNull(response) {
    try {
        return response.json();
    } catch (_error) {
        return null;
    }
}

export function iterationNumber(offset = 0) {
    return ((__VU * 1000000 + __ITER + offset) % 999999999999) + 1;
}

export function deterministicUuid(sequence, namespace = 0) {
    const tail = String((Number(sequence) + Number(namespace)) % 999999999999)
        .padStart(12, '0');
    return `00000000-0000-4000-8000-${tail}`;
}

export function pathFromTemplate(template, values) {
    return Object.entries(values).reduce(
        (result, [key, value]) => result.replaceAll(`{${key}}`, encodeURIComponent(String(value))),
        template,
    );
}

export function numberEnv(name, fallback) {
    const value = Number(__ENV[name] || fallback);
    if (!Number.isFinite(value)) {
        throw new Error(`${name} debe ser numérico`);
    }
    return value;
}

export function pauseSeconds() {
    return numberEnv('PAUSE', 0.25);
}

export function idempotencyKey(prefix, sequence) {
    const runId = String(__ENV.RUN_ID || 'manual').replace(/[^A-Za-z0-9_.-]/g, '-');
    return `k6-fase3-${runId}-${prefix}-${__VU}-${sequence}`;
}

export function requireFlag(name, explanation) {
    if (String(__ENV[name] || '').toLowerCase() !== 'true') {
        throw new Error(`${name}=true es obligatorio. ${explanation}`);
    }
}

export function customerIdFor(sequence) {
    return deterministicUuid(sequence, 100000);
}

export function customerSequence(sequence, customers = numberEnv('CUSTOMERS', 500)) {
    return ((Number(sequence) - 1) % customers) + 1;
}

export function eligibleCustomerSequence(sequence, customers = numberEnv('CUSTOMERS', 500)) {
    const eligibleCustomers = Math.max(1, Math.floor(customers / 5));
    return ((Number(sequence) - 1) % eligibleCustomers) * 5 + 1;
}

export function createCartWithItem(token, customerId, sequence, tags = {}) {
    const auxiliaryTags = { ...tags, supporting_action: 'true' };
    const opened = requestJson('POST', '/api/v1/carts', token, {
        customerId,
        channel: 'WEB',
    }, { ...auxiliaryTags, operation: 'cart-open' }, {
        'Idempotency-Key': idempotencyKey('cart', sequence),
    }, [201]);
    if (!expectStatus(opened, [201], 'abrir carrito', auxiliaryTags)) {
        return null;
    }
    const cart = jsonOrNull(opened);
    if (!cart?.id) {
        operationErrors.add(true, auxiliaryTags);
        return null;
    }

    const item = requestJson('PUT', `/api/v1/carts/${cart.id}/items/${TEST_PRODUCT.id}`, token, {
        productName: TEST_PRODUCT.name,
        unitPrice: TEST_PRODUCT.price,
        quantity: 1,
    }, { ...auxiliaryTags, operation: 'cart-item' }, {}, [200]);
    if (!expectStatus(item, [200], 'agregar producto', auxiliaryTags)) {
        return null;
    }
    return { cartId: cart.id, customerId, total: TEST_PRODUCT.price };
}

export function checkoutCart(token, cartId, sequence, tags = {}) {
    return requestJson('POST', `/api/v1/carts/${cartId}/checkout`, token, null, tags, {
        'Idempotency-Key': idempotencyKey('checkout', sequence),
        'X-Actor': 'k6:phase3',
    }, [201]);
}

export function sagaPayload(paymentRoute, sequence) {
    const payKlik = paymentRoute === 'PAYKLIK';
    return {
        paymentRoute,
        paymentMethod: payKlik ? 'CARD' : null,
        currency: 'GTQ',
        paymentReference: payKlik ? `tok_k6_${String(sequence).padStart(8, '0')}` : null,
        taxIdentifier: 'CF-K6',
        customerName: 'Cliente sintético k6',
        destination: 'Zona de pruebas de rendimiento',
    };
}

export function runSaga(token, orderId, paymentRoute, sequence, tags = {}) {
    return requestJson('POST', `/api/v1/orders/${orderId}/saga`, token,
        sagaPayload(paymentRoute, sequence), tags, {
            'Idempotency-Key': idempotencyKey(`saga-${paymentRoute.toLowerCase()}`, sequence),
            'X-Actor': 'k6:phase3',
        }, [200]);
}

function occurredDaysAgo(days) {
    return new Date(Date.now() - Number(days) * 24 * 60 * 60 * 1000).toISOString();
}

function historyEntry(customerId, sourceReference, type, daysAgo, amount, onTime = null, critical = false) {
    return {
        customerId,
        sourceReference,
        type,
        occurredAt: occurredDaysAgo(daysAgo),
        amount,
        onTime,
        critical,
    };
}

export function creditHistoryBatch(sequence, customerId, forcedCohort = null, sourceNamespace = 'dataset') {
    const cohort = forcedCohort === null ? (Number(sequence) - 1) % 5 : Number(forcedCohort);
    const namespace = String(sourceNamespace).replace(/[^A-Za-z0-9_.-]/g, '-').slice(0, 80);
    const prefix = `k6-f3-${namespace}-c${String(sequence).padStart(6, '0')}`;
    const entries = [];
    const purchases = cohort === 1 ? 2 : cohort === 0 ? 5 : 4;
    const purchaseAmount = cohort === 0 ? 3000 : 1000;
    for (let index = 1; index <= purchases; index += 1) {
        entries.push(historyEntry(customerId, `${prefix}-purchase-${index}`, 'PURCHASE', 20 + index,
            purchaseAmount));
    }
    if (cohort === 0) {
        for (let index = 1; index <= 5; index += 1) {
            entries.push(historyEntry(customerId, `${prefix}-payment-${index}`, 'PAYMENT', 10 + index,
                purchaseAmount, true));
        }
    } else if (cohort === 2) {
        entries.push(historyEntry(customerId, `${prefix}-return-1`, 'RETURN', 8, 250));
        entries.push(historyEntry(customerId, `${prefix}-return-2`, 'RETURN', 7, 250));
    } else if (cohort === 3) {
        entries.push(historyEntry(customerId, `${prefix}-incident-1`, 'INCIDENT', 6, 0, null, true));
        entries.push(historyEntry(customerId, `${prefix}-incident-2`, 'INCIDENT', 5, 0, null, true));
    } else if (cohort === 4) {
        entries.push(historyEntry(customerId, `${prefix}-payment-1`, 'PAYMENT', 6, 1000, true));
        entries.push(historyEntry(customerId, `${prefix}-payment-2`, 'PAYMENT', 5, 1000, false));
        entries.push(historyEntry(customerId, `${prefix}-payment-3`, 'PAYMENT', 4, 1000, false));
    }
    return {
        contractVersion: 'credit-history-v1',
        correlationId: deterministicUuid(sequence, 810000),
        entries,
    };
}

