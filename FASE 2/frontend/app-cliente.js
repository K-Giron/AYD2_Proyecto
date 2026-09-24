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
    const response = await fetch("catalog.json", { cache: "no-store" });
    if (!response.ok) throw new Error("No se pudo cargar el catálogo de MegaStore.");
    CATALOG = await response.json();
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

async function ensureCart() {
    if (activeCart?.status === "ACTIVE") return activeCart;
    let context = storedCartContext();
    if (!context) {
        context = { idempotencyKey: `cart-${crypto.randomUUID()}` };
        localStorage.setItem(CART_CONTEXT_KEY, JSON.stringify(context));
    }
    activeCart = await apiCall("POST", "/api/v1/carts", { customerId: CUSTOMER_ID, channel: "WEB" }, { "Idempotency-Key": context.idempotencyKey });
    renderCatalog(); renderCart();
    return activeCart;
}

async function loadAvailability() {
    const results = await Promise.allSettled(CATALOG.map(product => apiCall("GET", `/api/v1/inventory/stock?productId=${product.id}`)));
    results.forEach((result, index) => {
        if (result.status === "fulfilled") catalogAvailability.set(CATALOG[index].id, result.value.reduce((sum, stock) => sum + Number(stock.quantityAvailable ?? stock.availableQuantity ?? 0), 0));
    });
    const connected = results.filter(result => result.status === "fulfilled").length;
    el("catalog-status").textContent = connected === CATALOG.length ? "Inventario en línea" : "Disponibilidad parcial";
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
        const product = productById(item.productId) || { mark: "MS" };
        return `<div class="cart-line"><div class="cart-swatch" aria-hidden="true">${product.mark}</div><div><div class="cart-line-name">${escapeHtml(item.productName)}</div><div class="cart-line-price">${money(item.unitPrice)} por unidad</div></div><div class="qty-control"><button aria-label="Restar uno" data-quantity="${item.quantity - 1}" data-product="${item.productId}">−</button><span>${item.quantity}</span><button aria-label="Sumar uno" data-quantity="${item.quantity + 1}" data-product="${item.productId}">+</button></div></div>`;
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
    if (name === "checkout") renderCart();
    if (name === "tracking") loadRecentOrders();
    if (location.hash !== `#${name}`) history.replaceState(null, "", `#${name}`);
    window.scrollTo({ top: 0, behavior: "smooth" });
}

const FLOW_STEPS = ["Orden creada", "Inventario reservado", "Pago procesado", "Documento fiscal", "Despacho iniciado"];
function renderFlow(current, completed = false) {
    el("checkout-result").innerHTML = `<div class="flow-status">${FLOW_STEPS.map((label, index) => `<div class="flow-step ${completed || index < current ? "done" : index === current ? "current" : ""}">${label}</div>`).join("")}</div>`;
}

function sleep(milliseconds) { return new Promise(resolve => setTimeout(resolve, milliseconds)); }
async function waitForSaga(orderId, sagaKey, payload) {
    for (let attempt = 0; attempt < 12; attempt++) {
        const result = await apiCall("POST", `/api/v1/orders/${orderId}/saga`, payload,
            { "Idempotency-Key": sagaKey, "X-Actor": `customer:${CUSTOMER_ID}` });
        if (result.sagaStatus !== "WAITING_FISCAL") return result;
        await sleep(500);
    }
    return null;
}

async function completePurchase() {
    const button = el("confirm-purchase-btn");
    const fiscalName = el("fiscal-name").value.trim();
    const taxIdentifier = el("fiscal-tax-id").value.trim();
    const destination = el("checkout-destination").value.trim();
    if (!fiscalName || !taxIdentifier || !destination) { toast("Completa destino y datos fiscales.", "error"); return; }
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
            paymentMethod: "CARD", currency: "GTQ", paymentReference: "tok_demo_4242",
            taxIdentifier, customerName: fiscalName, destination
        });
        if (!saga) throw new Error("SAT sigue procesando el documento. Puedes reanudar la compra sin duplicar cobros.");
        if (saga.sagaStatus !== "COMPLETED") throw new Error(saga.detail || `La Saga quedó en ${saga.sagaStatus}.`);
        renderFlow(5, true);
        el("checkout-result").innerHTML += `<div class="checkout-success"><strong>Compra confirmada y en preparación</strong><br />Orden ${escapeHtml(order.id)}<br />CUI ${escapeHtml(saga.fiscalCui)} · Serie ${escapeHtml(saga.fiscalSeries)}<br />Guía en proceso para ${escapeHtml(destination)}</div>`;
        localStorage.removeItem(CART_CONTEXT_KEY); activeCart = null; await loadRecentOrders(); renderCatalog(); renderCart();
    } catch (error) {
        el("checkout-result").innerHTML = `<div class="checkout-error"><strong>No se completó el flujo.</strong><br />${escapeHtml(error.message)}</div>`;
        button.disabled = false;
    }
}

const STATUS_TONE = { DELIVERED:"ok",APPROVED:"ok",CONFIRMED:"ok",FISCAL_APPROVED:"ok",SENT:"ok",IN_TRANSIT:"warn",PREPARING:"warn",FISCAL_PENDING:"warn",PAYMENT_INDETERMINATE:"warn",PAYMENT_REJECTED:"error",REJECTED:"error",CANCELLED:"error",FAILED:"error" };
function badge(status) { return `<span class="badge badge-${STATUS_TONE[status] || "muted"}">${escapeHtml(status || "SIN DATO")}</span>`; }
const ORDER_STEPS = ["Confirmado", "Preparando", "En tránsito", "Entregado"];
function trackingIndex(order, shipments) {
    const shipment = shipments[0];
    if (shipment?.status === "DELIVERED" || order?.status === "DELIVERED") return 3;
    if (shipment?.status === "IN_TRANSIT" || order?.status === "IN_TRANSIT") return 2;
    if (["PREPARING","LABEL_REQUESTED","LABEL_ISSUED","READY_FOR_PICKUP"].includes(shipment?.status || order?.status)) return 1;
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
        apiCall("GET", `/api/v1/orders/${orderId}`), apiCall("GET", `/api/v1/shipments?orderId=${orderId}`), apiCall("GET", `/api/v1/notifications?sourceAggregateId=${orderId}`), apiCall("GET", `/api/v1/payment-attempts?orderId=${orderId}`), apiCall("GET", `/api/v1/fiscal-documents/orders/${orderId}`)
    ]);
    const order = orderResult.status === "fulfilled" ? orderResult.value : null;
    const shipments = shipmentsResult.status === "fulfilled" ? shipmentsResult.value : [];
    const notifications = notificationsResult.status === "fulfilled" ? notificationsResult.value : [];
    const payments = paymentsResult.status === "fulfilled" ? paymentsResult.value : [];
    const fiscal = fiscalResult.status === "fulfilled" ? fiscalResult.value : [];
    if (!order) { el("tracking-result").innerHTML = `<p class="empty-state">No encontramos esa orden.</p>`; return; }
    const index = trackingIndex(order, shipments);
    const events = [
        `<div class="track-event"><span>Orden · ${formatDate(order.createdAt)}</span>${badge(order.status)}</div>`,
        ...payments.map(item => `<div class="track-event"><span>PayKlik · ${formatDate(item.processedAt)}</span>${badge(item.status)}</div>`),
        ...fiscal.map(item => `<div class="track-event"><span>SAT · ${escapeHtml(item.cui)}</span>${badge(item.status)}</div>`),
        ...shipments.map(item => `<div class="track-event"><span>Despacho · ${escapeHtml(item.destination || "Destino registrado")}</span>${badge(item.status)}</div>`),
        ...notifications.map(item => `<div class="track-event"><span>${escapeHtml(item.channel)} · ${escapeHtml(item.eventType)}</span>${badge(item.status)}</div>`)
    ];
    el("tracking-result").innerHTML = `<article class="order-track-card"><p class="eyebrow">Orden trazable</p><h2>${escapeHtml(order.id)}</h2><div class="stepper">${ORDER_STEPS.map((label,i) => `<div class="step ${i < index ? "done" : i === index ? "current" : ""}"><div class="step-dot"></div>${label}</div>`).join("")}</div><h3>Actividad consolidada</h3><div class="track-events">${events.join("")}</div></article>`;
}

async function submitReturn(event) {
    event.preventDefault(); const form = event.currentTarget; const values = Object.fromEntries(new FormData(form).entries());
    if (!isValidUuid(values.orderId)) { toast("Ingresa un identificador de orden válido.", "error"); return; }
    try {
        await apiCall("POST", "/api/v1/incidents", { orderId: values.orderId, type: values.type, reason: values.reason, partial: form.elements.partial.checked, items: [{ sku: values.sku, quantity: Number(values.quantity) }] });
        toast("Solicitud registrada.", "ok"); el("returns-order-input").value = values.orderId; form.reset(); await loadReturns(values.orderId);
    } catch (error) { toast(error.message, "error"); }
}

async function loadReturns(orderId) {
    if (!isValidUuid(orderId)) { toast("Ingresa un identificador de orden válido.", "error"); return; }
    try {
        const incidents = await apiCall("GET", `/api/v1/incidents?orderId=${orderId}`);
        el("returns-list").innerHTML = incidents.length ? incidents.map(item => `<div class="track-event"><span>${escapeHtml(item.reason)} · ${formatDate(item.createdAt)}</span>${badge(item.status)}</div>`).join("") : `<p class="empty-state">No hay solicitudes para esta orden.</p>`;
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
    el("tracking-load-btn").addEventListener("click", () => loadTracking(el("tracking-order-input").value));
    el("tracking-order-input").addEventListener("keydown", event => { if (event.key === "Enter") loadTracking(event.currentTarget.value); });
    el("recent-orders").addEventListener("click", event => { const button = event.target.closest("[data-order]"); if (button) { el("tracking-order-input").value = button.dataset.order; loadTracking(button.dataset.order); } });
    el("return-form").addEventListener("submit", submitReturn);
    el("returns-load-btn").addEventListener("click", () => loadReturns(el("returns-order-input").value));
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
