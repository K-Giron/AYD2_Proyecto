"use strict";

// Administración de la tienda online (rol ONLINE_STORE): recibe por folio lo
// que el almacén central envía a POS-ONLINE y decide qué se publica en la web,
// con su imagen, precio, categoría y descripción (módulo onlinestore, ADR-015).

const session = sgoRequireSession("ONLINE_STORE");
const CORRELATION_ID = crypto.randomUUID();
let products = [];
let editing = null;
let lastLookedUp = null;

function el(id) { return document.getElementById(id); }
function escapeHtml(value) { return String(value ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])); }
function money(value) { return new Intl.NumberFormat("es-GT", { style: "currency", currency: "GTQ" }).format(Number(value || 0)); }
function formatDate(iso) { return iso ? new Date(iso).toLocaleString("es-GT", { dateStyle: "short", timeStyle: "short" }) : "—"; }
function badge(tone, label) { return `<span class="badge badge-${tone}">${escapeHtml(label)}</span>`; }
function originLabel(origin) { return !origin || origin === "CENTRAL" ? "Almacén central" : origin; }

function toast(message, type = "") {
    const node = document.createElement("div");
    node.className = `toast ${type}`.trim();
    node.textContent = message;
    el("toast-stack").appendChild(node);
    setTimeout(() => node.remove(), 4500);
}

async function api(method, path, body, contentType) {
    const headers = { "X-Correlation-Id": CORRELATION_ID, ...sgoAuthHeaders() };
    let payload;
    if (body instanceof Blob) {
        headers["Content-Type"] = contentType || body.type;
        payload = body;
    } else if (body !== undefined) {
        headers["Content-Type"] = "application/json";
        payload = JSON.stringify(body);
    }
    const response = await fetch(path, { method, headers, body: payload, cache: "no-store" });
    if (response.status === 401) { sgoLogout(); throw new Error("La sesión venció."); }
    const text = await response.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = { message: text }; }
    if (!response.ok) throw new Error(data?.message || `Error HTTP ${response.status}`);
    return data;
}

function table(headers, rows, empty) {
    if (!rows.length) return `<p class="empty-state">${escapeHtml(empty)}</p>`;
    return `<table class="admin-table"><thead><tr>${headers.map(h => `<th class="${h.endsWith("#") ? "num" : ""}">${escapeHtml(h.replace(/#$/, ""))}</th>`).join("")}</tr></thead><tbody>${rows.join("")}</tbody></table>`;
}

// ---------------------------------------------------------------------
// Sesión, pestañas y estado del central
// ---------------------------------------------------------------------
// Inventario (CD-WEB)
// ---------------------------------------------------------------------

let inventoryRows = [];

async function loadInventory() {
    try {
        inventoryRows = await api("GET", "/api/v1/online-store/inventory");
        renderInventory();
    } catch (error) {
        el("inventory-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

function renderInventory() {
    const filter = el("inventory-filter").value.trim().toLowerCase();
    const sum = key => inventoryRows.reduce((total, row) => total + row[key], 0);
    el("inventory-summary").innerHTML = `<span>En mano <strong>${sum("onHand")}</strong></span><span>Reservado <strong>${sum("reserved")}</strong></span>`
        + `<span>Disponible <strong>${sum("available")}</strong></span><span>En tránsito <strong>${sum("inTransit")}</strong></span>`;
    const rows = inventoryRows
        .filter(r => !filter || r.sku.toLowerCase().includes(filter) || r.name.toLowerCase().includes(filter))
        .map(r => `<tr>
            <td>${escapeHtml(r.name)}<span class="sub mono">${escapeHtml(r.sku)}</span></td>
            <td class="num">${r.onHand}</td>
            <td class="num">${r.reserved}</td>
            <td class="num"><strong>${r.available}</strong></td>
            <td class="num">${r.inTransit ? `+${r.inTransit}` : "—"}</td>
            <td>${!r.inCatalog ? badge("warn", "Por recibir") : r.published ? badge("ok", "Publicado") : badge("muted", "Sin publicar")}</td>
        </tr>`);
    el("inventory-table").innerHTML = table(["Producto", "En mano#", "Reservado#", "Disponible#", "En tránsito#", "Catálogo"], rows,
        filter ? "Ningún producto coincide." : "La tienda online aún no tiene mercadería: recibe un folio del almacén central.");
}

el("inventory-filter").addEventListener("input", renderInventory);

// ---------------------------------------------------------------------
// Ventas y facturas
// ---------------------------------------------------------------------

const ORDER_STATUS = {
    PREPARING: ["ok", "En preparación"], IN_TRANSIT: ["ok", "En tránsito"], DELIVERED: ["ok", "Entregada"],
    CONFIRMED: ["ok", "Confirmada"], FISCAL_PENDING: ["warn", "Factura en SAT"], PAYMENT_PENDING: ["warn", "Pago pendiente"],
    PAYMENT_INDETERMINATE: ["warn", "Pago por conciliar"], CREATED: ["muted", "Creada"], INVENTORY_RESERVED: ["muted", "Reservada"],
    PAYMENT_REJECTED: ["error", "Pago rechazado"], FISCAL_REJECTED: ["error", "Factura rechazada"], CANCELLED: ["error", "Cancelada"],
};

function localDate(date = new Date()) {
    const pad = n => String(n).padStart(2, "0");
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

el("sales-from").value = el("sales-to").value = localDate();
el("sales-filter").addEventListener("submit", event => { event.preventDefault(); loadSales(); });

async function loadSales() {
    try {
        const from = el("sales-from").value ? new Date(`${el("sales-from").value}T00:00:00`) : null;
        const to = el("sales-to").value ? new Date(`${el("sales-to").value}T23:59:59.999`) : null;
        // Ventas de la tienda online = órdenes de los canales digitales (la tienda física factura en su propio sistema).
        const orders = (await api("GET", "/api/v1/orders"))
            .filter(o => o.channel !== "STORE")
            .filter(o => (!from || new Date(o.createdAt) >= from) && (!to || new Date(o.createdAt) <= to));
        const billed = orders.filter(o => ["CONFIRMED", "PREPARING", "IN_TRANSIT", "DELIVERED"].includes(o.status));
        el("sales-summary").innerHTML = `<span><strong>${orders.length}</strong> orden(es)</span><span><strong>${billed.length}</strong> facturada(s)</span>`
            + `<span>Total facturado <strong>${money(billed.reduce((t, o) => t + Number(o.total), 0))}</strong></span>`;
        el("sales-table").innerHTML = table(["Fecha", "Orden", "Detalle", "Total#", "Estado", ""], orders.map(o => {
            const status = ORDER_STATUS[o.status] || ["muted", o.status];
            return `<tr>
                <td>${formatDate(o.createdAt)}<span class="sub">${escapeHtml(o.channel)}</span></td>
                <td class="mono">${escapeHtml(o.id.slice(0, 8))}<span class="sub mono">${escapeHtml(o.id)}</span></td>
                <td>${escapeHtml(o.items.map(i => `${i.quantity} × ${i.productName}`).join(", "))}</td>
                <td class="num">${money(o.total)}</td>
                <td>${badge(status[0], status[1])}</td>
                <td>${billed.includes(o) ? `<button class="btn btn-ghost btn-sm" data-invoice="${o.id}">Ver factura</button>` : "—"}</td>
            </tr>`;
        }), "No hay ventas en esas fechas.");
        el("sales-table").querySelectorAll("[data-invoice]").forEach(b => b.addEventListener("click", () => showInvoice(b.dataset.invoice)));
    } catch (error) {
        el("sales-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

const INVOICE_ISSUER = { name: "MegaStore, S.A.", taxId: "1234567-8", address: "Ciudad de Guatemala" };
let invoiceOnScreen = null;

async function showInvoice(orderId) {
    try {
        const [order, invoice] = await Promise.all([
            api("GET", `/api/v1/orders/${orderId}`),
            api("GET", `/api/v1/invoices/orders/${orderId}`),
        ]);
        invoiceOnScreen = invoice;
        const total = Number(invoice.total);
        const base = Math.round(total / 1.12 * 100) / 100;
        const approved = invoice.status === "APPROVED";
        el("invoice-body").innerHTML = `<header>
                <div><h2>${escapeHtml(INVOICE_ISSUER.name)}</h2><div class="muted">NIT ${escapeHtml(INVOICE_ISSUER.taxId)} · ${escapeHtml(INVOICE_ISSUER.address)}</div><div class="muted">Tienda online · canal ${escapeHtml(order.channel)}</div></div>
                <div class="doc-title"><strong>FACTURA ELECTRÓNICA</strong><span class="muted">Documento tributario electrónico</span><br /><span class="stamp ${approved ? "" : "pending"}">${approved ? "CERTIFICADA SAT" : escapeHtml(invoice.status)}</span></div>
            </header>
            <div class="grid">
                <div><span>Serie</span>${escapeHtml(invoice.series)}</div><div><span>Fecha de emisión</span>${formatDate(invoice.issuedAt)}</div>
                <div><span>CUI</span><span class="mono">${escapeHtml(invoice.cui)}</span></div><div><span>Autorización SAT</span><span class="mono">${escapeHtml(invoice.satAuthorization || "Pendiente")}</span></div>
                <div><span>NIT del cliente</span>${escapeHtml(invoice.taxIdentifier)}</div><div><span>Nombre</span>${escapeHtml(invoice.customerName)}</div>
                <div><span>Orden</span><span class="mono">${escapeHtml(order.id)}</span></div><div><span>Certificada</span>${formatDate(invoice.certifiedAt)}</div>
            </div>
            <table><thead><tr><th>Cant.</th><th>Descripción</th><th class="r">Precio</th><th class="r">Importe</th></tr></thead><tbody>
                ${order.items.map(i => `<tr><td>${i.quantity}</td><td>${escapeHtml(i.productName)}</td><td class="r">${money(i.unitPrice)}</td><td class="r">${money(i.lineTotal)}</td></tr>`).join("")}
            </tbody></table>
            <div class="totals"><div><span>Subtotal sin IVA</span><span>${money(base)}</span></div><div><span>IVA (12 %)</span><span>${money(total - base)}</span></div>
                <div class="grand"><span>Total ${escapeHtml(invoice.currency)}</span><span>${money(total)}</span></div></div>
            <footer>Copia de la factura emitida al cliente en la tienda online.</footer>`;
        el("invoice-modal").classList.remove("hidden");
    } catch (error) {
        toast(error.message.includes("404") ? "La factura aún se está certificando con SAT." : error.message, "error");
    }
}

el("invoice-close-btn").addEventListener("click", () => { el("invoice-modal").classList.add("hidden"); invoiceOnScreen = null; });
el("invoice-print-btn").addEventListener("click", () => window.print());
el("invoice-xml-btn").addEventListener("click", () => {
    if (!invoiceOnScreen) return;
    const url = URL.createObjectURL(new Blob([invoiceOnScreen.xml], { type: "application/xml" }));
    const link = Object.assign(document.createElement("a"), { href: url, download: `factura-${invoiceOnScreen.series}-${invoiceOnScreen.cui}.xml` });
    document.body.appendChild(link); link.click(); link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
});

// ---------------------------------------------------------------------
// Traslados por folio
// ---------------------------------------------------------------------

const TRANSFER_STATUS = { PENDIENTE: ["warn", "Pendiente"], ENVIADA: ["ok", "Enviado"], RECHAZADO: ["error", "Rechazado"] };

async function loadTransfers() {
    try {
        const [inventory, transfers] = await Promise.all([
            api("GET", "/api/v1/online-store/inventory"),
            api("GET", "/api/v1/online-store/transfers"),
        ]);
        const chosen = el("transfer-product").value;
        const available = inventory.filter(r => r.available > 0);
        el("transfer-product").innerHTML = available.length
            ? available.map(r => `<option value="${r.productId}">${escapeHtml(r.sku)} · ${escapeHtml(r.name)} (${r.available} disponible)</option>`).join("")
            : `<option value="">Sin existencia disponible para trasladar</option>`;
        if (chosen) el("transfer-product").value = chosen;
        el("transfers-table").innerHTML = table(["Folio", "Producto", "Cantidad#", "Destino", "Estado", "Central"], transfers.map(t => {
            const status = TRANSFER_STATUS[t.syncStatus];
            return `<tr>
                <td class="mono">${escapeHtml(t.folio)}<span class="sub">${formatDate(t.createdAt)}</span></td>
                <td>${escapeHtml(t.name)}<span class="sub mono">${escapeHtml(t.sku)}</span></td>
                <td class="num">−${t.quantity}</td>
                <td>${escapeHtml(t.destination === "CENTRAL" ? "Almacén central" : t.destination)}<span class="sub">${escapeHtml(t.reason || "")}</span></td>
                <td>${badge(status[0], status[1])}${t.lastSyncError ? `<span class="sub">${escapeHtml(t.lastSyncError)}</span>` : ""}</td>
                <td class="mono">${escapeHtml(t.centralFolio || "—")}<span class="sub">${escapeHtml(t.destinationFolio || "")}</span></td>
            </tr>`;
        }), "La tienda online no ha emitido traslados.");
    } catch (error) {
        el("transfers-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
    try {
        const destinations = await api("GET", "/api/v1/online-store/destinations");
        const current = el("transfer-destination").value;
        el("transfer-destination").innerHTML = `<option value="CENTRAL">Almacén central</option>`
            + destinations.map(d => `<option value="${escapeHtml(d.code)}">${escapeHtml(d.code)} · ${escapeHtml(d.name)}</option>`).join("");
        el("transfer-destination").value = current || "CENTRAL";
    } catch {
        // Sin conexión con el central solo se ofrece el central; el folio queda pendiente igual.
    }
}

el("transfer-form").addEventListener("submit", async event => {
    event.preventDefault();
    // Se guarda antes del await: después, event.currentTarget ya es null.
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form).entries());
    if (!values.productId) { toast("No hay producto disponible para trasladar.", "error"); return; }
    form.querySelector("button[type=submit]").disabled = true;
    try {
        const transfer = await api("POST", "/api/v1/online-store/transfers", {
            productId: values.productId, quantity: Number(values.quantity),
            destination: values.destination, reason: values.reason || undefined,
        });
        const status = TRANSFER_STATUS[transfer.syncStatus];
        toast(`Traslado ${transfer.folio}: ${status[1].toLowerCase()}${transfer.centralFolio ? ` (${transfer.centralFolio})` : ""}.`,
            transfer.syncStatus === "RECHAZADO" ? "error" : "ok");
        form.reset();
        loadTransfers();
    } catch (error) {
        toast(error.message, "error");
    } finally {
        form.querySelector("button[type=submit]").disabled = false;
    }
});

// ---------------------------------------------------------------------
// Bitácora de sincronización
// ---------------------------------------------------------------------

const LOG_KIND = { RECEPCION: "Recepción", VENTA: "Venta", TRASLADO: "Traslado", EVENTO_STOCK: "Evento de stock" };

async function loadLog() {
    try {
        const entries = await api("GET", "/api/v1/online-store/sync-log");
        const filter = el("log-filter").value;
        el("log-table").innerHTML = table(["Fecha", "Dirección", "Tipo", "Referencia", "Resultado", "Detalle"], entries
            .filter(e => !filter || (filter === "ERROR" ? e.result === "ERROR" : e.direction === filter))
            .map(e => `<tr>
                <td>${formatDate(e.occurredAt)}</td>
                <td>${e.direction === "SUBIDA" ? "↑ Tienda online → central" : "↓ Central → tienda online"}</td>
                <td>${escapeHtml(LOG_KIND[e.kind] || e.kind)}</td>
                <td class="mono">${escapeHtml(e.reference ? e.reference.slice(0, 18) : "—")}</td>
                <td>${badge(e.result === "OK" ? "ok" : "error", e.result)}</td>
                <td>${escapeHtml(e.detail)}</td>
            </tr>`), "Sin movimientos de sincronización todavía.");
    } catch (error) {
        el("log-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

el("log-filter").addEventListener("change", loadLog);

// ---------------------------------------------------------------------

if (session) {
    el("session-name").textContent = session.email || "Tienda online";
    el("logout-btn").addEventListener("click", sgoLogout);
}

document.querySelectorAll(".admin-tab").forEach(tab => tab.addEventListener("click", () => {
    document.querySelectorAll(".admin-tab").forEach(t => { t.classList.toggle("active", t === tab); t.setAttribute("aria-selected", String(t === tab)); });
    document.querySelectorAll(".admin-view").forEach(v => v.classList.toggle("active", v.id === `view-${tab.dataset.tab}`));
    refreshTab(tab.dataset.tab);
}));

const TAB_LOADERS = {
    receiving: () => loadReceiving(),
    inventory: () => loadInventory(),
    catalog: () => loadCatalog(),
    sales: () => loadSales(),
    transfers: () => loadTransfers(),
    log: () => loadLog(),
};

function activeTab() { return document.querySelector(".admin-tab.active").dataset.tab; }
function refreshTab(tab) { (TAB_LOADERS[tab] || loadReceiving)(); }

async function loadInfo() {
    try {
        const info = await api("GET", "/api/v1/online-store/info");
        el("central-dot").className = `status-dot ${info.centralReachable ? "up" : info.lastContactAt ? "down" : "unknown"}`;
        el("central-text").textContent = info.centralReachable ? "Almacén central: conectado"
            : info.lastContactAt ? "Almacén central: sin conexión" : "Almacén central: conectando…";
        el("transit-count").textContent = info.shipmentsInTransit;
        el("transit-count").classList.toggle("hidden", !info.shipmentsInTransit);
    } catch (error) {
        el("central-dot").className = "status-dot down";
        el("central-text").textContent = error.message;
    }
}

el("sync-btn").addEventListener("click", async () => {
    el("sync-btn").disabled = true;
    try {
        const result = await api("POST", "/api/v1/online-store/sync");
        toast(result.newShipments ? `${result.newShipments} envío(s) nuevo(s) del central: confírmalos en Recepción.` : "Sin envíos nuevos del central.", "ok");
        loadReceiving();
    } catch (error) {
        toast(error.message, "error");
    } finally {
        el("sync-btn").disabled = false;
        loadInfo();
    }
});

// ---------------------------------------------------------------------
// Recepción por folio
// ---------------------------------------------------------------------

async function loadReceiving() {
    try {
        const [transit, history] = await Promise.all([
            api("GET", "/api/v1/online-store/receiving"),
            api("GET", "/api/v1/online-store/receiving/history"),
        ]);
        el("transit-count").textContent = transit.length;
        el("transit-count").classList.toggle("hidden", !transit.length);
        el("transit-list").innerHTML = transit.length
            ? transit.map(s => `<div class="transit-item">
                <div><button class="link-btn mono" data-folio="${escapeHtml(s.folio)}">${escapeHtml(s.folio)}</button>
                    <span class="sub">${escapeHtml(s.lines.map(l => `${l.quantity} × ${l.name}`).join(", "))}</span></div>
                <span class="sub">${formatDate(s.sentAt)}</span></div>`).join("")
            : `<p class="empty-state">No hay mercadería en camino a la tienda online.</p>`;
        el("transit-list").querySelectorAll("[data-folio]").forEach(b => b.addEventListener("click", () => {
            el("folio-input").value = b.dataset.folio;
            lookupFolio();
        }));
        el("history-table").innerHTML = table(["Folio de envío", "Artículo", "Cantidad#", "Estado", "Enviado", "Recibido"],
            history.map(r => `<tr>
                <td class="mono">${escapeHtml(r.shipmentFolio)}<span class="sub">${escapeHtml(r.folio)} · ${escapeHtml(originLabel(r.origin))}</span></td>
                <td>${escapeHtml(r.name)}<span class="sub mono">${escapeHtml(r.sku)}</span></td>
                <td class="num">+${r.quantity}</td>
                <td>${r.status === "RECIBIDO" ? badge("ok", "Recibido") : badge("warn", "En tránsito")}</td>
                <td>${formatDate(r.occurredAt)}</td>
                <td>${r.status === "RECIBIDO" ? `${formatDate(r.receivedAt)}<span class="sub">${escapeHtml(r.receivedBy || "")}</span>` : "—"}</td>
            </tr>`), "El almacén central todavía no ha enviado nada a la tienda online.");
    } catch (error) {
        el("transit-list").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

async function lookupFolio() {
    const folio = el("folio-input").value.trim().toUpperCase();
    if (!folio) return;
    lastLookedUp = folio;
    el("folio-detail").innerHTML = `<p class="hint">Buscando ${escapeHtml(folio)}…</p>`;
    try {
        renderShipment(await api("GET", `/api/v1/online-store/receiving/${encodeURIComponent(folio)}`));
        loadReceiving();
    } catch (error) {
        el("folio-detail").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

function renderShipment(shipment) {
    if (shipment.status === "NO_ENCONTRADO") {
        el("folio-detail").innerHTML = `<p class="empty-state">El folio ${escapeHtml(shipment.folio)} no trae mercadería para la tienda online. Revisa el número, o si el central acaba de emitirlo, espera unos segundos y vuelve a buscar.</p>`;
        return;
    }
    const pending = shipment.status === "EN_TRANSITO";
    el("folio-detail").innerHTML = `
        <div class="shipment-head"><strong class="mono">${escapeHtml(shipment.folio)}</strong>
            <span>Desde: ${escapeHtml(originLabel(shipment.origin))}</span><span>Enviado: ${formatDate(shipment.sentAt)}</span><span>${shipment.units} unidad(es)</span></div>
        ${table(["Artículo", "Precio del central#", "Cantidad#", "Estado"], shipment.lines.map(l => `<tr>
            <td>${escapeHtml(l.name)}<span class="sub mono">${escapeHtml(l.sku)}</span></td>
            <td class="num">${money(l.price)}</td>
            <td class="num">${l.quantity}</td>
            <td>${l.status === "RECIBIDO" ? badge("ok", "Recibido") : badge("warn", "En tránsito")}</td></tr>`), "")}
        ${pending
            ? `<div class="editor-actions" style="margin-top:.9rem"><button class="btn btn-primary" id="confirm-btn">Confirmar que llegó (${shipment.units} unidades)</button></div>`
            : `<p class="hint">Ya recibido por ${escapeHtml(shipment.receivedBy || "—")} el ${formatDate(shipment.receivedAt)}.</p>`}`;
    if (pending) {
        el("confirm-btn").addEventListener("click", async () => {
            el("confirm-btn").disabled = true;
            try {
                const result = await api("POST", `/api/v1/online-store/receiving/${encodeURIComponent(shipment.folio)}/confirm`);
                toast(`${result.folio} recibido: +${result.units} en el inventario de la tienda online. Publícalo en Catálogo.`, "ok");
                renderShipment(result);
                el("folio-input").value = "";
                lastLookedUp = null;
                loadReceiving();
                loadInfo();
            } catch (error) {
                toast(error.message, "error");
                el("confirm-btn").disabled = false;
            }
        });
    }
}

el("folio-form").addEventListener("submit", event => { event.preventDefault(); lookupFolio(); });
// Tab (o salir del campo) carga el folio.
el("folio-input").addEventListener("change", () => {
    if (el("folio-input").value.trim().toUpperCase() !== lastLookedUp) lookupFolio();
});

// ---------------------------------------------------------------------
// Catálogo
// ---------------------------------------------------------------------

async function loadCatalog() {
    try {
        products = await api("GET", "/api/v1/online-store/products");
        renderCatalog();
        const categories = [...new Set(products.map(p => p.category).filter(Boolean))];
        el("category-options").innerHTML = categories.map(c => `<option value="${escapeHtml(c)}"></option>`).join("");
    } catch (error) {
        el("catalog-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

function productStatus(p) {
    if (p.published) return p.available > 0 ? badge("ok", "Publicado") : badge("warn", "Publicado · agotado");
    return badge("muted", p.image ? "Sin publicar" : "Sin publicar · falta imagen");
}

function renderCatalog() {
    const filter = el("catalog-filter").value.trim().toLowerCase();
    const rows = products
        .filter(p => !filter || p.sku.toLowerCase().includes(filter) || p.name.toLowerCase().includes(filter))
        .map(p => `<tr>
            <td>${p.image ? `<img class="thumb" src="${escapeHtml(p.image)}" alt="" />` : `<span class="thumb empty">sin imagen</span>`}</td>
            <td>${escapeHtml(p.name)}<span class="sub mono">${escapeHtml(p.sku)}${p.category ? " · " + escapeHtml(p.category) : ""}</span></td>
            <td class="num">${money(p.price)}</td>
            <td class="num">${p.available}<span class="sub">${p.onHand} en mano · ${p.reserved} reservado</span></td>
            <td>${productStatus(p)}</td>
            <td><button class="btn btn-ghost btn-sm" data-edit="${p.id}">Editar</button></td>
        </tr>`);
    el("catalog-table").innerHTML = table(["", "Producto", "Precio#", "Disponible#", "Estado", ""], rows,
        filter ? "Ningún producto coincide." : "Todavía no hay productos: recibe un folio del almacén central en Recepción.");
    el("catalog-table").querySelectorAll("[data-edit]").forEach(b => b.addEventListener("click", () => openEditor(b.dataset.edit)));
}

el("catalog-filter").addEventListener("input", renderCatalog);

// ---------------------------------------------------------------------
// Editor de ficha: datos, imagen y publicación
// ---------------------------------------------------------------------

function openEditor(productId) {
    editing = products.find(p => p.id === productId);
    if (!editing) return;
    const form = el("editor-form");
    form.name.value = editing.name;
    form.category.value = editing.category || "";
    form.price.value = Number(editing.price).toFixed(2);
    form.description.value = editing.description || "";
    el("editor-sku").textContent = `${editing.sku} · id ${editing.id}`;
    renderEditorState();
    el("editor").classList.remove("hidden");
    form.name.focus();
}

function renderEditorState() {
    el("image-preview").innerHTML = editing.image ? `<img src="${escapeHtml(editing.image)}" alt="Imagen de ${escapeHtml(editing.name)}" />` : "<span>Sin imagen</span>";
    el("remove-image-btn").disabled = !editing.image;
    el("toggle-publish-btn").textContent = editing.published ? "Despublicar" : "Publicar";
    el("editor-status").innerHTML = productStatus(editing);
    el("editor-stock").textContent = `Inventario de la tienda online: ${editing.onHand} en mano, ${editing.reserved} reservado por órdenes en curso, ${editing.available} disponible para vender.`;
}

function closeEditor() {
    el("editor").classList.add("hidden");
    editing = null;
    loadCatalog();
}

el("editor").querySelector("[data-close]").addEventListener("click", closeEditor);
el("editor").addEventListener("keydown", event => { if (event.key === "Escape") closeEditor(); });

el("editor-form").addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.currentTarget;
    try {
        editing = await api("PUT", `/api/v1/online-store/products/${editing.id}`, {
            name: form.name.value.trim(),
            category: form.category.value.trim() || undefined,
            price: Number(form.price.value),
            description: form.description.value.trim() || undefined,
        });
        renderEditorState();
        toast(editing.published ? "Cambios guardados: ya se ven en la web." : "Cambios guardados.", "ok");
    } catch (error) {
        toast(error.message, "error");
    }
});

el("image-input").addEventListener("change", async event => {
    const file = event.target.files[0];
    event.target.value = "";
    if (!file) return;
    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) { toast("La imagen debe ser JPG, PNG o WebP.", "error"); return; }
    if (file.size > 2 * 1024 * 1024) { toast("La imagen pesa más de 2 MB.", "error"); return; }
    try {
        editing = await api("PUT", `/api/v1/online-store/products/${editing.id}/image`, file, file.type);
        renderEditorState();
        toast("Imagen cargada.", "ok");
    } catch (error) {
        toast(error.message, "error");
    }
});

el("remove-image-btn").addEventListener("click", async () => {
    try {
        const wasPublished = editing.published;
        editing = await api("DELETE", `/api/v1/online-store/products/${editing.id}/image`);
        renderEditorState();
        toast(wasPublished ? "Imagen quitada: el producto se despublicó (no se publica sin imagen)." : "Imagen quitada.", "ok");
    } catch (error) {
        toast(error.message, "error");
    }
});

el("toggle-publish-btn").addEventListener("click", async () => {
    try {
        editing = await api("POST", `/api/v1/online-store/products/${editing.id}/${editing.published ? "unpublish" : "publish"}`);
        renderEditorState();
        toast(editing.published ? "Publicado: ya aparece en el catálogo de la web." : "Despublicado: ya no aparece en la web.", "ok");
    } catch (error) {
        toast(error.message, "error");
    }
});

// ---------------------------------------------------------------------

if (session) {
    loadInfo();
    loadReceiving();
    setInterval(() => {
        loadInfo();
        // No se redibuja mientras se edita una ficha o se ve una factura.
        if (el("editor").classList.contains("hidden") && el("invoice-modal").classList.contains("hidden")
                && activeTab() !== "transfers") {
            refreshTab(activeTab());
        }
    }, 15000);
}
