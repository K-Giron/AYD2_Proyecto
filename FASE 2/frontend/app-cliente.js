"use strict";

const session = sgoRequireSession("CLIENTE");
const CORRELATION_ID = crypto.randomUUID();
const CUSTOMER_ID = session?.customerId || "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
const CART_CONTEXT_KEY = `sgo.megastore.active-cart.${CUSTOMER_ID}`;
const LAST_ORDER_KEY = `sgo.megastore.last-order.${CUSTOMER_ID}`;
const PURCHASE_INTENT_KEY = "sgo.purchaseIntent";
const FAVORITES_KEY = "sgo.megastore.favorites";

let CATALOG = [];

let activeCart = null;
let catalogAvailability = new Map();

function el(id) { return document.getElementById(id); }
function escapeHtml(value) { return String(value ?? "").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;"); }
function money(value) { return new Intl.NumberFormat("es-GT", { style: "currency", currency: "GTQ" }).format(Number(value || 0)); }
function formatDate(iso) { return iso ? new Date(iso).toLocaleString("es-GT", { dateStyle: "medium", timeStyle: "short" }) : "Sin fecha"; }
function isValidUuid(value) { return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(String(value).trim()); }
function productById(id) { return CATALOG.find(product => product.id === id); }
function getFavorites() { try { return new Set(JSON.parse(localStorage.getItem(FAVORITES_KEY)) || []); } catch { return new Set(); } }
function saveFavorites(favorites) { localStorage.setItem(FAVORITES_KEY, JSON.stringify([...favorites])); }

async function loadCatalogDefinition() {
    CATALOG = await sgoLoadCatalog();
}

function toast(message, type = "") {
    const node = document.createElement("div");
    node.className = `toast ${type}`.trim();
    node.textContent = message;
    el("toast-stack").appendChild(node);
    setTimeout(() => node.remove(), 4500);
}

async function apiCall(method, path, body, extraHeaders = {}) {
    const headers = { "X-Correlation-Id": CORRELATION_ID, ...sgoAuthHeaders(), ...extraHeaders };
    if (body !== undefined) headers["Content-Type"] = "application/json";
    const response = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    const text = await response.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = { message: text }; }
    if (!response.ok) {
        const error = new Error(data?.message || `Error HTTP ${response.status}`);
        error.status = response.status;
        throw error;
    }
    return data;
}

function storedCartContext() {
    try { return JSON.parse(localStorage.getItem(CART_CONTEXT_KEY)) || null; } catch { return null; }
}

/**
 * Vuelve a leer el carrito activo del cliente en el servidor: pudo cambiar
 * desde otro canal (call center, tienda física, otro dispositivo).
 */
async function refreshActiveCart() {
    try {
        activeCart = await apiCall("GET", `/api/v1/carts/active?customerId=${CUSTOMER_ID}`);
    } catch (error) {
        if (error.status === 404) activeCart = null;
        return;
    }
    renderCatalog(); renderCart();
}

async function ensureCart() {
    if (activeCart?.status === "ACTIVE") return activeCart;
    let context = storedCartContext();
    if (!context) {
        context = { idempotencyKey: `cart-${crypto.randomUUID()}` };
        localStorage.setItem(CART_CONTEXT_KEY, JSON.stringify(context));
    }
    activeCart = await apiCall("POST", "/api/v1/carts", { customerId: CUSTOMER_ID, channel: "WEB" }, { "Idempotency-Key": context.idempotencyKey });
    if (activeCart.status !== "ACTIVE") {
        // La clave guardada apunta a un carrito ya cerrado (p. ej. se cobró en la
        // tienda física): con una clave nueva el servidor devuelve el carrito
        // vigente del cliente o abre uno nuevo.
        context = { idempotencyKey: `cart-${crypto.randomUUID()}` };
        localStorage.setItem(CART_CONTEXT_KEY, JSON.stringify(context));
        activeCart = await apiCall("POST", "/api/v1/carts", { customerId: CUSTOMER_ID, channel: "WEB" }, { "Idempotency-Key": context.idempotencyKey });
    }
    renderCatalog(); renderCart();
    return activeCart;
}

/** Vuelve a leer el catálogo publicado: la disponibilidad cambia con cada reserva y recepción. */
async function loadAvailability() {
    try {
        CATALOG = await sgoLoadCatalog();
        catalogAvailability = new Map(CATALOG.map(product => [product.id, product.available]));
        el("catalog-status").textContent = CATALOG.length ? "Existencia de la tienda online" : "La tienda online aún no tiene productos publicados";
    } catch {
        el("catalog-status").textContent = "Existencia no disponible: se valida al reservar";
    }
    renderCatalog();
}

function cartQuantity(productId) { return activeCart?.items?.find(item => item.productId === productId)?.quantity || 0; }
function renderCartCount() {
    const count = activeCart?.itemCount || 0;
    el("cart-count").textContent = `${count} producto${count === 1 ? "" : "s"}`;
}

function productCard(product) {
        const quantity = cartQuantity(product.id);
        const stock = catalogAvailability.get(product.id);
        const favorite = getFavorites().has(product.id);
        return `<article class="product-card commerce-product-card">
            <a class="product-card-image" href="producto.html?id=${encodeURIComponent(product.id)}"><img src="${escapeHtml(product.image)}" alt="${escapeHtml(product.name)}" width="900" height="900" loading="lazy" /></a>
            <button class="favorite-button ${favorite ? "active" : ""}" data-favorite-product="${product.id}" aria-label="${favorite ? "Quitar de favoritos" : "Agregar a favoritos"}" aria-pressed="${favorite}">♡</button>
            <div class="product-meta"><span class="product-kicker">${escapeHtml(product.category)} · ${product.sku}</span><a class="product-title-link" href="producto.html?id=${encodeURIComponent(product.id)}"><h3 class="product-name">${escapeHtml(product.name)}</h3></a>
            <div class="product-row"><div><span class="product-price">${money(product.price)}</span><span class="product-stock">${stock === undefined ? "Validación al reservar" : stock > 0 ? `${stock} disponibles` : "Agotado"}</span></div>
            <div class="product-actions"><a href="producto.html?id=${encodeURIComponent(product.id)}">Ver detalle</a><button class="btn btn-primary btn-sm" data-add="${product.id}" ${stock === 0 ? "disabled" : ""}>${quantity ? `Agregar otro (${quantity})` : "Agregar"}</button></div></div></div>
        </article>`;
}

function renderCatalog(filter = el("catalog-search")?.value || "") {
    const normalized = filter.trim().toLowerCase();
    const products = CATALOG.filter(product => `${product.name} ${product.category} ${product.sku}`.toLowerCase().includes(normalized));
    el("product-grid").innerHTML = products.length ? products.map(productCard).join("") : `<p class="empty-state">No encontramos productos para esa búsqueda.</p>`;
    renderCartCount();
}

function renderFavorites() {
    const favorites = getFavorites();
    const products = CATALOG.filter(product => favorites.has(product.id));
    el("favorites-grid").innerHTML = products.length ? products.map(productCard).join("") : `<div class="empty-favorites"><span aria-hidden="true">♡</span><h2>Aún no guardas productos</h2><p>Marca el corazón de cualquier producto para encontrarlo aquí.</p><button class="btn btn-primary" data-empty-catalog>Explorar catálogo</button></div>`;
}

function toggleFavorite(productId) {
    const favorites = getFavorites();
    favorites.has(productId) ? favorites.delete(productId) : favorites.add(productId);
    saveFavorites(favorites);
    renderCatalog();
    renderFavorites();
}

async function setQuantity(productId, quantity) {
    const product = productById(productId);
    if (!product) throw new Error("El producto seleccionado ya no está disponible en el catálogo.");
    const cart = await ensureCart();
    activeCart = quantity <= 0
        ? await apiCall("DELETE", `/api/v1/carts/${cart.id}/items/${productId}`)
        : await apiCall("PUT", `/api/v1/carts/${cart.id}/items/${productId}`, { productName: product.name, unitPrice: product.price, quantity });
    renderCatalog(); renderCart();
}

async function consumePurchaseIntent() {
    let productId = null;
    try { productId = JSON.parse(sessionStorage.getItem(PURCHASE_INTENT_KEY))?.productId; } catch { productId = null; }
    if (!productId || !productById(productId)) return;
    try {
        await setQuantity(productId, cartQuantity(productId) + 1);
        sessionStorage.removeItem(PURCHASE_INTENT_KEY);
        selectView("checkout");
        toast("Producto agregado. Revisa tu carrito para continuar.", "ok");
    } catch (error) {
        toast(error.message, "error");
    }
}

function renderCart() {
    const items = activeCart?.items || [];
    el("cart-items").innerHTML = items.length ? items.map(item => {
        const product = productById(item.productId);
        const initials = item.productName.split(/\s+/).slice(0, 2).map(word => word[0]).join("").toUpperCase();
        const swatch = product?.image ? `<img src="${escapeHtml(product.image)}" alt="" />` : escapeHtml(initials);
        return `<div class="cart-line"><div class="cart-swatch" aria-hidden="true">${swatch}</div><div><div class="cart-line-name">${escapeHtml(item.productName)}</div><div class="cart-line-price">${money(item.unitPrice)} por unidad</div></div><div class="qty-control"><button aria-label="Restar uno" data-quantity="${item.quantity - 1}" data-product="${item.productId}">−</button><span>${item.quantity}</span><button aria-label="Sumar uno" data-quantity="${item.quantity + 1}" data-product="${item.productId}">+</button></div></div>`;
    }).join("") : `<p class="empty-state">Tu carrito está vacío. Elige una pieza de la colección para comenzar.</p>`;
    const total = Number(activeCart?.total || 0);
    el("checkout-totals").innerHTML = `<div class="totals-row"><span>Productos</span><span>${money(total)}</span></div><div class="totals-row"><span>Entrega</span><span>Se calcula al despachar</span></div><div class="totals-row total"><span>Total de productos</span><span>${money(total)}</span></div>`;
    el("confirm-purchase-btn").disabled = !items.length;
    renderCartCount();
}

function selectView(name) {
    document.querySelectorAll(".store-tab").forEach(tab => tab.classList.toggle("active", tab.dataset.view === name));
    document.querySelectorAll(".store-view").forEach(view => view.classList.toggle("active", view.id === `view-${name}`));
    if (name === "favorites") renderFavorites();
    if (name === "checkout") { renderCart(); refreshActiveCart(); }
    if (name === "tracking") loadRecentOrders();
    if (name === "returns") loadReturnOptions();
    if (location.hash !== `#${name}`) history.replaceState(null, "", `#${name}`);
    window.scrollTo({ top: 0, behavior: "smooth" });
}

const FLOW_STEPS = ["Orden creada", "Inventario reservado", "Pago procesado", "Documento fiscal", "Despacho iniciado"];
function renderFlow(current, completed = false) {
    el("checkout-result").innerHTML = `<div class="flow-status">${FLOW_STEPS.map((label, index) => `<div class="flow-step ${completed || index < current ? "done" : index === current ? "current" : ""}">${label}</div>`).join("")}</div>`;
}

function sleep(milliseconds) { return new Promise(resolve => setTimeout(resolve, milliseconds)); }
/**
 * Repite la misma solicitud de la Saga (misma llave: nunca cobra dos veces).
 * SAT tarda poco; si PayKlik no respondió a tiempo, la conciliación consulta
 * a PayKlik en segundo plano y la compra continúa sola cuando se resuelve.
 */
async function waitForSaga(orderId, sagaKey, payload) {
    const started = Date.now();
    let fiscalSince = null;
    for (;;) {
        const result = await apiCall("POST", `/api/v1/orders/${orderId}/saga`, payload,
            { "Idempotency-Key": sagaKey, "X-Actor": `customer:${CUSTOMER_ID}` });
        if (result.sagaStatus === "WAITING_FISCAL") fiscalSince ??= Date.now();
        if (result.sagaStatus === "WAITING_FISCAL" && Date.now() - fiscalSince < 15000) { await sleep(500); continue; }
        if (result.sagaStatus === "WAITING_PAYMENT" && Date.now() - started < 180000) {
            if (!el("payment-wait")) el("checkout-result").insertAdjacentHTML("beforeend", `<p id="payment-wait" class="catalog-status"></p>`);
            el("payment-wait").textContent = "PayKlik tardó en responder. Estamos confirmando tu pago con PayKlik sin volver a cobrarte; no cierres esta ventana…";
            await sleep(5000);
            continue;
        }
        el("payment-wait")?.remove();
        return result.sagaStatus === "WAITING_FISCAL" ? null : result;
    }
}

// ---------------------------------------------------------------------
// Entrega: recoger en una ubicación física activa o envío a domicilio
// ---------------------------------------------------------------------

let pickupPoints = [];

async function loadPickupPoints() {
    try {
        const response = await fetch("/api/v1/online-catalog/pickup-points", { cache: "no-store" });
        pickupPoints = response.ok ? await response.json() : [];
    } catch { pickupPoints = []; }
    el("pickup-points").innerHTML = pickupPoints.length
        ? pickupPoints.map((point, index) => `<label class="pickup-point"><input type="radio" name="pickup-point" value="${escapeHtml(point.code)}" ${index === 0 ? "checked" : ""} />
            <span><strong>${escapeHtml(point.name)}</strong><small>${escapeHtml(point.address || "")}</small><small>${escapeHtml(point.hours || "")}</small></span></label>`).join("")
        : `<p class="catalog-status">No hay ubicaciones de recogida disponibles; elige envío a domicilio.</p>`;
}

function deliveryMode() { return document.querySelector("input[name=delivery]:checked")?.value || "PICKUP"; }

function updateDeliveryFields() {
    const pickup = deliveryMode() === "PICKUP";
    el("pickup-block").classList.toggle("hidden", !pickup);
    el("home-block").classList.toggle("hidden", pickup);
}

/** Lo que viaja como destino del despacho: la ubicación de recogida o la dirección. */
function deliveryDestination() {
    if (deliveryMode() === "PICKUP") {
        const code = document.querySelector("input[name=pickup-point]:checked")?.value;
        const point = pickupPoints.find(p => p.code === code);
        return point ? `Recoger en ${point.name} (${point.code}) — ${point.address}` : "";
    }
    return el("checkout-destination").value.trim();
}

// ---------------------------------------------------------------------
// Ventana de pago PayKlik: tarjeta tokenizada o transferencia (el backend la autoriza en payklik-service)
// ---------------------------------------------------------------------

/** Abre la ventana y resuelve con {paymentMethod, paymentReference}, o null si se cancela. */
function askPayKlikPayment(total) {
    const method = document.querySelector("input[name=payment]:checked")?.value || "CARD";
    // Referencias sin secuencias largas de dígitos: el backend rechaza lo que parezca un número de tarjeta.
    const code = crypto.randomUUID().replace(/-/g, "").slice(0, 8).toUpperCase();
    el("payklik-amount").textContent = money(total);
    el("transfer-reference").textContent = `MS-${code}`;
    el("payklik-card").classList.toggle("hidden", method !== "CARD");
    el("payklik-transfer").classList.toggle("hidden", method !== "TRANSFER");
    el("payklik-modal").classList.remove("hidden");
    (method === "CARD" ? el("payklik-charge-btn") : el("payklik-transfer-btn")).focus();
    return new Promise(resolve => {
        const close = result => {
            el("payklik-modal").classList.add("hidden");
            el("payklik-charge-btn").onclick = el("payklik-transfer-btn").onclick = el("payklik-cancel-btn").onclick = null;
            resolve(result);
        };
        el("payklik-charge-btn").onclick = () => close({ paymentMethod: "CARD", paymentReference: `tok_payklik_${code.toLowerCase()}` });
        el("payklik-transfer-btn").onclick = () => close({ paymentMethod: "TRANSFER", paymentReference: `TRF-MS-${code}` });
        el("payklik-cancel-btn").onclick = () => close(null);
    });
}

async function completePurchase() {
    const button = el("confirm-purchase-btn");
    const fiscalName = el("fiscal-name").value.trim();
    const taxIdentifier = el("fiscal-tax-id").value.trim();
    const destination = deliveryDestination();
    if (!destination) { toast(deliveryMode() === "PICKUP" ? "Elige dónde recoger tu compra." : "Escribe la dirección de entrega.", "error"); return; }
    if (!fiscalName || !taxIdentifier) { toast("Completa los datos fiscales.", "error"); return; }
    const total = (activeCart?.items || []).reduce((sum, item) => sum + Number(item.lineTotal), 0);
    const payment = await askPayKlikPayment(total);
    if (!payment) { toast("Pago cancelado: tu carrito sigue igual.", ""); return; }
    button.disabled = true;
    try {
        const cart = await ensureCart();
        const checkoutKey = `checkout-${cart.id}`;
        renderFlow(0);
        const order = await apiCall("POST", `/api/v1/carts/${cart.id}/checkout`, undefined, { "Idempotency-Key": checkoutKey, "X-Actor": `customer:${CUSTOMER_ID}` });
        localStorage.setItem(LAST_ORDER_KEY, order.id);
        renderFlow(1);
        const sagaKey = `saga-${order.id}`;
        const saga = await waitForSaga(order.id, sagaKey, {
            paymentMethod: payment.paymentMethod, currency: "GTQ", paymentReference: payment.paymentReference,
            taxIdentifier, customerName: fiscalName, destination
        });
        if (!saga) throw new Error("SAT sigue procesando el documento. Puedes reanudar la compra sin duplicar cobros.");
        if (saga.sagaStatus !== "COMPLETED") throw new Error(saga.detail || `La Saga quedó en ${saga.sagaStatus}.`);
        renderFlow(5, true);
        el("checkout-result").innerHTML += `<div class="checkout-success"><strong>Compra confirmada y en preparación</strong><br />Orden ${escapeHtml(order.id)}<br />CUI ${escapeHtml(saga.fiscalCui)} · Serie ${escapeHtml(saga.fiscalSeries)}<br />${payment.paymentMethod === "TRANSFER" ? "Pago por transferencia" : "Pago con tarjeta"} · ${escapeHtml(destination)}<br /><button type="button" class="btn btn-primary btn-sm" data-invoice-order="${escapeHtml(order.id)}" style="margin-top:.6rem">Ver factura</button></div>`;
        localStorage.removeItem(CART_CONTEXT_KEY); activeCart = null; await loadRecentOrders(); renderCatalog(); renderCart();
        await showInvoice(order.id);
    } catch (error) {
        el("checkout-result").innerHTML = `<div class="checkout-error"><strong>No se completó el flujo.</strong><br />${escapeHtml(error.message)}</div>`;
        button.disabled = false;
    }
}

// ---------------------------------------------------------------------
// Factura de la orden (CUI y serie autorizados por SAT, con el XML emitido)
// ---------------------------------------------------------------------

const INVOICE_ISSUER = { name: "MegaStore, S.A.", taxId: "1234567-8", address: "Ciudad de Guatemala" };
const IVA_RATE = 0.12;
let invoiceOnScreen = null;

async function showInvoice(orderId) {
    try {
        const [order, invoice] = await Promise.all([
            apiCall("GET", `/api/v1/orders/${orderId}`),
            apiCall("GET", `/api/v1/invoices/orders/${orderId}`),
        ]);
        invoiceOnScreen = invoice;
        el("invoice-body").innerHTML = invoiceHtml(order, invoice);
        el("invoice-modal").classList.remove("hidden");
        el("invoice-print-btn").focus();
    } catch (error) {
        toast(error.status === 404 ? "La factura todavía se está certificando con SAT. Inténtalo en unos segundos desde Mis pedidos." : error.message, "error");
    }
}

function invoiceHtml(order, invoice) {
    const total = Number(invoice.total);
    const base = Math.round(total / (1 + IVA_RATE) * 100) / 100;
    const iva = Math.round((total - base) * 100) / 100;
    const approved = invoice.status === "APPROVED";
    const lines = order.items.map(line => `<tr>
        <td>${line.quantity}</td><td>${escapeHtml(line.productName)}</td>
        <td class="r">${money(line.unitPrice)}</td><td class="r">${money(line.lineTotal)}</td></tr>`).join("");
    return `<header>
            <div><h2>${escapeHtml(INVOICE_ISSUER.name)}</h2><div class="muted">NIT ${escapeHtml(INVOICE_ISSUER.taxId)} · ${escapeHtml(INVOICE_ISSUER.address)}</div><div class="muted">Tienda online · canal ${escapeHtml(order.channel)}</div></div>
            <div class="doc-title"><strong>FACTURA ELECTRÓNICA</strong><span class="muted">Documento tributario electrónico</span><br /><span class="stamp ${approved ? "" : "pending"}">${approved ? "CERTIFICADA SAT" : escapeHtml(invoice.status)}</span></div>
        </header>
        <div class="grid">
            <div><span>Serie</span>${escapeHtml(invoice.series)}</div>
            <div><span>Fecha de emisión</span>${formatDate(invoice.issuedAt)}</div>
            <div><span>CUI</span><span class="mono">${escapeHtml(invoice.cui)}</span></div>
            <div><span>Autorización SAT</span><span class="mono">${escapeHtml(invoice.satAuthorization || "Pendiente")}</span></div>
            <div><span>NIT del cliente</span>${escapeHtml(invoice.taxIdentifier)}</div>
            <div><span>Nombre</span>${escapeHtml(invoice.customerName)}</div>
            <div><span>Orden</span><span class="mono">${escapeHtml(order.id)}</span></div>
            <div><span>Certificada</span>${formatDate(invoice.certifiedAt)}</div>
        </div>
        <table><thead><tr><th>Cant.</th><th>Descripción</th><th class="r">Precio</th><th class="r">Importe</th></tr></thead><tbody>${lines}</tbody></table>
        <div class="totals">
            <div><span>Subtotal sin IVA</span><span>${money(base)}</span></div>
            <div><span>IVA (12 %)</span><span>${money(iva)}</span></div>
            <div class="grand"><span>Total ${escapeHtml(invoice.currency)}</span><span>${money(total)}</span></div>
        </div>
        <footer>Pago con tarjeta tokenizada vía PayKlik (sin datos de tarjeta). El XML enviado a SAT se puede descargar con su CUI y serie.</footer>`;
}

function closeInvoice() { el("invoice-modal").classList.add("hidden"); invoiceOnScreen = null; }

el("invoice-close-btn").addEventListener("click", closeInvoice);
el("invoice-modal").addEventListener("keydown", event => { if (event.key === "Escape") closeInvoice(); });
el("invoice-print-btn").addEventListener("click", () => window.print());
el("invoice-xml-btn").addEventListener("click", () => {
    if (!invoiceOnScreen) return;
    const url = URL.createObjectURL(new Blob([invoiceOnScreen.xml], { type: "application/xml" }));
    const link = Object.assign(document.createElement("a"), { href: url, download: `factura-${invoiceOnScreen.series}-${invoiceOnScreen.cui}.xml` });
    document.body.appendChild(link); link.click(); link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
});
document.addEventListener("click", event => {
    const button = event.target.closest("[data-invoice-order]");
    if (button) showInvoice(button.dataset.invoiceOrder);
});

const STATUS_TONE = { DELIVERED:"ok",APPROVED:"ok",CONFIRMED:"ok",FISCAL_APPROVED:"ok",SENT:"ok",IN_TRANSIT:"warn",READY_FOR_PICKUP:"ok",LABEL_ISSUED:"warn",PREPARING:"warn",FISCAL_PENDING:"warn",PAYMENT_INDETERMINATE:"warn",PAYMENT_REJECTED:"error",REJECTED:"error",CANCELLED:"error",FAILED:"error" };
function badge(status) { return `<span class="badge badge-${STATUS_TONE[status] || "muted"}">${escapeHtml(status || "SIN DATO")}</span>`; }
const ORDER_STEPS = ["Confirmado", "Preparando", "En tránsito", "Entregado"];
function trackingIndex(order, shipments) {
    const shipment = shipments[0];
    if (shipment?.status === "DELIVERED" || order?.status === "DELIVERED") return 3;
    if (["IN_TRANSIT","READY_FOR_PICKUP"].includes(shipment?.status) || order?.status === "IN_TRANSIT") return 2;
    if (["PREPARING","LABEL_REQUESTED","LABEL_ISSUED"].includes(shipment?.status || order?.status)) return 1;
    return 0;
}

async function loadRecentOrders() {
    try {
        const orders = await apiCall("GET", `/api/v1/orders?customerId=${CUSTOMER_ID}`);
        el("recent-orders").innerHTML = orders.length ? orders.map(order => `<button class="order-chip" data-order="${order.id}">${order.id.slice(0,8)} · ${escapeHtml(order.status)}</button>`).join("") : `<p class="empty-state">Aún no tienes pedidos.</p>`;
    } catch { el("recent-orders").innerHTML = ""; }
}

async function loadTracking(orderId) {
    if (!isValidUuid(orderId)) { toast("Ingresa un identificador de orden válido.", "error"); return; }
    localStorage.setItem(LAST_ORDER_KEY, orderId); el("tracking-result").innerHTML = `<p class="empty-state">Consultando el recorrido completo.</p>`;
    const [orderResult, shipmentsResult, notificationsResult, paymentsResult, fiscalResult] = await Promise.allSettled([
        apiCall("GET", `/api/v1/orders/${orderId}`), apiCall("GET", `/api/v1/shipments?orderId=${orderId}`), apiCall("GET", `/api/v1/notifications?sourceAggregateId=${orderId}`), apiCall("GET", `/api/v1/payments/attempts?orderId=${orderId}`), apiCall("GET", `/api/v1/invoices/orders/${orderId}`)
    ]);
    const order = orderResult.status === "fulfilled" ? orderResult.value : null;
    const shipments = shipmentsResult.status === "fulfilled" ? shipmentsResult.value : [];
    const notifications = notificationsResult.status === "fulfilled" ? notificationsResult.value : [];
    const payments = paymentsResult.status === "fulfilled" ? paymentsResult.value : [];
    const fiscal = fiscalResult.status === "fulfilled" && fiscalResult.value ? [fiscalResult.value] : [];
    if (!order) { el("tracking-result").innerHTML = `<p class="empty-state">No encontramos esa orden.</p>`; return; }
    const index = trackingIndex(order, shipments);
    const events = [
        `<div class="track-event"><span>Orden · ${formatDate(order.createdAt)}</span>${badge(order.status)}</div>`,
        ...payments.map(item => `<div class="track-event"><span>PayKlik · ${formatDate(item.processedAt)}</span>${badge(item.status)}</div>`),
        ...fiscal.map(item => `<div class="track-event"><span>SAT · ${escapeHtml(item.cui)}</span>${badge(item.status)}</div>`),
        ...shipments.map(item => `<div class="track-event"><span>LogiExpress${item.trackingNumber ? ` · guía ${escapeHtml(item.trackingNumber)}` : ""} · ${escapeHtml(item.destination || "Destino registrado")}${item.status === "READY_FOR_PICKUP" ? " · <strong>listo para recoger</strong>" : ""}</span>${badge(item.status)}</div>`),
        ...notifications.map(item => `<div class="track-event"><span>${escapeHtml(item.channel)} · ${escapeHtml(item.eventType)}</span>${badge(item.status)}</div>`)
    ];
    el("tracking-result").innerHTML = `<article class="order-track-card"><p class="eyebrow">Orden trazable</p><h2>${escapeHtml(order.id)}</h2>${fiscal.length ? `<button type="button" class="btn btn-primary btn-sm" data-invoice-order="${escapeHtml(order.id)}">Ver factura ${escapeHtml(fiscal[0].series)}</button>` : `<p class="catalog-status">Factura aún en certificación con SAT.</p>`}<div class="stepper">${ORDER_STEPS.map((label,i) => `<div class="step ${i < index ? "done" : i === index ? "current" : ""}"><div class="step-dot"></div>${label}</div>`).join("")}</div><h3>Actividad consolidada</h3><div class="track-events">${events.join("")}</div></article>`;
}

// Devoluciones: se elige una compra propia y un producto de esa compra (sin teclear identificadores).
let returnOrders = [];

async function loadReturnOptions() {
    try {
        returnOrders = (await apiCall("GET", `/api/v1/orders?customerId=${CUSTOMER_ID}`))
            .filter(order => ["CONFIRMED", "PREPARING", "IN_TRANSIT", "DELIVERED"].includes(order.status));
    } catch { returnOrders = []; }
    const options = returnOrders.map(order => `<option value="${order.id}">${formatDate(order.createdAt)} · ${escapeHtml(order.items.map(i => `${i.quantity}× ${i.productName}`).join(", ")).slice(0, 60)} · ${money(order.total)}</option>`).join("");
    el("return-order-select").innerHTML = returnOrders.length ? options : `<option value="">Aún no tienes compras confirmadas</option>`;
    el("returns-order-input").innerHTML = `<option value="">Elige una compra</option>` + options;
    fillReturnProducts();
}

function fillReturnProducts() {
    const order = returnOrders.find(o => o.id === el("return-order-select").value);
    el("return-sku-select").innerHTML = order
        ? order.items.map(item => {
            const sku = productById(item.productId)?.sku || item.productName;
            return `<option value="${escapeHtml(sku)}">${escapeHtml(item.productName)} (${escapeHtml(sku)}) · compraste ${item.quantity}</option>`;
        }).join("")
        : `<option value="">Elige primero la compra</option>`;
}

async function submitReturn(event) {
    event.preventDefault(); const form = event.currentTarget; const values = Object.fromEntries(new FormData(form).entries());
    if (!isValidUuid(values.orderId)) { toast("Ingresa un identificador de orden válido.", "error"); return; }
    try {
        await apiCall("POST", "/api/v1/incidents", { orderId: values.orderId, customerId: CUSTOMER_ID, type: values.type, reason: values.reason, partial: form.elements.partial.checked, items: [{ sku: values.sku, quantity: Number(values.quantity) }] });
        toast("Solicitud registrada.", "ok"); el("returns-order-input").value = values.orderId; form.reset(); fillReturnProducts(); await loadReturns(values.orderId);
    } catch (error) { toast(error.message, "error"); }
}

function renderReturns(incidents, emptyMessage) {
    el("returns-list").innerHTML = incidents.length
        ? incidents.map(item => `<div class="track-event"><span>${escapeHtml(item.reason)} · ${formatDate(item.createdAt)}</span>${badge(item.status)}</div>`).join("")
        : `<p class="empty-state">${emptyMessage}</p>`;
}

async function loadReturns(orderId) {
    if (!isValidUuid(orderId)) { toast("Ingresa un identificador de orden válido.", "error"); return; }
    try {
        const incidents = await apiCall("GET", `/api/v1/incidents?orderId=${orderId}`);
        renderReturns(incidents, "No hay solicitudes para esta orden.");
    } catch (error) { el("returns-list").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`; }
}

async function loadReturnsHistory() {
    try {
        const incidents = await apiCall("GET", `/api/v1/incidents?customerId=${CUSTOMER_ID}`);
        renderReturns(incidents, "Todavía no tienes devoluciones ni incidencias registradas.");
    } catch (error) { el("returns-list").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`; }
}

function bindEvents() {
    document.querySelectorAll("[data-view]").forEach(tab => tab.addEventListener("click", () => selectView(tab.dataset.view)));
    [el("product-grid"), el("favorites-grid")].forEach(grid => grid.addEventListener("click", async event => {
        const favoriteButton = event.target.closest("[data-favorite-product]");
        if (favoriteButton) { toggleFavorite(favoriteButton.dataset.favoriteProduct); return; }
        const catalogButton = event.target.closest("[data-empty-catalog]");
        if (catalogButton) { selectView("catalog"); return; }
        const button = event.target.closest("[data-add]");
        if (!button) return;
        button.disabled = true;
        try { await setQuantity(button.dataset.add, cartQuantity(button.dataset.add) + 1); toast("Producto agregado.", "ok"); }
        catch (error) { toast(error.message, "error"); }
        finally { button.disabled = false; }
    }));
    el("cart-items").addEventListener("click", async event => { const button = event.target.closest("[data-quantity]"); if (!button) return; button.disabled = true; try { await setQuantity(button.dataset.product, Number(button.dataset.quantity)); } catch (error) { toast(error.message, "error"); } });
    el("catalog-search").addEventListener("input", event => renderCatalog(event.target.value));
    el("client-search-btn").addEventListener("click", () => { selectView("catalog"); el("catalog-start").scrollIntoView({ behavior: "smooth" }); });
    document.querySelector("[data-scroll-products]").addEventListener("click", () => el("catalog-start").scrollIntoView({ behavior: "smooth" }));
    el("confirm-purchase-btn").addEventListener("click", completePurchase);
    // Al volver a la pestaña, el carrito pudo haber cambiado en otro canal.
    document.addEventListener("visibilitychange", () => { if (!document.hidden) refreshActiveCart(); });
    document.querySelectorAll("input[name=delivery]").forEach(radio => radio.addEventListener("change", updateDeliveryFields));
    loadPickupPoints();
    updateDeliveryFields();
    el("tracking-load-btn").addEventListener("click", () => loadTracking(el("tracking-order-input").value));
    el("tracking-order-input").addEventListener("keydown", event => { if (event.key === "Enter") loadTracking(event.currentTarget.value); });
    el("recent-orders").addEventListener("click", event => { const button = event.target.closest("[data-order]"); if (button) { el("tracking-order-input").value = button.dataset.order; loadTracking(button.dataset.order); } });
    el("return-form").addEventListener("submit", submitReturn);
    el("return-order-select").addEventListener("change", fillReturnProducts);
    el("returns-order-input").addEventListener("change", event => { if (event.target.value) loadReturns(event.target.value); });
    el("returns-load-btn").addEventListener("click", () => loadReturns(el("returns-order-input").value));
    el("returns-history-btn").addEventListener("click", loadReturnsHistory);
}

async function initialize() {
    el("correlation-id").textContent = CORRELATION_ID;
    if (!session) return;
    el("session-name").textContent = session.name;
    el("logout-btn").addEventListener("click", sgoLogout);
    bindEvents();
    try {
        await loadCatalogDefinition();
    } catch (error) {
        toast(error.message, "error");
        return;
    }
    renderCatalog(); renderFavorites(); renderCart();
    const lastOrderId = localStorage.getItem(LAST_ORDER_KEY); if (lastOrderId) el("tracking-order-input").value = lastOrderId;
    await Promise.allSettled([ensureCart(), loadAvailability(), loadRecentOrders()]);
    await consumePurchaseIntent();
    const requestedView = location.hash.slice(1);
    if (["catalog", "favorites", "checkout", "tracking", "returns"].includes(requestedView)) selectView(requestedView);
}

initialize();
