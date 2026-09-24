"use strict";

/**
 * Login simulado (UI-01, Kevin Girón). La autenticación real depende de
 * BB-07 / ADR-013 (Edson Sosa) — el módulo `security` del backend solo
 * está declarado, sin implementación. Estas dos cuentas viven únicamente en
 * este navegador (`sessionStorage` o `localStorage` si se marca
 * "recordarme"); ningún backend las valida.
 *
 * Se incluye antes de app.js / app-cliente.js / app-login.js en cada
 * página, sin módulos ES ni build tooling.
 */

const SGO_SESSION_KEY = "sgo.session";
const SGO_REGISTERED_ACCOUNTS_KEY = "sgo.registeredAccounts";

const SGO_ACCOUNTS = {
    CLIENTE: {
        role: "CLIENTE",
        customerId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        email: "cliente@megastore.com",
        password: "Cliente123",
        name: "Cliente de prueba",
    },
    STAFF: {
        role: "STAFF",
        email: "staff@megastore.com",
        password: "Staff123",
        name: "Personal interno de prueba",
    },
    FINANCE: {
        role: "FINANCE",
        email: "finanzas@megastore.com",
        password: "Finanzas123",
        name: "Finanzas de prueba",
    },
};

function sgoFindAccount(email, password) {
    const normalized = String(email || "").trim().toLowerCase();
    let registered = [];
    try { registered = JSON.parse(localStorage.getItem(SGO_REGISTERED_ACCOUNTS_KEY)) || []; } catch { registered = []; }
    return (
        [...Object.values(SGO_ACCOUNTS), ...registered].find(
            (account) => account.email === normalized && account.password === password
        ) || null
    );
}

function sgoRegisterClient(name, email, password) {
    const normalizedEmail = String(email || "").trim().toLowerCase();
    if (sgoFindAccount(normalizedEmail, password)
            || Object.values(SGO_ACCOUNTS).some(account => account.email === normalizedEmail)) {
        throw new Error("Ya existe una cuenta con ese correo.");
    }
    let accounts = [];
    try { accounts = JSON.parse(localStorage.getItem(SGO_REGISTERED_ACCOUNTS_KEY)) || []; } catch { accounts = []; }
    if (accounts.some(account => account.email === normalizedEmail)) throw new Error("Ya existe una cuenta con ese correo.");
    const account = { role: "CLIENTE", customerId: crypto.randomUUID(), email: normalizedEmail, password, name: String(name).trim() };
    accounts.push(account);
    localStorage.setItem(SGO_REGISTERED_ACCOUNTS_KEY, JSON.stringify(accounts));
    return account;
}

/**
 * `remember` decide dónde vive la sesión: `localStorage` (sobrevive a
 * cerrar el navegador, como un "recordarme" real) o `sessionStorage`
 * (termina al cerrar la pestaña). Nunca se guarda en ambos a la vez.
 */
function sgoCreateSession(account, remember) {
    const payload = JSON.stringify({
        role: account.role,
        customerId: account.customerId || null,
        email: account.email,
        name: account.name,
        loggedInAt: new Date().toISOString(),
    });
    if (remember) {
        localStorage.setItem(SGO_SESSION_KEY, payload);
        sessionStorage.removeItem(SGO_SESSION_KEY);
    } else {
        sessionStorage.setItem(SGO_SESSION_KEY, payload);
        localStorage.removeItem(SGO_SESSION_KEY);
    }
}

function sgoGetSession() {
    const raw = sessionStorage.getItem(SGO_SESSION_KEY) || localStorage.getItem(SGO_SESSION_KEY);
    try {
        return raw ? JSON.parse(raw) : null;
    } catch {
        return null;
    }
}

function sgoLandingPageFor(role) {
    return role === "CLIENTE" ? "cliente.html" : "staff.html";
}

function sgoLogout() {
    sessionStorage.removeItem(SGO_SESSION_KEY);
    localStorage.removeItem(SGO_SESSION_KEY);
    location.href = "login.html";
}

/** Redirige a login.html si no hay una sesión simulada con el rol esperado. */
function sgoRequireSession(expectedRole) {
    const session = sgoGetSession();
    if (!session || session.role !== expectedRole) {
        location.href = "login.html";
        return null;
    }
    return session;
}

/** Admite un conjunto explícito de roles internos sin convertirlos entre sí. */
function sgoRequireAnySession(expectedRoles) {
    const session = sgoGetSession();
    if (!session || !expectedRoles.includes(session.role)) {
        location.href = "login.html";
        return null;
    }
    return session;
}
