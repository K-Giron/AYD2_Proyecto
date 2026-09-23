"use strict";

/**
 * Vista de cliente (UI-01, Kevin Girón). El catálogo y el carrito son una
 * vitrina de referencia sin backend (no existe INV-01 todavía); el
 * seguimiento de pedido, las notificaciones y las devoluciones sí llaman a
 * los endpoints reales de logistics/notifications/query/incidents.
 */

// sgoRequireSession redirige a login.html si no hay sesión de cliente; la
// redirección es asíncrona, así que se protege el resto contra `session === null`.
const session = sgoRequireSession("CLIENTE");

const CORRELATION_ID = crypto.randomUUID();
document.getElementById("correlation-id").textContent = CORRELATION_ID;
if (session) {
    document.getElementById("session-name").textContent = session.name;
    document.getElementById("logout-btn").addEventListener("click", sgoLogout);
}

// ---------------------------------------------------------------------
// Utilidades (mismas convenciones que app.js, self-contenidas)
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
        return new Date(iso).toLocaleString("es-GT", { dateStyle: "short", timeStyle: "short" });
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
    DELIVERED: "ok", SENT: "ok", REFUNDED: "ok", LABEL_ISSUED: "ok", APPROVED: "ok", CONFIRMED: "ok",
    PENDING: "warn", PREPARING: "warn", REGISTERED: "warn", IN_TRANSIT: "warn", ASSIGNED: "warn",
    LABEL_REQUESTED: "warn", READY_FOR_PICKUP: "warn", EVALUATING: "warn",
    FAILED: "error", REJECTED: "error", LABEL_FAILED: "error", CANCELLED: "error", RETURNED: "error",
};

function badge(status) {
    if (!status) return `<span class="badge badge-muted">sin dato</span>`;
    const tone = STATUS_TONE[status] || "muted";
    return `<span class="badge badge-${tone}">${escapeHtml(status)}</span>`;
}

function isValidUuid(value) {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value.trim());
}

async function apiCall(method, path, body) {
    const headers = { "X-Correlation-Id": CORRELATION_ID, ...sgoAuthHeaders() };
    if (body !== undefined) headers["Content-Type"] = "application/json";
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

// ---------------------------------------------------------------------
// Navegación por pestañas
// ---------------------------------------------------------------------

document.querySelectorAll(".store-tab").forEach((tab) => {
    tab.addEventListener("click", () => {
        document.querySelectorAll(".store-tab").forEach((t) => t.classList.toggle("active", t === tab));
        document.querySelectorAll(".store-view").forEach((view) => {
            view.classList.toggle("active", view.id === `view-${tab.dataset.view}`);
        });
    });
});

// ---------------------------------------------------------------------
// Catálogo (mock, decorativo) + carrito (localStorage, solo en este navegador)
// ---------------------------------------------------------------------

const CATALOG = [
    { id: "SKU-1001", name: "Licuadora 10 velocidades", price: 349.0, emoji: "🥤" },
    { id: "SKU-1002", name: "Set de sartenes antiadherentes", price: 599.0, emoji: "🍳" },
    { id: "SKU-1003", name: "Audífonos inalámbricos", price: 249.0, emoji: "🎧" },
    { id: "SKU-1004", name: "Mochila para laptop", price: 189.0, emoji: "🎒" },
    { id: "SKU-1005", name: "Cafetera de goteo", price: 279.0, emoji: "☕" },
    { id: "SKU-1006", name: "Lámpara de escritorio LED", price: 129.0, emoji: "💡" },
];

const CART_STORAGE_KEY = "sgo-store.cart";

function getCart() {
    try {
        return JSON.parse(localStorage.getItem(CART_STORAGE_KEY)) || {};
    } catch {
        return {};
    }
}

function saveCart(cart) {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cart));
    renderCartCount(cart);
}

function renderCartCount(cart) {
    const count = Object.values(cart).reduce((sum, qty) => sum + qty, 0);
    const badgeEl = el("cart-count");
    badgeEl.textContent = String(count);
    badgeEl.classList.toggle("hidden", count === 0);
}

function renderCatalog() {
    const cart = getCart();
    el("product-grid").innerHTML = CATALOG.map((product) => `
        <div class="product-card">
            <div class="product-thumb">${product.emoji}</div>
            <div class="product-name">${escapeHtml(product.name)}</div>
            <div class="product-price">Q${product.price.toFixed(2)}</div>
            <button class="btn btn-primary btn-sm" data-add-to-cart="${product.id}">
                ${cart[product.id] ? `En el carrito (${cart[product.id]})` : "Agregar al carrito"}
            </button>
            <div class="product-note">Catálogo de referencia — no conectado a INV-01 todavía.</div>
        </div>`).join("");
}

el("product-grid").addEventListener("click", (event) => {
    const button = event.target.closest("[data-add-to-cart]");
    if (!button) return;
    const cart = getCart();
    const productId = button.dataset.addToCart;
    cart[productId] = (cart[productId] || 0) + 1;
    saveCart(cart);
    renderCatalog();
    toast("Agregado al carrito.", "ok");
});

function renderCart() {
    const cart = getCart();
    const entries = Object.entries(cart).filter(([, qty]) => qty > 0);
    const container = el("cart-items");
    if (entries.length === 0) {
        container.innerHTML = `<p class="empty-state">Tu carrito está vacío. Ve al catálogo para agregar productos.</p>`;
    } else {
        container.innerHTML = entries.map(([productId, qty]) => {
            const product = CATALOG.find((p) => p.id === productId);
            return `
                <div class="cart-line">
                    <div>
                        <div class="cart-line-name">${escapeHtml(product.name)}</div>
                        <div class="cart-line-price">Q${product.price.toFixed(2)} c/u</div>
                    </div>
                    <div class="qty-control">
                        <button data-qty="-1" data-product="${productId}">−</button>
                        <span>${qty}</span>
                        <button data-qty="1" data-product="${productId}">+</button>
                    </div>
                </div>`;
        }).join("");
    }

    const subtotal = entries.reduce((sum, [productId, qty]) => {
        const product = CATALOG.find((p) => p.id === productId);
        return sum + product.price * qty;
    }, 0);
    const shipping = subtotal > 0 ? 35 : 0;
    el("checkout-totals").innerHTML = `
        <div class="totals-row"><span>Subtotal</span><span>Q${subtotal.toFixed(2)}</span></div>
        <div class="totals-row"><span>Envío estimado</span><span>Q${shipping.toFixed(2)}</span></div>
        <div class="totals-row total"><span>Total</span><span>Q${(subtotal + shipping).toFixed(2)}</span></div>`;
    el("confirm-purchase-btn").disabled = entries.length === 0;
}

el("cart-items").addEventListener("click", (event) => {
    const button = event.target.closest("[data-qty]");
    if (!button) return;
    const cart = getCart();
    const productId = button.dataset.product;
    const delta = Number(button.dataset.qty);
    cart[productId] = Math.max(0, (cart[productId] || 0) + delta);
    if (cart[productId] === 0) delete cart[productId];
    saveCart(cart);
    renderCart();
});

el("confirm-purchase-btn").addEventListener("click", () => {
    const delivery = document.querySelector('input[name="delivery"]:checked').value;
    const destination = el("checkout-destination").value.trim() || "(sin especificar)";
    el("checkout-result").innerHTML = `
        <div class="notice">
            <strong>Así se vería tu confirmación de compra.</strong>
            Elegiste <em>${escapeHtml(deliveryLabel(delivery))}</em> hacia <em>${escapeHtml(destination)}</em>.
            El checkout real (carrito recuperable, cobro y factura) depende de <code>ORD-01</code>
            (Jairo Gómez), <code>PAY-01</code> (Edson Sosa) y <code>FIS-01</code> (Andy Sánchez), que
            todavía no existen en el backend — por eso ningún cargo ni orden se generó ahora.
            Ve a "Mis pedidos" para ver despacho, notificaciones y devoluciones funcionando con datos
            reales sobre una orden ya existente.
        </div>`;
});

function deliveryLabel(value) {
    return { CUSTOMER_PICKUP: "recoger en tienda", DISTRIBUTION_CENTER: "envío a domicilio", STORE: "envío desde tienda" }[value] || value;
}

// ---------------------------------------------------------------------
// Mis pedidos — QRY-01 (real) + LOG-02 (real) + NOT-01 (real)
// ---------------------------------------------------------------------

const TRACKING_ORDER_KEY = "sgo-store.lastTrackedOrderId";

const STEP_DEFS = [
    { key: "confirmed", label: "Confirmado" },
    { key: "preparing", label: "Preparando" },
    { key: "transit", label: "En tránsito" },
    { key: "delivered", label: "Entregado" },
];

function stepIndexFromShipmentStatus(status) {
    switch (status) {
        case "DELIVERED": return 3;
        case "IN_TRANSIT": return 2;
        case "LABEL_ISSUED":
        case "READY_FOR_PICKUP":
        case "LABEL_REQUESTED":
        case "LABEL_FAILED": return 1;
        case "ASSIGNED":
        default: return 0;
    }
}

async function loadTracking(orderId) {
    if (!isValidUuid(orderId)) {
        toast("Ese número de orden no parece válido.", "error");
        return;
    }
    localStorage.setItem(TRACKING_ORDER_KEY, orderId);
    const container = el("tracking-result");
    container.innerHTML = `<p class="empty-state">Buscando tu pedido…</p>`;

    const [summaryResult, shipmentsResult, notificationsResult] = await Promise.allSettled([
        apiCall("GET", `/api/v1/orders/${encodeURIComponent(orderId)}/summary`),
        apiCall("GET", `/api/v1/shipments?orderId=${encodeURIComponent(orderId)}`),
        apiCall("GET", `/api/v1/notifications?sourceAggregateId=${encodeURIComponent(orderId)}`),
    ]);

    const summary = summaryResult.status === "fulfilled" ? summaryResult.value : null;
    const shipments = shipmentsResult.status === "fulfilled" ? shipmentsResult.value : [];
    const notifications = notificationsResult.status === "fulfilled" ? notificationsResult.value : [];

    if (!summary && shipments.length === 0 && notifications.length === 0) {
        container.innerHTML = `<p class="empty-state">No encontramos información para ese número de orden todavía.</p>`;
        return;
    }

    const mostRecentShipment = shipments[0];
    const stepIndex = mostRecentShipment ? stepIndexFromShipmentStatus(mostRecentShipment.status) : 0;
    const stepperHtml = STEP_DEFS.map((step, index) => {
        const cls = index < stepIndex ? "done" : index === stepIndex ? "current" : "";
        return `<div class="step ${cls}"><div class="step-dot"></div>${step.label}</div>`;
    }).join("");

    const shipmentHtml = shipments.length
        ? shipments.map((shipment) => `
            <div class="track-event">
                <span>${escapeHtml(shipment.destination)} ${shipment.trackingNumber ? `· guía ${escapeHtml(shipment.trackingNumber)}` : ""}</span>
                ${badge(shipment.status)}
            </div>`).join("")
        : `<p class="empty-state">Sin despacho registrado todavía.</p>`;

    const notificationsHtml = notifications.length
        ? notifications.map((n) => `
            <div class="track-event">
                <span>Te avisamos por ${escapeHtml(n.channel)} — ${escapeHtml(n.eventType)}</span>
                ${badge(n.status)}
            </div>`).join("")
        : `<p class="empty-state">Sin notificaciones registradas todavía.</p>`;

    const summaryHtml = summary
        ? `<p class="hint">Última actualización: ${formatDate(summary.updatedAt)}</p>`
        : `<p class="hint">Sin resumen consolidado todavía para esta orden.</p>`;

    container.innerHTML = `
        <div class="order-track-card">
            <h2>Pedido ${escapeHtml(orderId)}</h2>
            <div class="stepper">${stepperHtml}</div>
            ${summaryHtml}
            <h3>Despacho</h3>
            <div class="track-events">${shipmentHtml}</div>
            <h3>Notificaciones</h3>
            <div class="track-events">${notificationsHtml}</div>
        </div>`;
}

el("tracking-load-btn").addEventListener("click", () => loadTracking(el("tracking-order-input").value));
el("tracking-order-input").addEventListener("keydown", (event) => {
    if (event.key === "Enter") loadTracking(el("tracking-order-input").value);
});

// ---------------------------------------------------------------------
// Devoluciones — INC-01 (real)
// ---------------------------------------------------------------------

el("return-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.target;
    const values = Object.fromEntries(new FormData(form).entries());
    if (!isValidUuid(values.orderId)) {
        toast("Ese número de orden no parece válido.", "error");
        return;
    }
    const payload = {
        orderId: values.orderId,
        type: values.type,
        reason: values.reason,
        partial: form.elements.partial.checked,
        items: [{ sku: values.sku, quantity: Number(values.quantity) }],
    };
    try {
        await apiCall("POST", "/api/v1/incidents", payload);
        toast("Solicitud enviada. Te avisaremos del resultado.", "ok");
        form.reset();
        el("returns-order-input").value = values.orderId;
        await loadReturns(values.orderId);
    } catch (error) {
        toast(`No se pudo enviar la solicitud: ${error.message}`, "error");
    }
});

async function loadReturns(orderId) {
    if (!isValidUuid(orderId)) {
        toast("Ese número de orden no parece válido.", "error");
        return;
    }
    const container = el("returns-list");
    try {
        const incidents = await apiCall("GET", `/api/v1/incidents?orderId=${encodeURIComponent(orderId)}`);
        container.innerHTML = incidents.length
            ? incidents.map((incident) => `
                <div class="item-row">
                    <div class="item-row-head">
                        <div>
                            <div class="item-row-title">${escapeHtml(incident.reason)}</div>
                            <div class="item-row-meta">${formatDate(incident.createdAt)}${incident.refundAmount ? ` · reembolso Q${incident.refundAmount}` : ""}</div>
                        </div>
                        ${badge(incident.status)}
                    </div>
                </div>`).join("")
            : `<p class="empty-state">Sin solicitudes todavía para esta orden.</p>`;
    } catch (error) {
        container.innerHTML = `<p class="empty-state">Error al consultar: ${escapeHtml(error.message)}</p>`;
    }
}

el("returns-load-btn").addEventListener("click", () => loadReturns(el("returns-order-input").value));

// ---------------------------------------------------------------------
// Arranque
// ---------------------------------------------------------------------

renderCatalog();
renderCartCount(getCart());
renderCart();

const lastOrderId = localStorage.getItem(TRACKING_ORDER_KEY);
if (lastOrderId) {
    el("tracking-order-input").value = lastOrderId;
}
