"use strict";

/**
 * Sesión real (BB-07 / ADR-013, Edson Sosa). El login llama a
 * `POST /api/v1/auth/login`, guarda el JWT y las llamadas de la UI lo
 * envían como `Authorization: Bearer`. Ya no existe validación de cuentas
 * en el navegador.
 *
 * Se incluye antes de app.js / app-cliente.js / app-login.js en cada
 * página, sin módulos ES ni build tooling.
 */

const SGO_SESSION_KEY = "sgo.session";

/** Traduce los roles del backend a las dos experiencias de la UI. */
function sgoUiRoleFor(roles) {
    return Array.isArray(roles) && roles.includes("CLIENT") ? "CLIENTE" : "STAFF";
}

/**
 * Autentica contra el backend y devuelve `{ ok, session }` o
 * `{ ok: false, message }`. No guarda nada; app-login.js decide si la
 * sesión corresponde a la pestaña elegida.
 */
async function sgoLogin(email, password) {
    let response;
    try {
        response = await fetch("/api/v1/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                username: String(email || "").trim().toLowerCase(),
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
    return {
        ok: true,
        session: {
            role: sgoUiRoleFor(roles),
            backendRoles: roles,
            email: String(email || "").trim().toLowerCase(),
            name: String(email || "").trim().toLowerCase(),
            accessToken: data.accessToken,
            expiresAt: data.expiresAt,
            loggedInAt: new Date().toISOString(),
        },
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
        return raw ? JSON.parse(raw) : null;
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
    return role === "CLIENTE" ? "cliente.html" : "staff.html";
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
