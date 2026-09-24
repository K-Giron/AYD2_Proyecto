"use strict";

/** Centro de operaciones interno conectado a los módulos reales del prototipo. */

// sgoRequireSession redirige a login.html si no hay sesión de personal interno;
// la redirección es asíncrona, así que se protege el resto contra `session === null`.
const session = sgoRequireAnySession(["STAFF", "FINANCE", "SAT_AUDITOR"]);

const CORRELATION_ID = crypto.randomUUID();
document.getElementById("correlation-id").textContent = CORRELATION_ID;
if (session) {
    document.getElementById("session-name").textContent = session.name;
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
    const headers = { "X-Correlation-Id": CORRELATION_ID, ...(additionalHeaders || {}) };
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
    return Promise.all([refreshSummary(), refreshDispatch(), refreshNotifications(), refreshIncidents(), refreshAuditHistory()]);
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
            apiCall("GET", `/api/v1/payment-attempts?orderId=${encodeURIComponent(currentOrderId)}`),
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
            const order = await apiCall("GET", `/api/v1/orders/${currentOrderId}`);
            if (status === "IN_TRANSIT" && order.status === "PREPARING") {
                await transitionOrderStatus("IN_TRANSIT", "LogiExpress confirmó la salida del despacho");
            }
            if (status === "DELIVERED") {
                if (order.status === "PREPARING") {
                    await transitionOrderStatus("IN_TRANSIT", "Despacho sincronizado antes de confirmar la entrega");
                }
                if (["PREPARING", "IN_TRANSIT"].includes(order.status)) {
                    await transitionOrderStatus("DELIVERED", "LogiExpress confirmó la entrega al cliente");
                }
            }
            toast("Evento de tracking registrado.", "ok");
            await Promise.all([refreshDispatch(), refreshSummary(), loadOperationsOverview()]);
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
    if (!["FINANCE", "SAT_AUDITOR"].includes(session?.role)) {
        currentAuditEntries = [];
        container.innerHTML = `<p class="empty-state">La evidencia transversal requiere una sesión de Finanzas o Auditor SAT. El personal operativo conserva acceso al historial básico de la orden.</p>`;
        return;
    }
    try {
        currentAuditEntries = await apiCall("GET",
            `/api/v1/audit/events?orderId=${encodeURIComponent(currentOrderId)}`,
            undefined, undefined,
            { "X-Actor-Id": session.email, "X-Actor-Role": session.role });
        renderList(container, currentAuditEntries, entry => `<div class="item-row audit-entry">
            <div class="item-row-head"><div><div class="item-row-title">${escapeHtml(entry.action)}</div>
            <div class="item-row-meta">${escapeHtml(entry.actorId)} · ${formatDate(entry.occurredAt)}</div></div>${badge(entry.result)}</div>
            <p>${escapeHtml(entry.previousState?.status || "INICIO")} → ${escapeHtml(entry.newState?.status || "SIN CAMBIO")} · ${escapeHtml(entry.metadata?.reason || "Sin descripción")}</p>
            <code>correlationId=${escapeHtml(entry.correlationId)} · hash=${escapeHtml(entry.evidenceHash)}</code>
        </div>`, "No existe evidencia para esta orden.");
    } catch (error) {
        currentAuditEntries = [];
        container.innerHTML = `<p class="empty-state">No fue posible recuperar la evidencia: ${escapeHtml(error.message)}</p>`;
    }
}

el("export-audit-btn").addEventListener("click", async () => {
    if (!currentOrderId) { toast("Carga una orden antes de exportar.", "error"); return; }
    if (!["FINANCE", "SAT_AUDITOR"].includes(session?.role)) {
        toast("La exportación requiere una sesión de Finanzas o Auditor SAT.", "error");
        return;
    }
    try {
        const entries = await apiCall("GET",
            `/api/v1/audit/exports?orderId=${encodeURIComponent(currentOrderId)}`,
            undefined, undefined,
            { "X-Actor-Id": session.email, "X-Actor-Role": session.role });
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

el("erp-sync-btn").addEventListener("click", async () => {
    if (!currentOrderId) { toast("Selecciona una orden.", "error"); return; }
    try {
        const order = await apiCall("GET", `/api/v1/orders/${currentOrderId}`);
        if (!CONFIRMED_STATUSES.has(order.status)) throw new Error("ERP solo recibe ventas confirmadas.");
        const grandTotal = Number(order.total);
        const netTotal = Number((grandTotal / 1.12).toFixed(2));
        const taxTotal = Number((grandTotal - netTotal).toFixed(2));
        const sale = await apiCall("POST", "/api/v1/integrations/erp/sales", { orderId: order.id, netTotal, taxTotal, grandTotal, currency: "GTQ", confirmedAt: order.updatedAt });
        el("legacy-result").innerHTML = `<div class="integration-success"><strong>Venta registrada en ERP-Core</strong><span>Estado ${escapeHtml(sale.status)} · ${escapeHtml(sale.id)}</span></div>`;
        toast("Venta disponible para el siguiente lote ERP.", "ok");
    } catch (error) { toast(error.message, "error"); }
});

el("pos-publish-btn").addEventListener("click", async () => {
    if (!currentOrderId) { toast("Selecciona una orden.", "error"); return; }
    const storeNumber = Number(el("pos-store-number").value);
    if (storeNumber < 1 || storeNumber > 32) { toast("La tienda POS debe estar entre 1 y 32.", "error"); return; }
    try {
        const order = await apiCall("GET", `/api/v1/orders/${currentOrderId}`);
        const eventKey = `sgo.pos-event.${order.id}.${storeNumber}.${order.status}`;
        let eventId = localStorage.getItem(eventKey);
        if (!eventId) { eventId = crypto.randomUUID(); localStorage.setItem(eventKey, eventId); }
        const delivery = await apiCall("POST", "/api/v1/integrations/pos/events", { eventId, eventType: "ORDER_STATUS_CHANGED", orderId: order.id, storeNumber, payload: { orderId: order.id, status: order.status, updatedAt: order.updatedAt } });
        el("legacy-result").innerHTML = `<div class="integration-success"><strong>Evento publicado a tienda ${storeNumber}</strong><span>Estado ${escapeHtml(delivery.status)} · intento ${delivery.attempts}</span></div>`;
        toast("Evento POS en proceso de entrega.", "ok");
    } catch (error) { toast(error.message, "error"); }
});

// ---------------------------------------------------------------------
// Arranque
// ---------------------------------------------------------------------

checkBackendStatus();
loadOperationsOverview();

const savedOrderId = localStorage.getItem(ORDER_ID_STORAGE_KEY);
if (savedOrderId) {
    el("order-id-input").value = savedOrderId;
    loadOrder(savedOrderId);
}
