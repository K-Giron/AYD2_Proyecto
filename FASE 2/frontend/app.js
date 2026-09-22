"use strict";

/**
 * UI-01: panel operativo de Kevin Girón. Llama a los endpoints reales de
 * logistics, notifications, query e incidents — los únicos módulos
 * funcionales de este incremento. No simula checkout: ver la tarjeta
 * "Checkout" y RESPONSABILIDADES_INTEGRANTES_FASE_2.md §8.3.
 *
 * Todo el estado por orden se recarga desde el backend en cada "Cargar";
 * localStorage solo recuerda el último Order ID escrito, como conveniencia
 * de este navegador — no es una fuente de verdad.
 */

// sgoRequireSession redirige a login.html si no hay sesión de personal interno;
// la redirección es asíncrona, así que se protege el resto contra `session === null`.
const session = sgoRequireSession("STAFF");

const CORRELATION_ID = crypto.randomUUID();
document.getElementById("correlation-id").textContent = CORRELATION_ID;
if (session) {
    document.getElementById("session-name").textContent = session.name;
    document.getElementById("logout-btn").addEventListener("click", sgoLogout);
}

const ORDER_ID_STORAGE_KEY = "sgo-logic.lastOrderId";
let currentOrderId = null;

// ---------------------------------------------------------------------
// Utilidades generales
// ---------------------------------------------------------------------

function el(id) {
    return document.getElementById(id);
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
}

function formatDate(iso) {
    if (!iso) return "—";
    try {
        return new Date(iso).toLocaleString("es-GT", { dateStyle: "short", timeStyle: "medium" });
    } catch {
        return iso;
    }
}

function toast(message, type) {
    const stack = el("toast-stack");
    const node = document.createElement("div");
    node.className = `toast ${type || ""}`.trim();
    node.textContent = message;
    stack.appendChild(node);
    setTimeout(() => node.remove(), 5000);
}

const STATUS_TONE = {
    // verde: resultado final positivo
    DELIVERED: "ok", SENT: "ok", REFUNDED: "ok", LABEL_ISSUED: "ok", APPROVED: "ok", CONFIRMED: "ok",
    // ámbar: en curso
    PENDING: "warn", PREPARING: "warn", REGISTERED: "warn", IN_TRANSIT: "warn", ASSIGNED: "warn",
    LABEL_REQUESTED: "warn", READY_FOR_PICKUP: "warn", EVALUATING: "warn",
    // rojo: fallo o rechazo
    FAILED: "error", REJECTED: "error", LABEL_FAILED: "error", CANCELLED: "error", RETURNED: "error",
};

function badge(status) {
    if (!status) return `<span class="badge badge-muted">sin dato</span>`;
    const tone = STATUS_TONE[status] || "muted";
    return `<span class="badge badge-${tone}">${escapeHtml(status)}</span>`;
}

function formToObject(form) {
    return Object.fromEntries(new FormData(form).entries());
}

/** Cliente HTTP mínimo: siempre propaga X-Correlation-Id y, si se indica, Idempotency-Key. */
async function apiCall(method, path, body, idempotencyKey) {
    const headers = { "X-Correlation-Id": CORRELATION_ID };
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (idempotencyKey) headers["Idempotency-Key"] = idempotencyKey;
    const response = await fetch(path, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        const error = new Error((data && data.message) || `HTTP ${response.status}`);
        error.status = response.status;
        error.body = data;
        throw error;
    }
    return data;
}

function renderList(container, items, renderItem, emptyMessage) {
    if (!items || items.length === 0) {
        container.innerHTML = `<p class="empty-state">${escapeHtml(emptyMessage)}</p>`;
        return;
    }
    container.innerHTML = items.map(renderItem).join("");
}

// ---------------------------------------------------------------------
// Estado del backend (Actuator / platform status)
// ---------------------------------------------------------------------

async function checkBackendStatus() {
    const dot = el("backend-status-dot");
    const text = el("backend-status-text");
    try {
        const status = await apiCall("GET", "/api/v1/platform/status");
        dot.className = "status-dot up";
        text.textContent = `Backend activo (${status.applicationName})`;
    } catch {
        dot.className = "status-dot down";
        text.textContent = "Backend no responde";
    }
}

// ---------------------------------------------------------------------
// Selector de orden
// ---------------------------------------------------------------------

function isValidUuid(value) {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value.trim());
}

async function loadOrder(orderId) {
    if (!isValidUuid(orderId)) {
        toast("Ese Order ID no parece un UUID válido.", "error");
        return;
    }
    currentOrderId = orderId.trim();
    localStorage.setItem(ORDER_ID_STORAGE_KEY, currentOrderId);
    el("dashboard").classList.remove("hidden");
    await refreshAll();
}

el("load-order-btn").addEventListener("click", () => loadOrder(el("order-id-input").value));
el("order-id-input").addEventListener("keydown", (event) => {
    if (event.key === "Enter") loadOrder(el("order-id-input").value);
});
el("new-order-btn").addEventListener("click", () => {
    const id = crypto.randomUUID();
    el("order-id-input").value = id;
    loadOrder(id);
});

function refreshAll() {
    return Promise.all([refreshSummary(), refreshDispatch(), refreshNotifications(), refreshIncidents()]);
}

// ---------------------------------------------------------------------
// QRY-01 — Resumen consolidado
// ---------------------------------------------------------------------

const SUMMARY_FIELDS = [
    ["orderStatus", "Compra"],
    ["paymentStatus", "Pago"],
    ["dispatchStatus", "Despacho"],
    ["deliveryStatus", "Entrega"],
    ["incidentStatus", "Incidencia"],
];

async function refreshSummary() {
    const body = el("summary-body");
    if (!currentOrderId) return;
    try {
        const summary = await apiCall("GET", `/api/v1/orders/${encodeURIComponent(currentOrderId)}/summary`);
        const tiles = SUMMARY_FIELDS.map(([key, label]) => {
            const field = summary[key];
            const value = field && field.status !== null ? escapeHtml(field.status) : "Sin dato";
            const staleBadge = field && field.status !== null
                ? (field.stale
                    ? '<span class="badge badge-stale">desactualizado</span>'
                    : '<span class="badge badge-ok">al día</span>')
                : "";
            return `
                <div class="summary-tile">
                    <div class="summary-tile-label">${label}</div>
                    <div class="summary-tile-value">${value}</div>
                    ${staleBadge}
                </div>`;
        }).join("");
        body.innerHTML = `<div class="summary-grid">${tiles}</div>`;
    } catch (error) {
        if (error.status === 404) {
            body.innerHTML = `<p class="empty-state">
                Sin resumen todavía para esta orden. Esto se llenará automáticamente cuando exista un
                consumidor del Outbox (BB-04); mientras tanto puedes usar "Alimentar resumen manualmente"
                abajo.
            </p>`;
        } else {
            body.innerHTML = `<p class="empty-state">Error al consultar el resumen: ${escapeHtml(error.message)}</p>`;
        }
    }
}

el("summary-ingest-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const { source, status } = formToObject(event.target);
    try {
        await apiCall("POST", "/api/v1/query/order-summary", { orderId: currentOrderId, source, status });
        toast("Resumen actualizado.", "ok");
        event.target.reset();
        await refreshSummary();
    } catch (error) {
        toast(`No se pudo actualizar el resumen: ${error.message}`, "error");
    }
});

document.querySelector('[data-action="refresh-summary"]').addEventListener("click", refreshSummary);

// ---------------------------------------------------------------------
// LOG-01 / LOG-02 / LOG-03 — Despacho
// ---------------------------------------------------------------------

const DISPATCH_ORIGIN_LABEL = {
    DISTRIBUTION_CENTER: "Centro de distribución",
    STORE: "Tienda",
    CUSTOMER_PICKUP: "Recogida por el cliente",
};

async function refreshDispatch() {
    if (!currentOrderId) return;
    const container = el("dispatch-list");
    try {
        const shipments = await apiCall("GET", `/api/v1/shipments?orderId=${encodeURIComponent(currentOrderId)}`);
        renderList(container, shipments, renderShipmentRow, "Sin despachos todavía para esta orden.");
    } catch (error) {
        container.innerHTML = `<p class="empty-state">Error al listar despachos: ${escapeHtml(error.message)}</p>`;
    }
}

function renderShipmentRow(shipment) {
    const originLabel = DISPATCH_ORIGIN_LABEL[shipment.origin] || shipment.origin;
    const tracking = shipment.trackingNumber
        ? `Guía: <strong>${escapeHtml(shipment.trackingNumber)}</strong> · `
        : "";
    return `
        <div class="item-row" data-shipment-id="${shipment.shipmentId}">
            <div class="item-row-head">
                <div>
                    <div class="item-row-title">${escapeHtml(originLabel)} → ${escapeHtml(shipment.destination)}</div>
                    <div class="item-row-meta">${tracking}Creado ${formatDate(shipment.createdAt)} · intentos de guía: ${shipment.labelAttempts}</div>
                </div>
                ${badge(shipment.status)}
            </div>
            <div class="item-row-actions">
                <button class="btn btn-ghost btn-sm" data-shipment-action="tracking" data-shipment-id="${shipment.shipmentId}">Ver tracking</button>
                <button class="btn btn-ghost btn-sm" data-shipment-action="in-transit" data-shipment-id="${shipment.shipmentId}">Marcar en tránsito</button>
                <button class="btn btn-ghost btn-sm" data-shipment-action="delivered" data-shipment-id="${shipment.shipmentId}">Marcar entregado</button>
            </div>
            <div class="timeline hidden" data-timeline-for="${shipment.shipmentId}"></div>
        </div>`;
}

el("dispatch-list").addEventListener("click", async (event) => {
    const button = event.target.closest("[data-shipment-action]");
    if (!button) return;
    const shipmentId = button.dataset.shipmentId;
    const action = button.dataset.shipmentAction;
    try {
        if (action === "tracking") {
            const timeline = document.querySelector(`[data-timeline-for="${shipmentId}"]`);
            const isHidden = timeline.classList.contains("hidden");
            if (isHidden) {
                const events = await apiCall("GET", `/api/v1/shipments/${shipmentId}/tracking-events`);
                timeline.innerHTML = events.length
                    ? events.map((e) => `<div class="timeline-item"><strong>${badge(e.status)}</strong> ${formatDate(e.occurredAt)} · ${escapeHtml(e.source)}</div>`).join("")
                    : `<div class="timeline-item">Sin eventos de tracking todavía.</div>`;
            }
            timeline.classList.toggle("hidden");
        } else if (action === "in-transit" || action === "delivered") {
            const status = action === "in-transit" ? "IN_TRANSIT" : "DELIVERED";
            await apiCall("POST", `/api/v1/shipments/${shipmentId}/tracking-events`, { status });
            toast("Evento de tracking registrado.", "ok");
            await refreshDispatch();
        }
    } catch (error) {
        toast(`No se pudo completar la acción: ${error.message}`, "error");
    }
});

el("dispatch-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const values = formToObject(event.target);
    try {
        await apiCall(
            "POST",
            "/api/v1/shipments",
            { orderId: currentOrderId, origin: values.origin, destination: values.destination },
            crypto.randomUUID()
        );
        toast("Despacho creado.", "ok");
        event.target.reset();
        event.target.closest("details").open = false;
        await refreshDispatch();
    } catch (error) {
        toast(`No se pudo crear el despacho: ${error.message}`, "error");
    }
});

document.querySelector('[data-action="refresh-dispatch"]').addEventListener("click", refreshDispatch);

// ---------------------------------------------------------------------
// NOT-01 — Notificaciones
// ---------------------------------------------------------------------

const NOTIFICATION_EVENT_LABEL = {
    ORDER_CONFIRMED: "Confirmación de compra",
    SHIPMENT_PREPARING: "Preparación",
    SHIPMENT_IN_TRANSIT: "Tránsito",
    SHIPMENT_DELIVERED: "Entrega",
    INCIDENT_REGISTERED: "Incidencia",
};

async function refreshNotifications() {
    if (!currentOrderId) return;
    const container = el("notifications-list");
    try {
        const notifications = await apiCall(
            "GET", `/api/v1/notifications?sourceAggregateId=${encodeURIComponent(currentOrderId)}`);
        renderList(container, notifications, renderNotificationRow, "Sin notificaciones todavía para esta orden.");
    } catch (error) {
        container.innerHTML = `<p class="empty-state">Error al listar notificaciones: ${escapeHtml(error.message)}</p>`;
    }
}

function renderNotificationRow(notification) {
    const eventLabel = NOTIFICATION_EVENT_LABEL[notification.eventType] || notification.eventType;
    return `
        <div class="item-row">
            <div class="item-row-head">
                <div>
                    <div class="item-row-title">${escapeHtml(eventLabel)} · ${escapeHtml(notification.channel)}</div>
                    <div class="item-row-meta">Para ${escapeHtml(notification.recipient)} · intentos: ${notification.attempts} · ${formatDate(notification.createdAt)}</div>
                </div>
                ${badge(notification.status)}
            </div>
        </div>`;
}

el("notification-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const values = formToObject(event.target);
    try {
        await apiCall("POST", "/api/v1/notifications", { ...values, sourceAggregateId: currentOrderId });
        toast("Notificación encolada (202).", "ok");
        event.target.reset();
        event.target.closest("details").open = false;
        await refreshNotifications();
    } catch (error) {
        toast(`No se pudo enviar la notificación: ${error.message}`, "error");
    }
});

document.querySelector('[data-action="refresh-notifications"]').addEventListener("click", refreshNotifications);

// ---------------------------------------------------------------------
// INC-01 — Devoluciones / incidencias
// ---------------------------------------------------------------------

const INCIDENT_TYPE_LABEL = { RETURN: "Devolución", INCIDENT: "Incidencia" };

async function refreshIncidents() {
    if (!currentOrderId) return;
    const container = el("incidents-list");
    try {
        const incidents = await apiCall("GET", `/api/v1/incidents?orderId=${encodeURIComponent(currentOrderId)}`);
        renderList(container, incidents, renderIncidentRow, "Sin devoluciones ni incidencias todavía para esta orden.");
    } catch (error) {
        container.innerHTML = `<p class="empty-state">Error al listar incidencias: ${escapeHtml(error.message)}</p>`;
    }
}

function renderIncidentRow(incident) {
    const typeLabel = INCIDENT_TYPE_LABEL[incident.type] || incident.type;
    const partial = incident.partial ? " · parcial" : "";
    const items = incident.items.map((i) => `${escapeHtml(i.sku)} ×${i.quantity}`).join(", ");
    const canResolve = incident.status === "REGISTERED" || incident.status === "EVALUATING";
    const actions = canResolve
        ? `<div class="item-row-actions">
            <button class="btn btn-ghost btn-sm" data-incident-action="approve" data-incident-id="${incident.id}">Aprobar reembolso</button>
            <button class="btn btn-ghost btn-sm" data-incident-action="reject" data-incident-id="${incident.id}">Rechazar</button>
           </div>`
        : "";
    return `
        <div class="item-row">
            <div class="item-row-head">
                <div>
                    <div class="item-row-title">${escapeHtml(typeLabel)}${partial} — ${escapeHtml(incident.reason)}</div>
                    <div class="item-row-meta">${escapeHtml(items)} · ${formatDate(incident.createdAt)}${incident.refundAmount ? ` · reembolso: ${incident.refundAmount}` : ""}</div>
                </div>
                ${badge(incident.status)}
            </div>
            ${actions}
        </div>`;
}

el("incidents-list").addEventListener("click", async (event) => {
    const button = event.target.closest("[data-incident-action]");
    if (!button) return;
    const incidentId = button.dataset.incidentId;
    const action = button.dataset.incidentAction;
    try {
        if (action === "approve") {
            await apiCall("PATCH", `/api/v1/incidents/${incidentId}/resolution`, { resolution: "REFUNDED", refundAmount: 0 });
            toast("Devolución aprobada y marcada como reembolsada.", "ok");
        } else if (action === "reject") {
            await apiCall("PATCH", `/api/v1/incidents/${incidentId}/resolution`, { resolution: "REJECTED" });
            toast("Devolución rechazada.", "ok");
        }
        await refreshIncidents();
    } catch (error) {
        toast(`No se pudo resolver la incidencia: ${error.message}`, "error");
    }
});

el("incident-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.target;
    const values = formToObject(form);
    const payload = {
        orderId: currentOrderId,
        type: values.type,
        reason: values.reason,
        partial: form.elements.partial.checked,
        items: [{ sku: values.sku, quantity: Number(values.quantity) }],
    };
    try {
        await apiCall("POST", "/api/v1/incidents", payload);
        toast("Incidencia registrada.", "ok");
        form.reset();
        form.closest("details").open = false;
        await refreshIncidents();
    } catch (error) {
        toast(`No se pudo registrar la incidencia: ${error.message}`, "error");
    }
});

document.querySelector('[data-action="refresh-incidents"]').addEventListener("click", refreshIncidents);

// ---------------------------------------------------------------------
// Arranque
// ---------------------------------------------------------------------

checkBackendStatus();

const savedOrderId = localStorage.getItem(ORDER_ID_STORAGE_KEY);
if (savedOrderId) {
    el("order-id-input").value = savedOrderId;
    loadOrder(savedOrderId);
}
