"use strict";

/**
 * Sesión JWT (BB-07 / ADR-013). El login llama a
 * `POST /api/v1/auth/login`, guarda el JWT y las llamadas de la UI lo
 * envían como `Authorization: Bearer`. Ya no existe validación de cuentas
 * en el navegador.
 *
 * Se incluye antes de app.js / app-cliente.js / app-login.js en cada
 * página, sin módulos ES ni build tooling.
 */

// crypto.randomUUID solo existe en contextos seguros (HTTPS o localhost). En
// la nube el sitio se sirve por http://<IP>, así que sin este respaldo los
// scripts que generan correlation ids fallan al cargar. getRandomValues sí
// está disponible por HTTP y produce un UUID v4 equivalente.
if (typeof crypto.randomUUID !== "function") {
    crypto.randomUUID = () => "10000000-1000-4000-8000-100000000000".replace(/[018]/g, c =>
        (Number(c) ^ (crypto.getRandomValues(new Uint8Array(1))[0] & (15 >> (Number(c) / 4)))).toString(16));
}

const SGO_SESSION_KEY = "sgo.session";
// Versiones anteriores guardaban cuentas y contraseñas de demostración en el
// navegador. Ya no son válidas ni seguras: la identidad vive en el backend.
localStorage.removeItem("sgo.registeredAccounts");

/**
 * Resuelve los accesos entre portales usando el host desde el que el usuario
 * abrió la plataforma. En desarrollo el frontend se publica en 8081; en nube
 * se sirve por el puerto estándar, mientras los portales conservan sus puertos.
 */
function configurePortalLinks() {
    const localHosts = new Set(["localhost", "127.0.0.1", "::1"]);
    const ports = {
        main: localHosts.has(window.location.hostname) ? "8081" : "",
        inventory: "8082",
        store: "8084",
        logiexpress: "8085",
        payklik: "8086",
    };

    document.querySelectorAll("a[data-portal]").forEach(link => {
        const port = ports[link.dataset.portal];
        if (port === undefined) return;

        const url = new URL(window.location.href);
        url.port = port;
        url.pathname = link.dataset.portalPath || "/";
        url.search = "";
        url.hash = "";
        link.href = url.toString();
    });
}

configurePortalLinks();

/** Traduce los roles del backend a las dos experiencias de la UI. */
function sgoUiRoleFor(roles) {
    if (Array.isArray(roles) && roles.includes("ONLINE_STORE")) return "ONLINE_STORE";
    if (Array.isArray(roles) && roles.includes("CLIENT")) return "CLIENTE";
    if (Array.isArray(roles) && roles.includes("FINANCE")) return "FINANCE";
    return "STAFF";
}

/**
 * Autentica contra el backend y devuelve `{ ok, session }` o
 * `{ ok: false, message }`. No guarda nada; app-login.js decide si la
 * sesión corresponde a la pestaña elegida.
 */
async function sgoLogin(email, password) {
    const normalizedEmail = String(email || "").trim().toLowerCase();
    let response;
    try {
        response = await fetch("/api/v1/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                username: normalizedEmail,
                password,
            }),
        });
    } catch {
        return { ok: false, message: "No se pudo contactar al servidor de autenticación." };
    }

    const text = await response.text();
    let data = null;
    try {
        data = text ? JSON.parse(text) : null;
    } catch {
        data = null;
    }
    if (!response.ok) {
        return { ok: false, message: (data && data.message) || "Correo o contraseña incorrectos." };
    }
    const roles = Array.isArray(data.roles) ? data.roles : [];
    const customerId = roles.includes("CLIENT") ? data.customerId : null;
    if (roles.includes("CLIENT") && !customerId) {
        return { ok: false, message: "La cuenta no posee una identidad de cliente válida." };
    }
    return {
        ok: true,
        session: {
            role: sgoUiRoleFor(roles),
            backendRoles: roles,
            customerId,
            email: data.username || normalizedEmail,
            name: data.fullName || data.username || normalizedEmail,
            identityVersion: 2,
            accessToken: data.accessToken,
            expiresAt: data.expiresAt,
            loggedInAt: new Date().toISOString(),
        },
    };
}

/**
 * Registro persistente: el backend codifica la contraseña con BCrypt, crea un
 * customerId único y devuelve un JWT limitado a ese cliente.
 */
async function sgoRegisterClient(name, email, password) {
    const normalizedEmail = String(email || "").trim().toLowerCase();
    let response;
    try {
        response = await fetch("/api/v1/auth/register", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ fullName: String(name || "").trim(), email: normalizedEmail, password }),
        });
    } catch {
        throw new Error("No se pudo contactar al servidor para crear la cuenta.");
    }
    const text = await response.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = null; }
    if (!response.ok) {
        throw new Error((data && data.message) || "No se pudo crear la cuenta.");
    }
    return {
        role: sgoUiRoleFor(data.roles),
        backendRoles: data.roles,
        customerId: data.customerId,
        email: data.username || normalizedEmail,
        name: data.fullName || String(name || "").trim(),
        identityVersion: 2,
        accessToken: data.accessToken,
        expiresAt: data.expiresAt,
        loggedInAt: new Date().toISOString(),
    };
}

/**
 * `remember` decide dónde vive la sesión: `localStorage` (sobrevive a
 * cerrar el navegador, como un "recordarme" real) o `sessionStorage`
 * (termina al cerrar la pestaña). Nunca se guarda en ambos a la vez.
 */
function sgoCreateSession(session, remember) {
    const payload = JSON.stringify(session);
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
        const session = raw ? JSON.parse(raw) : null;
        // Fuerza un nuevo login después de abandonar la identidad canónica
        // compartida. Los tokens CLIENT anteriores no distinguen cuentas.
        if (session?.role === "CLIENTE" && session.identityVersion !== 2) {
            sessionStorage.removeItem(SGO_SESSION_KEY);
            localStorage.removeItem(SGO_SESSION_KEY);
            return null;
        }
        return session;
    } catch {
        return null;
    }
}

/** Cabeceras de autorización para las llamadas a la API. */
function sgoAuthHeaders() {
    const session = sgoGetSession();
    return session && session.accessToken ? { Authorization: "Bearer " + session.accessToken } : {};
}

function sgoLandingPageFor(role) {
    if (role === "CLIENTE") return "cliente.html";
    if (role === "ONLINE_STORE") return "tienda-online.html";
    return "staff.html";
}

function sgoLogout() {
    sessionStorage.removeItem(SGO_SESSION_KEY);
    localStorage.removeItem(SGO_SESSION_KEY);
    location.href = "login.html";
}

/** Redirige a login.html si no hay una sesión con el rol esperado. */
function sgoRequireSession(expectedRole) {
    const session = sgoGetSession();
    if (!session || session.role !== expectedRole) {
        location.href = "login.html";
        return null;
    }
    return session;
}

function sgoRequireAnySession(expectedRoles) {
    const session = sgoGetSession();
    if (!session || !expectedRoles.includes(session.role)) {
        location.href = "login.html";
        return null;
    }
    return session;
}

// Catálogo de la web = lo que el administrador de la tienda online publicó
// (con su imagen, precio y descripción). {@code available} es la existencia
// real de la tienda online menos lo reservado por otras órdenes: la misma que
// la reserva atómica valida al pagar (ADR-015, módulo onlinestore).
async function sgoLoadCatalog() {
    const response = await fetch("/api/v1/online-catalog", { cache: "no-store" });
    if (!response.ok) throw new Error("No se pudo cargar el catálogo de la tienda online.");
    const products = await response.json();
    return products.map(product => ({
        ...product,
        category: product.category || "General",
        description: product.description || "",
        image: product.image || "favicon.svg",
        available: Number(product.available) || 0,
    }));
}
