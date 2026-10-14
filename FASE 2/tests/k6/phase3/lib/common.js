import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

export const operationDuration = new Trend('phase3_operation_duration', true);
export const operationErrors = new Rate('phase3_operation_errors');
export const operationRequests = new Counter('phase3_operation_requests');

export const BASE_URL = (__ENV.BASE_URL || 'http://host.docker.internal:8080').replace(/\/+$/, '');

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

export function login(role = 'client') {
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
    return token;
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
    return `k6-fase3-${prefix}-${__VU}-${sequence}`;
}

