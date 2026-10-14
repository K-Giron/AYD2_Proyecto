"use strict";

/** Centro de operaciones interno conectado a los módulos reales del prototipo. */

// sgoRequireSession redirige a login.html si no hay sesión de personal interno;
// la redirección es asíncrona, así que se protege el resto contra `session === null`.
const session = sgoRequireAnySession(["STAFF", "FINANCE", "SAT_AUDITOR", "ONLINE_STORE"]);
// La cuenta de la tienda online administra la web completa: también ve finanzas y auditoría.
const canAudit = () => ["FINANCE", "SAT_AUDITOR"].includes(session?.role) || (session?.backendRoles || []).includes("FINANCE");
const auditRole = () => (session?.backendRoles || []).includes("FINANCE") ? "FINANCE" : session?.role;

const CORRELATION_ID = crypto.randomUUID();
document.getElementById("correlation-id").textContent = CORRELATION_ID;
if (session) {
    document.getElementById("session-name").textContent = session.name;
    document.getElementById("online-admin-link").classList.toggle("hidden", session.role !== "ONLINE_STORE");
    document.getElementById("logout-btn").addEventListener("click", sgoLogout);
}

const ORDER_ID_STORAGE_KEY = "sgo-logic.lastOrderId";
let currentOrderId = null;
let currentAuditEntries = [];

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
    if (!iso) return "Sin fecha";
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
async function apiCall(method, path, body, idempotencyKey, additionalHeaders) {
    const headers = { "X-Correlation-Id": CORRELATION_ID, ...sgoAuthHeaders(), ...(additionalHeaders || {}) };
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (idempotencyKey) headers["Idempotency-Key"] = idempotencyKey;
    const response = await fetch(path, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    const text = await response.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = { message: text }; }
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
        document.querySelectorAll(".integration-strip .status-dot").forEach(node => { node.className = "status-dot up"; });
    } catch {
        dot.className = "status-dot down";
        text.textContent = "Backend no responde";
        document.querySelectorAll(".integration-strip .status-dot").forEach(node => { node.className = "status-dot down"; });
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

function money(value) {
    return new Intl.NumberFormat("es-GT", { style: "currency", currency: "GTQ" }).format(Number(value || 0));
}

const ATTENTION_STATUSES = new Set(["PAYMENT_REJECTED", "PAYMENT_INDETERMINATE", "FISCAL_REJECTED", "CANCELLED"]);
const CONFIRMED_STATUSES = new Set(["CONFIRMED", "PREPARING", "IN_TRANSIT", "DELIVERED"]);

async function loadOperationsOverview() {
    const container = el("operations-orders");
    try {
        const orders = await apiCall("GET", "/api/v1/orders");
        el("metric-orders").textContent = orders.length;
        el("metric-confirmed").textContent = orders.filter(order => CONFIRMED_STATUSES.has(order.status)).length;
        el("metric-attention").textContent = orders.filter(order => ATTENTION_STATUSES.has(order.status)).length;
        el("metric-revenue").textContent = money(orders.reduce((sum, order) => sum + Number(order.total || 0), 0));
        container.innerHTML = orders.length ? `<div class="operations-table" role="table">
            <div class="operations-row operations-row-head" role="row"><span>Orden</span><span>Canal</span><span>Fecha</span><span>Total</span><span>Estado</span><span></span></div>
            ${orders.slice(0, 12).map(order => `<div class="operations-row" role="row">
                <span data-label="Orden"><strong>${escapeHtml(order.id.slice(0, 8))}</strong><small>${escapeHtml(order.customerId.slice(0, 8))}</small></span>
                <span data-label="Canal">${escapeHtml(order.channel)}</span>
                <span data-label="Fecha">${formatDate(order.createdAt)}</span>
                <span data-label="Total">${money(order.total)}</span>
                <span data-label="Estado">${badge(order.status)}</span>
                <span><button class="btn btn-ghost btn-sm" data-admin-order="${order.id}">Gestionar</button></span>
            </div>`).join("")}</div>` : `<p class="empty-state">Todavía no hay órdenes. Las compras realizadas desde la tienda aparecerán aquí.</p>`;
    } catch (error) {
        container.innerHTML = `<p class="empty-state">No fue posible cargar la operación: ${escapeHtml(error.message)}</p>`;
    }
}

el("operations-orders").addEventListener("click", event => {
    const button = event.target.closest("[data-admin-order]");
    if (!button) return;
    el("order-id-input").value = button.dataset.adminOrder;
    loadOrder(button.dataset.adminOrder);
    el("order-id-input").scrollIntoView({ behavior: "smooth", block: "center" });
});
el("refresh-operations-btn").addEventListener("click", loadOperationsOverview);

function refreshAll() {
    return Promise.all([refreshSummary(), refreshDispatch(), refreshNotifications(), refreshIncidents(), refreshAuditHistory(), refreshLegacyForOrder()]);
}

async function transitionOrderStatus(status, reason) {
    return apiCall("PATCH", `/api/v1/orders/${currentOrderId}/status`, {
        status,
        actor: `staff:${session?.email || "internal"}`,
        reason,
    });
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
        const [orderResult, paymentsResult, shipmentsResult, incidentsResult] = await Promise.allSettled([
            apiCall("GET", `/api/v1/orders/${encodeURIComponent(currentOrderId)}`),
            apiCall("GET", `/api/v1/payments/attempts?orderId=${encodeURIComponent(currentOrderId)}`),
            apiCall("GET", `/api/v1/shipments?orderId=${encodeURIComponent(currentOrderId)}`),
            apiCall("GET", `/api/v1/incidents?orderId=${encodeURIComponent(currentOrderId)}`),
        ]);
        if (orderResult.status === "rejected") throw orderResult.reason;
        const order = orderResult.value;
        const payments = paymentsResult.status === "fulfilled" ? paymentsResult.value : [];
        const shipments = shipmentsResult.status === "fulfilled" ? shipmentsResult.value : [];
        const incidents = incidentsResult.status === "fulfilled" ? incidentsResult.value : [];
        const latestShipment = shipments[0];
        const values = {
            orderStatus: order.status,
            paymentStatus: payments[0]?.status,
            dispatchStatus: latestShipment?.status,
            deliveryStatus: latestShipment?.status === "DELIVERED" ? "DELIVERED" : null,
            incidentStatus: incidents[0]?.status,
        };
        const tiles = SUMMARY_FIELDS.map(([key, label]) => {
            const value = values[key] ? escapeHtml(values[key]) : "Sin dato";
            return `
                <div class="summary-tile">
                    <div class="summary-tile-label">${label}</div>
                    <div class="summary-tile-value">${value}</div>
                    ${values[key] ? badge(values[key]) : '<span class="badge badge-muted">pendiente</span>'}
                </div>`;
        }).join("");
        body.innerHTML = `<div class="order-context"><span>Orden ${escapeHtml(order.id)}</span><strong>${money(order.total)}</strong></div><div class="summary-grid">${tiles}</div>`;
    } catch (error) {
        if (error.status === 404) {
            body.innerHTML = `<p class="empty-state">No existe una orden con ese identificador.</p>`;
        } else {
            body.innerHTML = `<p class="empty-state">Error al consultar el resumen: ${escapeHtml(error.message)}</p>`;
        }
    }
}

document.querySelector('[data-action="refresh-summary"]').addEventListener("click", refreshSummary);

// ---------------------------------------------------------------------
// LOG-01 / LOG-02 / LOG-03 — Despacho
// ---------------------------------------------------------------------

const WAREHOUSE_LABEL = { "CD-WEB": "Almacén Tienda Online (CD-WEB)" };

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

// Portal de operación de LogiExpress (sistema del transportista): ahí se recolecta y se entrega.
const LOGIEXPRESS_PORTAL = `${location.protocol}//${location.hostname}:8085/`;

/** El avance del despacho lo reporta LogiExpress (o la tienda de retiro); la consola solo lo muestra. */
function shipmentProgressHint(shipment) {
    if (!shipment.trackingNumber) {
        return shipment.status === "LABEL_FAILED"
            ? "LogiExpress no respondió tras los reintentos (2/8/32 s). Si emitió la guía, el rastreo la asociará sola; si estuvo caído, usa “Reintentar guía”."
            : "Solicitando la guía a LogiExpress…";
    }
    return {
        LABEL_ISSUED: "Guía emitida: LogiExpress la recolecta en el origen y el estado se actualiza solo.",
        IN_TRANSIT: "En camino con LogiExpress; la entrega se registra en su sistema.",
        READY_FOR_PICKUP: "LogiExpress lo dejó en la tienda; la tienda lo entrega al cliente desde su sistema.",
        DELIVERED: "Entregado.",
    }[shipment.status] || "";
}

function renderShipmentRow(shipment) {
    // Los despachos de la tienda online salen de su almacén (CD-WEB), que la Saga registra.
    const originLabel = (DISPATCH_ORIGIN_LABEL[shipment.origin] || shipment.origin)
        + (shipment.originCode ? ` · ${WAREHOUSE_LABEL[shipment.originCode] || shipment.originCode}` : "");
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
                ${shipment.trackingNumber ? `<a class="btn btn-ghost btn-sm" href="${LOGIEXPRESS_PORTAL}" target="_blank" rel="noopener">Abrir en LogiExpress</a>` : ""}
                ${shipment.status === "LABEL_FAILED" && !shipment.trackingNumber ? `<button class="btn btn-primary btn-sm" data-shipment-action="label-retry" data-shipment-id="${shipment.shipmentId}">Reintentar guía</button>` : ""}
            </div>
            <p class="integration-help">${shipmentProgressHint(shipment)}</p>
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
        } else if (action === "label-retry") {
            await apiCall("POST", `/api/v1/shipments/${shipmentId}/label-retry`);
            toast("Guía solicitada de nuevo a LogiExpress.", "ok");
            setTimeout(refreshDispatch, 3000);
        }
    } catch (error) {
        toast(`No se pudo completar la acción: ${error.message}`, "error");
    }
});

el("dispatch-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const values = formToObject(event.target);
    try {
        const order = await apiCall("GET", `/api/v1/orders/${currentOrderId}`);
        if (order.status !== "CONFIRMED" && order.status !== "PREPARING") {
            throw new Error(`La orden debe estar CONFIRMED para iniciar despacho; estado actual: ${order.status}.`);
        }
        await apiCall(
            "POST",
            "/api/v1/shipments",
            { orderId: currentOrderId, origin: values.origin, destination: values.destination },
            crypto.randomUUID()
        );
        if (order.status === "CONFIRMED") {
            await transitionOrderStatus("PREPARING", "Despacho creado y enviado a LogiExpress");
        }
        toast("Despacho creado.", "ok");
        event.target.reset();
        event.target.closest("details").open = false;
        await Promise.all([refreshDispatch(), refreshSummary(), loadOperationsOverview()]);
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
// ERP / POS y evidencia de auditoría
// ---------------------------------------------------------------------

async function refreshAuditHistory() {
    if (!currentOrderId) return;
    const container = el("audit-list");
    if (!canAudit()) {
        currentAuditEntries = [];
        container.innerHTML = `<p class="empty-state">La evidencia transversal requiere una sesión de Finanzas o Auditor SAT. El personal operativo conserva acceso al historial básico de la orden.</p>`;
        return;
    }
    try {
        currentAuditEntries = await apiCall("GET",
            `/api/v1/audit/events?orderId=${encodeURIComponent(currentOrderId)}`,
            undefined, undefined,
            { "X-Actor-Id": session.email, "X-Actor-Role": auditRole() });
        // La API devuelve actor y estados como texto JSON (beforeState/afterState).
        const state = value => { try { return typeof value === "string" ? JSON.parse(value) : (value || {}); } catch { return {}; } };
        renderList(container, currentAuditEntries, entry => `<div class="item-row audit-entry">
            <div class="item-row-head"><div><div class="item-row-title">${escapeHtml(entry.action)}</div>
            <div class="item-row-meta">${escapeHtml(entry.actor || entry.actorId || "sistema")} · ${formatDate(entry.occurredAt)}</div></div>${badge(entry.result)}</div>
            <p>${escapeHtml(state(entry.beforeState).status || "INICIO")} → ${escapeHtml(state(entry.afterState).status || "SIN CAMBIO")}${state(entry.metadata).reason ? " · " + escapeHtml(state(entry.metadata).reason) : ""}</p>
            <code>correlationId=${escapeHtml(entry.correlationId)}${entry.evidenceHash ? " · hash=" + escapeHtml(entry.evidenceHash) : ""}</code>
        </div>`, "No existe evidencia para esta orden.");
    } catch (error) {
        currentAuditEntries = [];
        container.innerHTML = `<p class="empty-state">No fue posible recuperar la evidencia: ${escapeHtml(error.message)}</p>`;
    }
}

el("export-audit-btn").addEventListener("click", async () => {
    if (!currentOrderId) { toast("Carga una orden antes de exportar.", "error"); return; }
    if (!canAudit()) {
        toast("La exportación requiere una sesión de Finanzas o Auditor SAT.", "error");
        return;
    }
    try {
        const entries = await apiCall("GET",
            `/api/v1/audit/exports?orderId=${encodeURIComponent(currentOrderId)}`,
            undefined, undefined,
            { "X-Actor-Id": session.email, "X-Actor-Role": auditRole() });
        const payload = { orderId: currentOrderId, exportedAt: new Date().toISOString(), correlationId: CORRELATION_ID, entries };
        const url = URL.createObjectURL(new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" }));
        const link = document.createElement("a");
        link.href = url; link.download = `auditoria-${currentOrderId}.json`; link.click();
        URL.revokeObjectURL(url);
        toast("Evidencia exportada y acceso registrado.", "ok");
    } catch (error) {
        toast(`No se pudo exportar la evidencia: ${error.message}`, "error");
    }
});

// ---------------------------------------------------------------------
// Sistemas legados: ERP AS/400 (lotes CSV/XML) y POS de las 32 tiendas
// ---------------------------------------------------------------------

const hasBackendRole = role => (session?.backendRoles || []).includes(role) || session?.role === role;
const BATCH_TONE = { DELIVERED: "ok", PENDING: "warn", RETRY_SCHEDULED: "warn", DEAD_LETTER: "error", REJECTED: "error" };
const POS_TONE = { ACKNOWLEDGED: "ok", PENDING: "warn", RETRY_SCHEDULED: "warn", DEAD_LETTER: "error", REJECTED: "error" };
const CHANNEL_LABEL = { WEB: "Web", MOBILE: "Móvil", CALL_CENTER: "Call center", STORE: "Tienda física" };

/** Descarga con el token de la sesión (un <a href> no lleva el encabezado Authorization). */
async function downloadAuthenticated(path, fallbackName) {
    const response = await fetch(path, { headers: { "X-Correlation-Id": CORRELATION_ID, ...sgoAuthHeaders() } });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const disposition = response.headers.get("Content-Disposition") || "";
    const name = (disposition.match(/filename="?([^";]+)"?/) || [])[1] || fallbackName;
    const url = URL.createObjectURL(await response.blob());
    const link = document.createElement("a");
    link.href = url; link.download = name; link.click();
    URL.revokeObjectURL(url);
}

function summarizePos(deliveries) {
    const counts = {};
    deliveries.forEach(d => { counts[d.status] = (counts[d.status] || 0) + 1; });
    return Object.entries(counts).map(([status, n]) => toneBadge(POS_TONE[status] || "muted", `${n} ${status}`)).join(" ");
}

/** Tarjeta de la orden: su venta en el ERP y los avisos a las tiendas. */
async function refreshLegacyForOrder() {
    const box = el("legacy-result");
    if (!currentOrderId) return;
    const parts = [];
    if (hasBackendRole("FINANCE")) {
        try {
            const sale = await apiCall("GET", `/api/v1/integrations/erp/sales/${currentOrderId}`);
            parts.push(`<div class="integration-success"><strong>ERP: ${erpSaleBadge(sale.status)}</strong>
                <span>${money(sale.grandTotal)} (neto ${money(sale.netTotal)} + IVA ${money(sale.taxTotal)})${sale.batchId ? ` · lote ${escapeHtml(sale.batchId.slice(0, 8))} ${escapeHtml(sale.batchStatus || "")}` : ""}</span></div>`);
        } catch (error) {
            parts.push(`<p class="empty-state">${error.status === 404 ? "ERP: la orden todavía no está confirmada (solo las ventas confirmadas van al ERP)." : `ERP: ${escapeHtml(error.message)}`}</p>`);
        }
    }
    if (hasBackendRole("LOGISTICS")) {
        try {
            const deliveries = await apiCall("GET", `/api/v1/integrations/pos/deliveries?orderId=${currentOrderId}`);
            const byType = {};
            deliveries.forEach(d => { (byType[d.eventType] ||= []).push(d); });
            parts.push(Object.keys(byType).length
                ? Object.entries(byType).map(([type, list]) => `<div class="integration-success"><strong>POS ${escapeHtml(type)} → ${list.length} tiendas</strong><span>${summarizePos(list)}</span></div>`).join("")
                : `<p class="empty-state">POS: aún no hay eventos de esta orden para las tiendas.</p>`);
        } catch (error) { parts.push(`<p class="empty-state">POS: ${escapeHtml(error.message)}</p>`); }
    }
    box.innerHTML = parts.join("") || `<p class="empty-state">Tu rol no consulta ERP ni POS.</p>`;
}

async function loadLegacyPanel() {
    const batchesBox = el("erp-batches");
    const posBox = el("pos-events");
    if (hasBackendRole("FINANCE")) {
        try {
            const batches = await apiCall("GET", "/api/v1/integrations/erp/batches");
            batchesBox.innerHTML = batches.length ? `<div class="operations-table" role="table">
                <div class="operations-row operations-row-head legacy-row" role="row"><span>Lote</span><span>Fecha</span><span>Ventas</span><span>Estado</span><span>Archivo</span></div>
                ${batches.map(b => `<div class="operations-row legacy-row" role="row">
                    <span data-label="Lote"><strong>${escapeHtml(b.id.slice(0, 8))}</strong><small>${escapeHtml(b.format)} · intento ${b.attempts}</small></span>
                    <span data-label="Fecha">${formatDate(b.createdAt)}</span>
                    <span data-label="Ventas">${b.saleCount}</span>
                    <span data-label="Estado">${toneBadge(BATCH_TONE[b.status] || "muted", b.status)}${b.failureMessage ? `<small>${escapeHtml(b.failureMessage)}</small>` : ""}</span>
                    <span>${b.payload ? `<button class="btn btn-ghost btn-sm" data-batch-file="${b.id}">${escapeHtml((b.acknowledgement || "").replace("ERP-FILE:", "") || "Descargar")}</button>` : "<small>Ventana sin ventas</small>"}${b.status === "DEAD_LETTER" ? `<button class="btn btn-primary btn-sm" data-batch-retry="${b.id}">Reintentar</button>` : ""}</span>
                </div>`).join("")}</div>` : `<p class="empty-state">Aún no hay lotes. El primero sale en el siguiente ciclo automático o con "Generar lote ERP ahora".</p>`;
        } catch (error) { batchesBox.innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`; }
    } else {
        batchesBox.innerHTML = `<p class="empty-state">Solo Finanzas ve los lotes del ERP.</p>`;
        el("erp-run-btn").classList.add("hidden");
    }
    if (hasBackendRole("LOGISTICS")) {
        try {
            const deliveries = await apiCall("GET", "/api/v1/integrations/pos/deliveries");
            const byEvent = new Map();
            deliveries.forEach(d => { if (!byEvent.has(d.eventId)) byEvent.set(d.eventId, []); byEvent.get(d.eventId).push(d); });
            posBox.innerHTML = byEvent.size ? `<div class="operations-table" role="table">
                <div class="operations-row operations-row-head legacy-row" role="row"><span>Orden</span><span>Evento</span><span>Fecha</span><span>Tiendas</span><span>Tienda 01</span></div>
                ${[...byEvent.values()].slice(0, 15).map(list => {
                    const first = list[0];
                    const store1 = list.find(d => d.storeNumber === 1);
                    return `<div class="operations-row legacy-row" role="row">
                        <span data-label="Orden"><strong>${escapeHtml(first.orderId.slice(0, 8))}</strong></span>
                        <span data-label="Evento">${escapeHtml(first.eventType)}</span>
                        <span data-label="Fecha">${formatDate(first.createdAt)}</span>
                        <span data-label="Tiendas">${summarizePos(list)}</span>
                        <span data-label="Tienda 01">${store1 ? toneBadge(POS_TONE[store1.status] || "muted", store1.status) + `<small>${escapeHtml(store1.acknowledgement || store1.failureMessage || "")}</small>` : "—"}</span>
                    </div>`;
                }).join("")}</div>` : `<p class="empty-state">Aún no hay eventos. Se generan solos cuando una orden se confirma, prepara, envía, entrega o cancela.</p>`;
        } catch (error) { posBox.innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`; }
    } else {
        posBox.innerHTML = `<p class="empty-state">Solo Logística ve los eventos a los POS.</p>`;
    }
}

el("erp-run-btn").addEventListener("click", async () => {
    try {
        const batch = await apiCall("POST", "/api/v1/integrations/erp/batches?format=CSV");
        toast(batch.saleCount ? `Lote ${batch.id.slice(0, 8)}: ${batch.saleCount} ventas, estado ${batch.status}.` : "No había ventas pendientes para el ERP.", batch.status === "DEAD_LETTER" ? "error" : "ok");
        await loadLegacyPanel();
    } catch (error) { toast(error.message, "error"); }
});
el("legacy-refresh-btn").addEventListener("click", loadLegacyPanel);
el("erp-batches").addEventListener("click", async event => {
    const retry = event.target.closest("[data-batch-retry]");
    if (retry) {
        try {
            await apiCall("POST", `/api/v1/integrations/erp/batches/${retry.dataset.batchRetry}/retry`);
            toast("Lote reenviado al ERP.", "ok");
            setTimeout(loadLegacyPanel, 2500);
        } catch (error) { toast(error.message, "error"); }
        return;
    }
    const button = event.target.closest("[data-batch-file]");
    if (!button) return;
    try { await downloadAuthenticated(`/api/v1/integrations/erp/batches/${button.dataset.batchFile}/file`, "lote-erp.csv"); }
    catch (error) { toast(`No se pudo descargar: ${error.message}`, "error"); }
});

// ---------------------------------------------------------------------
// Informe de ventas conciliado (Finanzas)
// ---------------------------------------------------------------------

const ERP_SALE_LABEL = { DELIVERED: ["ok", "Entregada al ERP"], BATCHED: ["warn", "En lote, reintentando"], PENDING: ["warn", "En cola"], SIN_REGISTRAR: ["error", "Sin registrar"] };
const erpSaleBadge = status => toneBadge(...(ERP_SALE_LABEL[status] || ["muted", status || "—"]));

function guatemalaToday() {
    return new Intl.DateTimeFormat("en-CA", { timeZone: "America/Guatemala" }).format(new Date());
}

async function generateSalesReport() {
    const box = el("sales-report");
    const from = el("report-from").value, to = el("report-to").value;
    box.innerHTML = `<p class="empty-state">Generando…</p>`;
    try {
        const report = await apiCall("GET", `/api/v1/reports/sales?from=${from}&to=${to}`);
        const s = report.summary;
        box.innerHTML = `
            <div class="report-summary">
                <div><span>Ventas</span><strong>${s.sales}</strong></div>
                <div><span>Total</span><strong>${money(s.total)}</strong></div>
                <div><span>Neto / IVA</span><strong>${money(s.net)} / ${money(s.tax)}</strong></div>
                <div><span>Entregado al ERP</span><strong>${money(s.deliveredToErp)}</strong><small>${money(s.pendingErp)} por entregar</small></div>
                <div class="${s.reconciled ? "report-ok" : "report-bad"}"><span>Conciliación</span><strong>${s.reconciled ? "Cuadra" : `${s.discrepancies} diferencias`}</strong></div>
            </div>
            <p class="integration-help">Por canal: ${report.byChannel.map(c => `${escapeHtml(CHANNEL_LABEL[c.channel] || c.channel)} ${c.sales} (${money(c.total)})`).join(" · ") || "sin ventas"} · Informe ${escapeHtml(report.reportId.slice(0, 8))} generado por ${escapeHtml(report.generatedBy)} y registrado en auditoría.</p>
            ${report.rows.length ? `<div class="operations-table" role="table">
                <div class="operations-row operations-row-head report-row" role="row"><span>Venta</span><span>Canal</span><span>Total</span><span>Pago</span><span>Factura</span><span>ERP</span><span>Conciliación</span></div>
                ${report.rows.map(r => `<div class="operations-row report-row ${r.discrepancies.length ? "report-row-bad" : ""}" role="row">
                    <span data-label="Venta"><strong>${escapeHtml(r.reference.length > 20 ? r.reference.slice(0, 8) : r.reference)}</strong><small>${formatDate(r.date)}${r.orderStatus ? " · " + escapeHtml(r.orderStatus) : ""}</small></span>
                    <span data-label="Canal">${escapeHtml(CHANNEL_LABEL[r.channel] || r.channel)}<small>${escapeHtml(r.pointOfSale || "")}</small></span>
                    <span data-label="Total">${money(r.total)}</span>
                    <span data-label="Pago"><strong>${r.financialRoute === "OWN_CREDIT" ? "Crédito propio" : r.financialRoute === "STORE" ? "Cobro en tienda" : "PayKlik"}</strong>${r.paymentStatus ? badge(r.paymentStatus) : "—"}<small>${escapeHtml(r.paymentReference || "")}</small></span>
                    <span data-label="Factura">${r.fiscalStatus ? badge(r.fiscalStatus) : "—"}<small>${escapeHtml(r.fiscalDocument || "")}</small></span>
                    <span data-label="ERP">${erpSaleBadge(r.erpStatus)}<small>${escapeHtml(r.erpBatchStatus || "")}</small></span>
                    <span data-label="Conciliación">${r.discrepancies.length ? r.discrepancies.map(d => `<small class="report-issue">${escapeHtml(d)}</small>`).join("") : toneBadge("ok", "Cuadra")}</span>
                </div>`).join("")}</div>` : `<p class="empty-state">No hay ventas en ese rango.</p>`}`;
    } catch (error) {
        box.innerHTML = `<p class="empty-state">No se pudo generar: ${escapeHtml(error.message)}</p>`;
    }
}

if (hasBackendRole("FINANCE")) {
    el("report-from").value = el("report-to").value = guatemalaToday();
    el("sales-report-form").addEventListener("submit", event => { event.preventDefault(); generateSalesReport(); });
    el("report-export-btn").addEventListener("click", async () => {
        try {
            await downloadAuthenticated(`/api/v1/reports/sales/export?from=${el("report-from").value}&to=${el("report-to").value}`, "informe-ventas.csv");
            toast("Informe exportado; la exportación quedó en auditoría.", "ok");
        } catch (error) { toast(`No se pudo exportar: ${error.message}`, "error"); }
    });
} else {
    el("sales-report-panel").classList.add("hidden");
}
if (session) loadLegacyPanel();

// ---------------------------------------------------------------------
// Inventario — entradas y salidas (independiente de una orden)
// ---------------------------------------------------------------------

let productCatalog = [];

// ---------------------------------------------------------------------
// Inventario de la tienda online: existencias y kárdex (CD-WEB)
// ---------------------------------------------------------------------

const KARDEX_RESERVATION = {
    ACTIVE: ["warn", "Reservada (pago en curso)"], CONFIRMED: ["ok", "Vendida"],
    RELEASED: ["muted", "Liberada"], EXPIRED: ["muted", "Vencida por temporizador"],
};
let kardexRows = [];

function toneBadge(tone, label) { return `<span class="badge badge-${tone}">${escapeHtml(label)}</span>`; }

async function loadOnlineInventory() {
    try {
        const [stock, movements] = await Promise.all([
            apiCall("GET", "/api/v1/online-store/inventory"),
            apiCall("GET", "/api/v1/online-store/movements"),
        ]);
        kardexRows = movements;
        el("online-stock-summary").innerHTML = stock.length
            ? `<table class="kardex-grid"><thead><tr><th>Artículo</th><th class="num">En mano</th><th class="num">Reservado</th><th class="num">Disponible</th><th class="num">En tránsito</th></tr></thead><tbody>${stock.map(r => `<tr>
                <td>${escapeHtml(r.name)}<span class="cell-sub">${escapeHtml(r.sku)}</span></td>
                <td class="num">${r.onHand}</td><td class="num">${r.reserved}</td><td class="num"><strong>${r.available}</strong></td>
                <td class="num">${r.inTransit ? "+" + r.inTransit : "—"}</td></tr>`).join("")}</tbody></table>`
            : `<p class="empty-state">La tienda online aún no tiene mercadería.</p>`;
        renderKardex();
    } catch (error) {
        el("online-stock-summary").innerHTML = `<p class="empty-state">No se pudo cargar el inventario: ${escapeHtml(error.message)}</p>`;
        el("kardex-table").innerHTML = "";
    }
}

function renderKardex() {
    const kind = el("kardex-kind-filter").value;
    const text = el("kardex-search").value.trim().toLowerCase();
    const rows = kardexRows
        .filter(r => !kind || r.kind === kind)
        .filter(r => !text || `${r.sku || ""} ${r.name} ${r.orderId || ""}`.toLowerCase().includes(text))
        .map(r => {
            const reservation = KARDEX_RESERVATION[r.status];
            const sign = r.kind === "ENTRADA" ? "+" : "−";
            const detail = r.kind === "RESERVA"
                ? `Orden ${escapeHtml((r.orderId || "").slice(0, 8))}${r.status === "ACTIVE" ? ` · vence ${formatDate(r.expiresAt)}` : ""}`
                : escapeHtml(r.reason || "");
            return `<tr>
                <td>${formatDate(r.occurredAt)}</td>
                <td>${r.kind === "RESERVA" ? toneBadge(reservation ? reservation[0] : "muted", reservation ? reservation[1] : r.status)
                    : `<span class="badge badge-${r.kind === "ENTRADA" ? "ok" : "warn"}">${r.kind === "ENTRADA" ? "Entrada" : "Salida"}</span>`}</td>
                <td>${escapeHtml(r.name)}<span class="cell-sub">${escapeHtml(r.sku || "")}</span></td>
                <td class="num">${r.kind === "RESERVA" ? r.quantity : sign + r.quantity}</td>
                <td>${detail}</td>
            </tr>`;
        });
    el("kardex-table").innerHTML = rows.length
        ? `<table class="kardex-grid"><thead><tr><th>Fecha</th><th>Movimiento</th><th>Artículo</th><th class="num">Cantidad</th><th>Detalle</th></tr></thead><tbody>${rows.join("")}</tbody></table>`
        : `<p class="empty-state">Sin movimientos${kind || text ? " con ese filtro" : ""}.</p>`;
}

el("kardex-kind-filter").addEventListener("change", renderKardex);
el("kardex-search").addEventListener("input", renderKardex);
el("refresh-inventory-btn").addEventListener("click", loadOnlineInventory);

// ---------------------------------------------------------------------
// Atención al cliente (call center) — CDU-01 y consulta de historial.
// Busca al cliente por correo en el directorio de clientes; su carrito es
// el mismo de la web (orders: un solo carrito ACTIVE por cliente) y se
// atiende en canal CALL_CENTER. El historial reúne sus órdenes y atenciones.
// ---------------------------------------------------------------------

let currentCustomer = null;
let currentCustomerCart = null;

el("customer-search-form").addEventListener("submit", async event => {
    event.preventDefault();
    const email = el("customer-email-input").value.trim().toLowerCase();
    if (!email) return;
    el("customer-result").innerHTML = `<p class="empty-state">Buscando ${escapeHtml(email)}…</p>`;
    try {
        currentCustomer = await apiCall("GET", `/api/v1/customers/by-username?username=${encodeURIComponent(email)}`);
    } catch (error) {
        currentCustomer = null;
        el("customer-result").innerHTML = `<p class="empty-state">No hay un cliente registrado con ${escapeHtml(email)}. El cliente se registra en la web (o en la tienda física al cobrarle con su carrito).</p>`;
        return;
    }
    await loadCustomerView();
});

async function loadCustomerView() {
    if (!currentCustomer) return;
    if (!productCatalog.length) productCatalog = await sgoLoadCatalog().catch(() => []);
    const [cart, orders, incidents] = await Promise.all([
        apiCall("GET", `/api/v1/carts/active?customerId=${currentCustomer.id}`).catch(() => null),
        apiCall("GET", `/api/v1/orders?customerId=${currentCustomer.id}`).catch(() => []),
        apiCall("GET", `/api/v1/incidents?customerId=${currentCustomer.id}`).catch(() => []),
    ]);
    currentCustomerCart = cart;
    renderCustomerView(orders, incidents);
}

function renderCustomerView(orders, incidents) {
    const c = currentCustomer;
    const cart = currentCustomerCart;
    const cartItems = cart && cart.items.length
        ? cart.items.map(item => `<div class="track-event"><span>${escapeHtml(item.productName)} × ${item.quantity} · Q${Number(item.lineTotal).toFixed(2)}</span>
            <span class="cart-line-actions">
                <button class="btn btn-ghost btn-sm" data-cart-qty="${item.productId}" data-delta="-1" aria-label="Quitar uno">−</button>
                <button class="btn btn-ghost btn-sm" data-cart-qty="${item.productId}" data-delta="1" aria-label="Agregar uno">+</button>
            </span></div>`).join("")
        : `<p class="empty-state">${cart ? "El carrito está vacío." : "El cliente no tiene carrito activo."}</p>`;
    const options = productCatalog.map(p => `<option value="${escapeHtml(p.id)}" ${p.available > 0 ? "" : "disabled"}>${escapeHtml(p.name)} · Q${Number(p.price).toFixed(2)} · ${p.available} disp.</option>`).join("");
    el("customer-result").innerHTML = `
        <div class="customer-card">
            <div><strong>${escapeHtml(c.fullName)}</strong><span class="cell-sub">${escapeHtml(c.username)} · registrado en ${escapeHtml(c.registeredChannel)} · ${formatDate(c.createdAt)}</span></div>
        </div>
        <div class="customer-columns">
            <div>
                <h3>Carrito ${cart ? `<span class="badge badge-ok">${escapeHtml(cart.status)}</span>` : ""}</h3>
                ${cart ? `<p class="hint">Abierto en ${escapeHtml(cart.originChannel)} · último canal ${escapeHtml(cart.lastChannel)} · ${cart.itemCount} artículo(s) · Q${Number(cart.total).toFixed(2)}</p>` : ""}
                ${cartItems}
                <form id="customer-cart-add-form" class="form-inline compact">
                    <select name="productId" required>${options || `<option value="">No hay productos publicados</option>`}</select>
                    <input type="number" name="quantity" min="1" max="99" value="1" required style="width:5rem" />
                    <button type="submit" class="btn btn-primary btn-sm">${cart ? "Agregar al carrito" : "Abrir carrito y agregar"}</button>
                </form>
                <p class="hint">Lo que agregues aquí aparece en el mismo carrito del cliente en la web; él termina el pago desde su cuenta.</p>
            </div>
            <div>
                <h3>Historial de compras</h3>
                ${orders.length ? orders.map(o => `<div class="track-event"><span><a href="#" data-open-order="${o.id}">${escapeHtml(o.id.slice(0, 8))}</a> · ${formatDate(o.createdAt)} · ${escapeHtml(o.items.map(i => `${i.quantity}× ${i.productName}`).join(", "))} · Q${Number(o.total).toFixed(2)}</span>${badge(o.status)}</div>`).join("") : `<p class="empty-state">Sin compras todavía.</p>`}
                <h3>Atenciones (devoluciones e incidencias)</h3>
                ${incidents.length ? incidents.map(i => `<div class="track-event"><span>${escapeHtml(i.type)} · ${escapeHtml(i.reason || "")} · ${formatDate(i.createdAt)}</span>${badge(i.status)}</div>`).join("") : `<p class="empty-state">Sin atenciones registradas.</p>`}
            </div>
        </div>`;
    el("customer-result").querySelectorAll("[data-cart-qty]").forEach(button => button.addEventListener("click", () =>
        changeCustomerCart(button.dataset.cartQty, Number(button.dataset.delta))));
    el("customer-result").querySelectorAll("[data-open-order]").forEach(link => link.addEventListener("click", event => {
        event.preventDefault();
        el("order-id-input").value = link.dataset.openOrder;
        el("load-order-btn").click();
    }));
    el("customer-cart-add-form").addEventListener("submit", async event => {
        event.preventDefault();
        const values = Object.fromEntries(new FormData(event.currentTarget).entries());
        if (!values.productId) return;
        const current = currentCustomerCart?.items.find(i => i.productId === values.productId)?.quantity || 0;
        await changeCustomerCart(values.productId, Number(values.quantity), current);
    });
}

/** Abre (o recupera) el carrito en canal CALL_CENTER y ajusta la cantidad de un producto. */
async function changeCustomerCart(productId, delta, knownQuantity) {
    const product = productCatalog.find(p => p.id === productId);
    try {
        // Abrir/recuperar en canal CALL_CENTER: es el mismo carrito ACTIVE del cliente y queda
        // registrado que ahora lo atiende el call center (último canal).
        if (!currentCustomerCart || currentCustomerCart.lastChannel !== "CALL_CENTER") {
            currentCustomerCart = await apiCall("POST", "/api/v1/carts",
                { customerId: currentCustomer.id, channel: "CALL_CENTER" }, `callcenter-${currentCustomer.id}-${crypto.randomUUID()}`);
        }
        const line = currentCustomerCart.items.find(i => i.productId === productId);
        const quantity = (knownQuantity ?? line?.quantity ?? 0) + delta;
        currentCustomerCart = quantity <= 0
            ? await apiCall("DELETE", `/api/v1/carts/${currentCustomerCart.id}/items/${productId}`)
            : await apiCall("PUT", `/api/v1/carts/${currentCustomerCart.id}/items/${productId}`, {
                productName: product?.name || line?.productName,
                unitPrice: product?.price ?? line?.unitPrice,
                quantity,
            });
        toast(quantity <= 0 ? "Producto quitado del carrito." : "Carrito actualizado desde call center.", "ok");
        await loadCustomerView();
    } catch (error) {
        toast(error.message, "error");
    }
}

// ---------------------------------------------------------------------
// Arranque
// ---------------------------------------------------------------------

checkBackendStatus();
loadOperationsOverview();
loadOnlineInventory();

const savedOrderId = localStorage.getItem(ORDER_ID_STORAGE_KEY);
if (savedOrderId) {
    el("order-id-input").value = savedOrderId;
    loadOrder(savedOrderId);
}
