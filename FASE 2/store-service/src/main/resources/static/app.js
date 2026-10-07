// Caja de la tienda física. Todo se guarda en el sistema de la tienda
// (su propia base de datos); el central solo recibe las facturas y
// traslados después, y entrega las recepciones. Vender no depende de él.

function el(id) { return document.getElementById(id); }

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

function money(value) {
    return "Q" + Number(value || 0).toLocaleString("es-GT", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatDate(iso) {
    if (!iso) return "—";
    return new Date(iso).toLocaleString("es-GT", { dateStyle: "short", timeStyle: "medium" });
}

function badge(tone, label) { return `<span class="badge badge-${tone}">${escapeHtml(label)}</span>`; }

function table(headers, rows, emptyMessage) {
    if (!rows.length) return `<p class="empty-state">${escapeHtml(emptyMessage)}</p>`;
    return `<table><thead><tr>${headers.map(h => `<th>${escapeHtml(h)}</th>`).join("")}</tr></thead><tbody>${rows.join("")}</tbody></table>`;
}

function toast(message, type) {
    const node = document.createElement("div");
    node.className = `toast ${type || ""}`;
    node.textContent = message;
    el("toast-stack").appendChild(node);
    setTimeout(() => node.remove(), 4500);
}

async function api(method, path, body) {
    const response = await fetch(path, {
        method,
        headers: body === undefined ? {} : { "Content-Type": "application/json" },
        body: body === undefined ? undefined : JSON.stringify(body),
        cache: "no-store",
    });
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) throw new Error((data && data.message) || `Error ${response.status}`);
    return data;
}

// El vendedor de turno vive solo en esta pestaña del navegador.
function storedSeller() { try { return sessionStorage.getItem("store.seller"); } catch { return null; } }
function storeSeller(name) { try { name ? sessionStorage.setItem("store.seller", name) : sessionStorage.removeItem("store.seller"); } catch { /* sin almacenamiento */ } }

let seller = storedSeller();
let store = { code: "", name: "" };
let ticket = [];               // [{articleId, sku, name, price, quantity, onHand}]
let paymentMethod = "EFECTIVO";
let webCart = null;            // {username, fullName, cartId}
let stockCache = [];

const SALE_STATUS = { PENDIENTE: ["warn", "Pendiente"], ENVIADA: ["ok", "Enviada"], ERROR: ["error", "Rechazada"] };
const TRANSFER_STATUS = { PENDIENTE: ["warn", "Pendiente"], ENVIADA: ["ok", "Enviado"], RECHAZADO: ["error", "Rechazado"] };
const PAYMENT_LABEL = { EFECTIVO: "Efectivo", TARJETA: "Tarjeta", TRANSFERENCIA: "Transferencia" };

// ---------------------------------------------------------------------
// Vendedor y navegación
// ---------------------------------------------------------------------

function showSeller() {
    el("seller-name").textContent = seller || "—";
    el("seller-avatar").textContent = (seller || "··").slice(0, 2).toUpperCase();
    el("seller-gate").classList.toggle("hidden", !!seller);
    if (seller) el("scan-input").focus();
}

el("seller-form").addEventListener("submit", event => {
    event.preventDefault();
    seller = new FormData(event.currentTarget).get("seller").trim();
    if (!seller) return;
    storeSeller(seller);
    event.currentTarget.reset();
    showSeller();
});

el("change-seller-btn").addEventListener("click", () => { seller = null; storeSeller(null); showSeller(); });

document.querySelectorAll(".nav-btn").forEach(button => {
    button.addEventListener("click", () => {
        document.querySelectorAll(".nav-btn").forEach(b => b.classList.remove("active"));
        document.querySelectorAll(".view").forEach(v => v.classList.remove("active"));
        button.classList.add("active");
        el(`view-${button.dataset.tab}`).classList.add("active");
        el("topbar-title").textContent = button.dataset.title;
        el("topbar-lede").textContent = button.dataset.lede;
        refreshTab(button.dataset.tab);
    });
});

function refreshTab(tab) {
    if (tab === "receiving") loadReceiving();
    if (tab === "inventory") loadInventory();
    if (tab === "transfers") loadTransfers();
    if (tab === "sales") loadSales();
    if (tab === "log") loadLog();
    if (tab === "weborders") loadWebOrders();
    if (tab === "sale") el("scan-input").focus();
}

function activeTab() { return document.querySelector(".nav-btn.active").dataset.tab; }

// ---------------------------------------------------------------------
// Estado de la tienda y sincronización
// ---------------------------------------------------------------------

async function loadInfo() {
    try {
        const info = await api("GET", "/api/store/info");
        store = info;
        document.title = `${info.code} · ${info.name}`;
        el("brand-name").textContent = info.name;
        el("brand-code").textContent = `${info.code} · sistema de tienda`;
        el("gate-store").textContent = info.name;
        el("brand-mark").textContent = el("gate-mark").textContent = info.code.replace(/[^0-9A-Z]/g, "").slice(-2) || "T";
        el("central-dot").className = `dot ${info.centralReachable ? "ok" : (info.lastContactAt ? "error" : "")}`;
        el("central-status").textContent = info.centralReachable ? "Central: conectado" : (info.lastContactAt ? "Central: sin conexión" : "Central: conectando…");
        el("pending-status").textContent = `${info.pendingInvoices} factura(s) y ${info.pendingTransfers} traslado(s) por subir`;
        updateInTransitCount(info.shipmentsInTransit || 0);
    } catch (error) {
        el("central-status").textContent = "Sistema de la tienda no responde";
        el("central-dot").className = "dot error";
    }
}

el("sync-btn").addEventListener("click", async () => {
    el("sync-btn").disabled = true;
    try {
        const result = await api("POST", "/api/store/sync");
        toast(result.receiptsApplied
            ? `${result.receiptsApplied} envío(s) nuevo(s) en camino: confírmalos en Recepción.`
            : "Sincronizado: no hay envíos nuevos.", "ok");
    } catch (error) {
        toast(error.message, "error");
    } finally {
        el("sync-btn").disabled = false;
        await loadInfo();
        refreshTab(activeTab());
    }
});

// ---------------------------------------------------------------------
// Venta: escáner, ticket, cobro
// ---------------------------------------------------------------------

// Un solo campo para el escáner y la búsqueda manual: el código exacto + Enter
// agrega directo; al escribir parte del código, nombre o descripción se
// muestran coincidencias que se eligen con flechas, Enter o clic.
let searchResults = [];
let searchActive = -1;
let searchTimer = null;
let searchSeq = 0;

function scanQuantity() { return Math.max(1, parseInt(el("scan-qty").value, 10) || 1); }

function closeSearch() {
    searchResults = [];
    searchActive = -1;
    el("search-results").classList.add("hidden");
    el("search-results").innerHTML = "";
    el("scan-input").setAttribute("aria-expanded", "false");
}

function highlight(text, needle) {
    const safe = escapeHtml(text || "");
    if (!needle) return safe;
    const index = (text || "").toLowerCase().indexOf(needle.toLowerCase());
    if (index < 0) return safe;
    return escapeHtml(text.slice(0, index)) + "<mark>" + escapeHtml(text.slice(index, index + needle.length)) + "</mark>"
        + escapeHtml(text.slice(index + needle.length));
}

function renderSearch(needle) {
    const box = el("search-results");
    if (!searchResults.length) {
        box.innerHTML = `<div class="search-empty">Nada en el almacén de la tienda coincide con "${escapeHtml(needle)}".</div>`;
    } else {
        box.innerHTML = searchResults.map((a, i) => `<button type="button" role="option" class="search-item ${i === searchActive ? "active" : ""}"
                data-index="${i}" ${a.quantity > 0 ? "" : "disabled"} aria-selected="${i === searchActive}">
            <span class="si-name">${highlight(a.name, needle)}</span>
            <span class="si-price">${money(a.price)}</span>
            <span class="si-meta"><span class="mono">${highlight(a.sku, needle)}</span>${a.description ? " · " + highlight(a.description, needle) : ""}</span>
            <span class="si-stock">${a.quantity > 0 ? `${a.quantity} en tienda` : "Sin existencia"}</span>
        </button>`).join("");
        box.querySelectorAll(".search-item").forEach(item => item.addEventListener("mousedown", event => {
            event.preventDefault();
            pickSearchResult(Number(item.dataset.index));
        }));
        box.querySelector(".search-item.active")?.scrollIntoView({ block: "nearest" });
    }
    box.classList.remove("hidden");
    el("scan-input").setAttribute("aria-expanded", "true");
}

async function runSearch() {
    const needle = el("scan-input").value.trim();
    if (needle.length < 2) { closeSearch(); return; }
    const seq = ++searchSeq;
    try {
        const results = await api("GET", `/api/store/articles/search?q=${encodeURIComponent(needle)}`);
        if (seq !== searchSeq) return;  // llegó tarde: ya se escribió otra cosa
        searchResults = results;
        searchActive = results.findIndex(a => a.quantity > 0);
        renderSearch(needle);
    } catch (error) {
        closeSearch();
    }
}

function pickSearchResult(index) {
    const article = searchResults[index];
    if (!article || article.quantity <= 0) return;
    addToTicket(article, scanQuantity());
    resetScan();
}

function resetScan() {
    closeSearch();
    searchSeq++;
    clearTimeout(searchTimer);
    el("scan-input").value = "";
    el("scan-qty").value = 1;
    el("scan-input").focus();
}

el("scan-input").addEventListener("input", () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(runSearch, 200);
});

el("scan-input").addEventListener("keydown", event => {
    const open = !el("search-results").classList.contains("hidden") && searchResults.length;
    if (event.key === "Escape") { closeSearch(); return; }
    if (!open || (event.key !== "ArrowDown" && event.key !== "ArrowUp")) return;
    event.preventDefault();
    const step = event.key === "ArrowDown" ? 1 : -1;
    let next = searchActive;
    for (let i = 0; i < searchResults.length; i++) {
        next = (next + step + searchResults.length) % searchResults.length;
        if (searchResults[next].quantity > 0) break;
    }
    searchActive = next;
    renderSearch(el("scan-input").value.trim());
});

el("scan-input").addEventListener("blur", () => setTimeout(closeSearch, 150));

el("scan-form").addEventListener("submit", async event => {
    event.preventDefault();
    const text = el("scan-input").value.trim();
    if (!text) return;
    clearTimeout(searchTimer);
    // 1) Código exacto (escáner): agrega directo, aunque la lista esté abierta.
    try {
        const article = await api("GET", `/api/store/articles/by-sku/${encodeURIComponent(text)}`);
        addToTicket(article, scanQuantity());
        resetScan();
        return;
    } catch {
        // no es un código exacto: se usa la búsqueda
    }
    // 2) Lo elegido en la lista, o la única coincidencia con existencia.
    if (searchResults.length && searchActive >= 0) { pickSearchResult(searchActive); return; }
    const results = await api("GET", `/api/store/articles/search?q=${encodeURIComponent(text)}`).catch(() => []);
    const sellable = results.filter(a => a.quantity > 0);
    if (sellable.length === 1) { addToTicket(sellable[0], scanQuantity()); resetScan(); return; }
    if (!results.length) {
        toast(`"${text}" no está en el almacén de esta tienda. Solo se vende lo que el central envió por folio.`, "error");
        el("scan-input").select();
        return;
    }
    searchResults = results;
    searchActive = results.findIndex(a => a.quantity > 0);
    renderSearch(text);
    toast(sellable.length ? "Hay varias coincidencias: elige una de la lista." : "Lo encontrado no tiene existencia en la tienda.", sellable.length ? "" : "error");
});

function addToTicket(article, quantity) {
    const line = ticket.find(l => l.articleId === article.articleId);
    const wanted = (line ? line.quantity : 0) + quantity;
    if (wanted > article.quantity) {
        toast(`Solo hay ${article.quantity} unidad(es) de ${article.name} en la tienda.`, "error");
        return;
    }
    if (line) {
        line.quantity = wanted;
        line.onHand = article.quantity;
    } else {
        ticket.push({ articleId: article.articleId, sku: article.sku, name: article.name, price: Number(article.price), quantity, onHand: article.quantity });
    }
    renderTicket();
}

function ticketTotal() { return ticket.reduce((sum, l) => sum + l.price * l.quantity, 0); }

function renderTicket() {
    const rows = ticket.map((l, i) => `<tr>
        <td><span class="cell-primary">${escapeHtml(l.name)}</span><span class="cell-sub mono">${escapeHtml(l.sku)} · ${l.onHand} en tienda</span></td>
        <td class="num">${money(l.price)}</td>
        <td><span class="qty-control">
            <button class="btn btn-ghost btn-sm" data-dec="${i}" aria-label="Quitar una">−</button>
            <span class="pill-qty">${l.quantity}</span>
            <button class="btn btn-ghost btn-sm" data-inc="${i}" aria-label="Agregar una">+</button>
        </span></td>
        <td class="num">${money(l.price * l.quantity)}</td>
        <td><button class="btn btn-ghost btn-sm" data-del="${i}">Quitar</button></td>
    </tr>`);
    el("ticket-lines").innerHTML = table(["Artículo", "Precio", "Cantidad", "Subtotal", ""], rows, "El ticket está vacío. Escanea un artículo.");
    el("ticket-lines").querySelectorAll("[data-inc]").forEach(b => b.addEventListener("click", () => {
        const l = ticket[b.dataset.inc];
        if (l.quantity >= l.onHand) { toast(`Solo hay ${l.onHand} unidad(es) en la tienda.`, "error"); return; }
        l.quantity++; renderTicket();
    }));
    el("ticket-lines").querySelectorAll("[data-dec]").forEach(b => b.addEventListener("click", () => {
        const l = ticket[b.dataset.dec];
        l.quantity > 1 ? l.quantity-- : ticket.splice(b.dataset.dec, 1);
        renderTicket();
    }));
    el("ticket-lines").querySelectorAll("[data-del]").forEach(b => b.addEventListener("click", () => { ticket.splice(b.dataset.del, 1); renderTicket(); }));
    el("ticket-total").textContent = money(ticketTotal());
    el("checkout-btn").disabled = ticket.length === 0;
    updateChange();
}

document.querySelectorAll("#payment-methods .seg").forEach(button => {
    button.addEventListener("click", () => {
        document.querySelectorAll("#payment-methods .seg").forEach(b => b.classList.remove("active"));
        button.classList.add("active");
        paymentMethod = button.dataset.method;
        const cash = paymentMethod === "EFECTIVO";
        el("cash-field").classList.toggle("hidden", !cash);
        el("change-box").classList.toggle("hidden", !cash);
    });
});

function updateChange() {
    const received = parseFloat(el("cash-received").value);
    const total = ticketTotal();
    const box = el("change-box");
    if (Number.isNaN(received)) {
        el("change-amount").textContent = money(0);
        box.classList.remove("short");
        return;
    }
    const change = received - total;
    box.classList.toggle("short", change < 0);
    el("change-amount").textContent = change < 0 ? `faltan ${money(-change)}` : money(change);
}

el("cash-received").addEventListener("input", updateChange);

el("tax-id").addEventListener("input", () => {
    const isCf = ["", "CF", "C/F"].includes(el("tax-id").value.trim().toUpperCase());
    el("customer-name").placeholder = isCf ? "Consumidor Final" : "Nombre para la factura (obligatorio con NIT)";
    el("customer-name").required = !isCf;
});

function resetSale() {
    ticket = [];
    webCart = null;
    el("tax-id").value = "CF";
    el("customer-name").value = "";
    el("customer-name").required = false;
    el("customer-name").placeholder = "Consumidor Final";
    el("cash-received").value = "";
    el("web-username").value = "";
    el("web-cart-result").innerHTML = "";
    renderTicket();
}

el("clear-ticket-btn").addEventListener("click", () => { resetSale(); el("scan-input").focus(); });

el("checkout-form").addEventListener("submit", async event => {
    event.preventDefault();
    if (!ticket.length) return;
    const body = {
        seller,
        taxId: el("tax-id").value.trim() || "CF",
        customerName: el("customer-name").value.trim() || undefined,
        paymentMethod,
        cashReceived: paymentMethod === "EFECTIVO" && el("cash-received").value !== "" ? Number(el("cash-received").value) : undefined,
        lines: ticket.map(l => ({ articleId: l.articleId, quantity: l.quantity })),
        webCustomer: webCart ? webCart.username : undefined,
        webCartId: webCart && webCart.cartId ? webCart.cartId : undefined,
    };
    el("checkout-btn").disabled = true;
    try {
        const sale = await api("POST", "/api/store/sales", body);
        showReceipt(sale);
        resetSale();
        loadInfo();
    } catch (error) {
        toast(error.message, "error");
        el("checkout-btn").disabled = false;
    }
});

// Factura de la tienda: se genera al cobrar y se puede reimprimir desde Ventas.
// El IVA (12 %) está incluido en los precios; se desglosa en la factura.
const IVA_RATE = 0.12;
let invoiceOnScreen = null;   // { sale, copy }

function invoiceHtml(sale, copyNumber) {
    const issuer = store.invoice || {};
    const total = Number(sale.total);
    const base = Math.round(total / (1 + IVA_RATE) * 100) / 100;
    const iva = Math.round((total - base) * 100) / 100;
    const rows = sale.lines.map(l => `<tr>
        <td>${l.quantity}</td>
        <td>${escapeHtml(l.name)}<div class="muted-line">${escapeHtml(l.sku)} · ${money(l.unitPrice)} c/u</div></td>
        <td class="r">${money(l.lineTotal)}</td>
    </tr>`).join("");
    const cash = sale.paymentMethod === "EFECTIVO"
        ? `<div class="row"><span>Efectivo recibido</span><span>${money(sale.cashReceived)}</span></div><div class="row"><span>Cambio</span><span>${money(sale.changeGiven)}</span></div>`
        : "";
    const status = SALE_STATUS[sale.syncStatus] || ["muted", sale.syncStatus];
    return `<div class="receipt-paper">
        <div class="center">
            <strong>${escapeHtml(issuer.legalName || store.name)}</strong><br>
            ${escapeHtml(store.name)} (${escapeHtml(store.code)})<br>
            <span class="muted-line">${escapeHtml(issuer.address || "")}</span><br>
            <span class="muted-line">NIT emisor: ${escapeHtml(issuer.taxId || "—")}</span>
            <h4 id="invoice-title">FACTURA</h4>
            <span class="muted-line">Serie ${escapeHtml(store.code)} · No. ${escapeHtml(sale.folio)}</span>
        </div>
        ${copyNumber ? `<div class="copy-mark">REIMPRESIÓN ${copyNumber} — COPIA</div>` : ""}
        <hr>
        <div class="row"><span>Fecha</span><span>${formatDate(sale.createdAt)}</span></div>
        <div class="row"><span>NIT</span><span>${escapeHtml(sale.taxId)}</span></div>
        <div class="row"><span>Nombre</span><span>${escapeHtml(sale.customerName)}</span></div>
        <div class="row"><span>Vendedor</span><span>${escapeHtml(sale.seller)}</span></div>
        <hr>
        <table><thead><tr><th>Cant.</th><th>Descripción</th><th class="r">Importe</th></tr></thead><tbody>${rows}</tbody></table>
        <hr>
        <div class="row"><span>Subtotal sin IVA</span><span>${money(base)}</span></div>
        <div class="row"><span>IVA (12 %)</span><span>${money(iva)}</span></div>
        <div class="row"><strong>Total</strong><strong>${money(total)}</strong></div>
        <div class="row"><span>Forma de pago</span><span>${PAYMENT_LABEL[sale.paymentMethod]}</span></div>
        ${cash}
        <hr>
        <div class="center">${badge(status[0], `Envío al central: ${status[1].toLowerCase()}`)} ${sale.centralFolio ? `<span class="mono">${escapeHtml(sale.centralFolio)}</span>` : ""}</div>
        <p class="center muted-line">Documento emitido por el sistema de la tienda. Certificación FEL ante SAT no integrada en esta versión.</p>
    </div>`;
}

/** {@code copy}: viene de Ventas; imprimir cuenta como reimpresión. */
function showReceipt(sale, copy = false) {
    invoiceOnScreen = { sale, copy };
    el("receipt-body").innerHTML = invoiceHtml(sale, copy && sale.reprintCount ? sale.reprintCount : 0);
    el("print-receipt-btn").textContent = copy ? "Reimprimir factura" : "Imprimir factura";
    el("close-receipt-btn").textContent = copy ? "Cerrar" : "Nueva venta";
    el("receipt-modal").classList.remove("hidden");
    el("print-receipt-btn").focus();
}

el("close-receipt-btn").addEventListener("click", () => {
    el("receipt-modal").classList.add("hidden");
    const wasCopy = invoiceOnScreen?.copy;
    invoiceOnScreen = null;
    if (wasCopy) loadSales(); else el("scan-input").focus();
});

el("print-receipt-btn").addEventListener("click", async () => {
    if (!invoiceOnScreen) return;
    if (invoiceOnScreen.copy) {
        try {
            const sale = await api("POST", `/api/store/sales/${invoiceOnScreen.sale.id}/reprint`);
            invoiceOnScreen.sale = sale;
            el("receipt-body").innerHTML = invoiceHtml(sale, sale.reprintCount);
        } catch (error) {
            toast(error.message, "error");
            return;
        }
    }
    window.print();
});

// Cliente web opcional: trae su carrito y lo vuelca al ticket (a precio de tienda).
el("web-cart-form").addEventListener("submit", async event => {
    event.preventDefault();
    const username = el("web-username").value.trim();
    if (!username) return;
    try {
        const cart = await api("GET", `/api/store/web-cart?username=${encodeURIComponent(username)}`);
        webCart = { username: cart.username, fullName: cart.fullName, cartId: cart.cartId };
        let skipped = 0;
        cart.lines.forEach(l => {
            const quantity = Math.min(l.quantity, l.onHand);
            if (quantity < l.quantity) skipped++;
            if (quantity > 0) addToTicket({ articleId: l.articleId, sku: l.sku, name: l.name, price: l.price, quantity: l.onHand }, quantity);
        });
        if (el("customer-name").value.trim() === "") el("customer-name").value = el("tax-id").value.trim().toUpperCase() === "CF" ? "" : cart.fullName;
        const notes = [...cart.unavailable];
        if (skipped) notes.push(`${skipped} línea(s) con menos existencia de la pedida`);
        el("web-cart-result").innerHTML = `<div class="customer-box">
            <strong>${escapeHtml(cart.fullName)}</strong>
            <span class="muted small">${escapeHtml(cart.username)} · ${cart.cartId ? `${cart.lines.length} línea(s) traídas del carrito web; al cobrar, ese carrito se cierra.` : "no tiene carrito web activo."}</span>
            ${notes.length ? `<span class="small" style="color:var(--warn)">No se agregó: ${escapeHtml(notes.join("; "))}</span>` : ""}
        </div>`;
    } catch (error) {
        webCart = null;
        el("web-cart-result").innerHTML = "";
        toast(error.message, "error");
    }
});

// ---------------------------------------------------------------------
// Inventario
// ---------------------------------------------------------------------

async function loadInventory() {
    try {
        stockCache = await api("GET", "/api/store/stock");
        renderStock();
    } catch (error) {
        el("stock-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

// ---------------------------------------------------------------------
// Recepción por folio
// ---------------------------------------------------------------------

function originLabel(origin) { return !origin || origin === "CENTRAL" ? "Almacén central" : origin; }

async function loadReceiving() {
    try {
        const [inTransit, receipts] = await Promise.all([api("GET", "/api/store/receiving"), api("GET", "/api/store/receipts")]);
        updateInTransitCount(inTransit.length);
        el("in-transit-table").innerHTML = table(["Folio", "Desde", "Contenido", "Enviado"],
            inTransit.map(s => `<tr>
                <td><button class="link-btn mono" data-open-folio="${escapeHtml(s.folio)}">${escapeHtml(s.folio)}</button></td>
                <td>${escapeHtml(originLabel(s.origin))}</td>
                <td>${escapeHtml(s.lines.map(l => `${l.quantity} × ${l.name}`).join(", "))}</td>
                <td>${formatDate(s.sentAt)}</td>
            </tr>`), "No hay mercadería en camino a esta tienda.");
        el("in-transit-table").querySelectorAll("[data-open-folio]").forEach(b => b.addEventListener("click", () => {
            el("receiving-folio").value = b.dataset.openFolio;
            lookupFolio();
        }));
        el("receipts-table").innerHTML = table(["Folio de envío", "Desde", "Artículo", "Cantidad", "Estado", "Enviado", "Recibido"],
            receipts.map(r => `<tr>
                <td class="mono">${escapeHtml(r.shipmentFolio)}<span class="cell-sub mono">${escapeHtml(r.folio)}</span></td>
                <td>${escapeHtml(originLabel(r.origin))}</td>
                <td><span class="cell-primary">${escapeHtml(r.name)}</span><span class="cell-sub mono">${escapeHtml(r.sku)}</span></td>
                <td class="num pill-qty">+${r.quantity}</td>
                <td>${r.status === "RECIBIDO" ? badge("ok", "Recibido") : badge("warn", "En tránsito")}</td>
                <td>${formatDate(r.occurredAt)}</td>
                <td>${r.status === "RECIBIDO" ? `${formatDate(r.appliedAt)}<span class="cell-sub">${escapeHtml(r.receivedBy || "")}</span>` : "—"}</td>
            </tr>`), "El almacén central todavía no ha enviado nada a esta tienda.");
    } catch (error) {
        el("in-transit-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

function updateInTransitCount(count) {
    el("in-transit-count").textContent = count;
    el("in-transit-count").classList.toggle("hidden", !count);
}

let lastLookedUpFolio = null;

async function lookupFolio() {
    const folio = el("receiving-folio").value.trim().toUpperCase();
    if (!folio) return;
    lastLookedUpFolio = folio;
    el("receiving-detail").innerHTML = `<p class="hint">Buscando ${escapeHtml(folio)}…</p>`;
    try {
        renderShipment(await api("GET", `/api/store/receiving/${encodeURIComponent(folio)}`));
        loadReceiving();
    } catch (error) {
        el("receiving-detail").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

function renderShipment(shipment) {
    if (shipment.status === "NO_ENCONTRADO") {
        el("receiving-detail").innerHTML = `<p class="empty-state">El folio ${escapeHtml(shipment.folio)} no trae mercadería para esta tienda. Revisa el número, o si el central acaba de emitirlo, espera unos segundos y vuelve a buscar.</p>`;
        return;
    }
    const pending = shipment.status === "EN_TRANSITO";
    const rows = shipment.lines.map(l => `<tr>
        <td><span class="cell-primary">${escapeHtml(l.name)}</span><span class="cell-sub mono">${escapeHtml(l.sku)}</span></td>
        <td class="num">${money(l.price)}</td>
        <td class="num pill-qty">${l.quantity}</td>
        <td>${l.status === "RECIBIDO" ? badge("ok", "Recibido") : badge("warn", "En tránsito")}</td>
    </tr>`);
    el("receiving-detail").innerHTML = `
        <div class="receiving-head">
            <strong class="mono">${escapeHtml(shipment.folio)}</strong>
            <span>Desde: ${escapeHtml(originLabel(shipment.origin))}</span>
            <span>Enviado: ${formatDate(shipment.sentAt)}</span>
            <span>${shipment.units} unidad(es)</span>
        </div>
        <div class="table-wrap">${table(["Artículo", "Precio", "Cantidad", "Estado"], rows, "")}</div>
        ${pending
            ? `<div class="receiving-actions"><button class="btn btn-primary" id="confirm-receiving-btn">Confirmar que llegó (${shipment.units} unidades)</button></div>`
            : `<p class="hint" style="margin-top:.9rem">Ya recibido por ${escapeHtml(shipment.receivedBy || "—")} el ${formatDate(shipment.receivedAt)}.</p>`}`;
    if (pending) {
        el("confirm-receiving-btn").addEventListener("click", async () => {
            el("confirm-receiving-btn").disabled = true;
            try {
                const result = await api("POST", `/api/store/receiving/${encodeURIComponent(shipment.folio)}/confirm`, { seller });
                toast(`${result.folio} recibido: +${result.units} unidad(es) en existencia.`, "ok");
                renderShipment(result);
                el("receiving-folio").value = "";
                loadReceiving();
                loadInfo();
            } catch (error) {
                toast(error.message, "error");
                el("confirm-receiving-btn").disabled = false;
            }
        });
    }
}

el("receiving-form").addEventListener("submit", event => { event.preventDefault(); lookupFolio(); });
// Tab (o salir del campo) carga el folio, como pediste.
el("receiving-folio").addEventListener("change", () => {
    if (el("receiving-folio").value.trim().toUpperCase() !== lastLookedUpFolio) lookupFolio();
});

function renderStock() {
    const filter = el("stock-filter").value.trim().toLowerCase();
    const rows = stockCache
        .filter(s => !filter || s.sku.toLowerCase().includes(filter) || s.name.toLowerCase().includes(filter))
        .map(s => `<tr>
            <td class="mono">${escapeHtml(s.sku)}</td>
            <td><span class="cell-primary">${escapeHtml(s.name)}</span></td>
            <td class="num">${money(s.price)}</td>
            <td>${badge(s.quantity > 5 ? "ok" : s.quantity > 0 ? "warn" : "error", `${s.quantity} unidad(es)`)}</td>
            <td>${s.inTransit ? badge("warn", `+${s.inTransit} por recibir`) : "—"}</td>
            <td>${formatDate(s.updatedAt)}</td>
        </tr>`);
    el("stock-table").innerHTML = table(["Código", "Artículo", "Precio", "Existencia", "En tránsito", "Último movimiento"], rows,
        filter ? "Ningún artículo coincide." : "La tienda no tiene artículos todavía: recibe un folio del central en la pestaña Recepción.");
}

el("stock-filter").addEventListener("input", renderStock);

// ---------------------------------------------------------------------
// Traslados
// ---------------------------------------------------------------------

async function loadTransfers() {
    try {
        const [stock, transfers] = await Promise.all([api("GET", "/api/store/stock"), api("GET", "/api/store/transfers")]);
        stockCache = stock;
        const available = stock.filter(s => s.quantity > 0);
        el("transfer-article").innerHTML = available.length
            ? available.map(s => `<option value="${s.articleId}">${escapeHtml(s.sku)} · ${escapeHtml(s.name)} (${s.quantity} en tienda)</option>`).join("")
            : `<option value="">Sin existencia para trasladar</option>`;
        el("transfers-table").innerHTML = table(["Folio", "Artículo", "Cantidad", "Destino", "Estado", "Central", "Fecha"],
            transfers.map(t => {
                const status = TRANSFER_STATUS[t.syncStatus];
                return `<tr>
                <td class="mono">${escapeHtml(t.folio)}<span class="cell-sub">${escapeHtml(t.seller)}</span></td>
                <td><span class="cell-primary">${escapeHtml(t.articleName)}</span><span class="cell-sub mono">${escapeHtml(t.sku)}</span></td>
                <td class="num pill-qty">−${t.quantity}</td>
                <td>${escapeHtml(t.destination === "CENTRAL" ? "Almacén central" : t.destination)}<span class="cell-sub">${escapeHtml(t.reason || "")}</span></td>
                <td>${badge(status[0], status[1])}${t.lastSyncError ? `<span class="cell-sub">${escapeHtml(t.lastSyncError)}</span>` : ""}</td>
                <td class="mono">${escapeHtml(t.centralFolio || "—")}<span class="cell-sub">${escapeHtml(t.destinationFolio || "")}</span></td>
                <td>${formatDate(t.createdAt)}</td>
            </tr>`;
            }), "Esta tienda no ha emitido traslados.");
    } catch (error) {
        el("transfers-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
    try {
        const destinations = await api("GET", "/api/store/destinations");
        const current = el("transfer-destination").value;
        el("transfer-destination").innerHTML = `<option value="CENTRAL">Almacén central</option>`
            + destinations.map(d => `<option value="${escapeHtml(d.code)}">${escapeHtml(d.code)} · ${escapeHtml(d.name)}</option>`).join("");
        el("transfer-destination").value = current || "CENTRAL";
    } catch {
        // Sin central solo se puede elegir el central; el folio queda pendiente igual.
    }
}

el("transfer-form").addEventListener("submit", async event => {
    event.preventDefault();
    // Se guarda el formulario antes del await: después, event.currentTarget ya es null.
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form).entries());
    if (!values.articleId) { toast("No hay artículo para trasladar.", "error"); return; }
    try {
        const transfer = await api("POST", "/api/store/transfers", {
            articleId: values.articleId,
            quantity: Number(values.quantity),
            destination: values.destination,
            reason: values.reason || undefined,
            seller,
        });
        const status = TRANSFER_STATUS[transfer.syncStatus];
        toast(`Traslado ${transfer.folio}: ${status[1].toLowerCase()}${transfer.centralFolio ? ` (${transfer.centralFolio})` : ""}.`,
            transfer.syncStatus === "RECHAZADO" ? "error" : "ok");
        form.reset();
        loadTransfers();
        loadInfo();
    } catch (error) {
        toast(error.message, "error");
    }
});

// ---------------------------------------------------------------------
// Ventas y bitácora
// ---------------------------------------------------------------------

function localDate(date = new Date()) {
    const pad = n => String(n).padStart(2, "0");
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

el("sales-from").value = el("sales-to").value = localDate();
el("sales-filter").addEventListener("submit", event => { event.preventDefault(); loadSales(); });
el("sales-today").addEventListener("click", () => {
    el("sales-from").value = el("sales-to").value = localDate();
    el("sales-q").value = "";
    loadSales();
});

async function loadSales() {
    const params = new URLSearchParams();
    if (el("sales-from").value) params.set("from", el("sales-from").value);
    if (el("sales-to").value) params.set("to", el("sales-to").value);
    if (el("sales-q").value.trim()) params.set("q", el("sales-q").value.trim());
    try {
        const sales = await api("GET", `/api/store/sales?${params}`);
        const total = sales.reduce((sum, s) => sum + Number(s.total), 0);
        const byMethod = {};
        sales.forEach(s => { byMethod[s.paymentMethod] = (byMethod[s.paymentMethod] || 0) + Number(s.total); });
        el("sales-summary").innerHTML = `<span><strong>${sales.length}</strong> factura(s)</span><span>Total <strong>${money(total)}</strong></span>`
            + Object.entries(byMethod).map(([m, v]) => `<span>${PAYMENT_LABEL[m]} <strong>${money(v)}</strong></span>`).join("");
        el("sales-table").innerHTML = table(["Factura", "Fecha", "Cliente", "Detalle", "Pago", "Total", "Envío al central", ""],
            sales.map(s => {
                const status = SALE_STATUS[s.syncStatus];
                return `<tr>
                <td class="mono">${escapeHtml(s.folio)}<span class="cell-sub">${escapeHtml(s.seller)}</span></td>
                <td>${formatDate(s.createdAt)}</td>
                <td><span class="cell-primary">${escapeHtml(s.customerName)}</span><span class="cell-sub mono">${escapeHtml(s.taxId)}${s.webCustomer ? ` · web: ${escapeHtml(s.webCustomer)}` : ""}</span></td>
                <td>${escapeHtml(s.lines.map(l => `${l.quantity} × ${l.name}`).join(", "))}</td>
                <td>${escapeHtml(PAYMENT_LABEL[s.paymentMethod])}${s.paymentMethod === "EFECTIVO" ? `<span class="cell-sub">cambio ${money(s.changeGiven)}</span>` : ""}</td>
                <td class="num">${money(s.total)}</td>
                <td>${badge(status[0], status[1])} <span class="mono">${escapeHtml(s.centralFolio || "")}</span>
                    ${s.lastSyncError ? `<span class="cell-sub">${escapeHtml(s.lastSyncError)}</span>` : ""}
                    ${s.syncStatus === "ERROR" ? `<button class="btn btn-ghost btn-sm" data-retry="${s.id}">Reintentar</button>` : ""}</td>
                <td class="row-actions"><button class="btn btn-ghost btn-sm" data-invoice="${s.id}">Ver / reimprimir</button>
                    ${s.reprintCount ? `<span class="cell-sub">${s.reprintCount} reimpresión(es)</span>` : ""}</td>
            </tr>`;
            }), "No hay facturas en esas fechas.");
        el("sales-table").querySelectorAll("[data-invoice]").forEach(b => b.addEventListener("click", async () => {
            try {
                showReceipt(await api("GET", `/api/store/sales/${b.dataset.invoice}`), true);
            } catch (error) {
                toast(error.message, "error");
            }
        }));
        el("sales-table").querySelectorAll("[data-retry]").forEach(b => b.addEventListener("click", async () => {
            try {
                const sale = await api("POST", `/api/store/sales/${b.dataset.retry}/retry`);
                toast(`${sale.folio}: ${SALE_STATUS[sale.syncStatus][1].toLowerCase()}.`, sale.syncStatus === "ENVIADA" ? "ok" : "error");
            } catch (error) {
                toast(error.message, "error");
            }
            loadSales();
            loadInfo();
        }));
    } catch (error) {
        el("sales-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

const WEB_STATUS = {
    CONFIRMED: ["ok", "Confirmada"], PREPARING: ["warn", "En preparación"], IN_TRANSIT: ["warn", "En tránsito"],
    READY_FOR_PICKUP: ["ok", "Llegó, lista para entregar"], DELIVERED: ["ok", "Entregada"], CANCELLED: ["error", "Cancelada"],
};

async function loadWebOrders() {
    try {
        const orders = await api("GET", "/api/store/pos-events");
        const pickupOnly = el("weborders-pickup-only").checked;
        const pending = orders.filter(o => o.pickupHere && o.status === "READY_FOR_PICKUP").length;
        el("pickup-count").textContent = pending;
        el("pickup-count").classList.toggle("hidden", !pending);
        el("weborders-table").innerHTML = table(["Pedido", "Estado", "Detalle", "Total", "Entrega", "Actualizado", ""],
            orders.filter(o => !pickupOnly || o.pickupHere).map(o => {
                const status = WEB_STATUS[o.status] || ["muted", o.status];
                return `<tr>
                    <td class="mono">${escapeHtml(o.orderId.slice(0, 8).toUpperCase())}</td>
                    <td>${badge(status[0], status[1])}</td>
                    <td>${escapeHtml(o.items.join(", "))}</td>
                    <td class="num">${o.total ? money(o.total) : "—"}</td>
                    <td>${o.pickupHere ? badge("warn", "Recoger aquí") : `<span class="cell-sub">${escapeHtml(o.destination || "—")}</span>`}${o.trackingNumber ? `<span class="cell-sub mono">Guía ${escapeHtml(o.trackingNumber)}</span>` : ""}</td>
                    <td>${formatDate(o.lastUpdate)}</td>
                    <td>${o.pickupHere && o.status === "READY_FOR_PICKUP" ? `<button class="btn btn-primary btn-sm" data-handoff="${escapeHtml(o.orderId)}">Entregar al cliente</button>` : ""}</td>
                </tr>`;
            }), pickupOnly ? "No hay compras web para recoger en esta tienda." : "SGO-Logic todavía no ha enviado eventos de compras web.");
    } catch (error) {
        el("weborders-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

el("weborders-pickup-only").addEventListener("change", loadWebOrders);

// El cliente llega por su compra web: se identifica y la tienda confirma la entrega a SGO-Logic.
el("weborders-table").addEventListener("click", async event => {
    const button = event.target.closest("[data-handoff]");
    if (!button) return;
    const receivedBy = prompt("Nombre y documento de quien recoge el pedido:");
    if (!receivedBy || !receivedBy.trim()) return;
    button.disabled = true;
    try {
        await api("POST", `/api/store/pos-events/${button.dataset.handoff}/handoff`, { seller, receivedBy: receivedBy.trim() });
        toast("Pedido entregado. La web y el cliente ya lo ven como entregado.", "ok");
        setTimeout(loadWebOrders, 2500);
    } catch (error) { toast(error.message, "error"); button.disabled = false; }
});

const KIND_LABEL = { FACTURA: "Factura", TRASLADO: "Traslado", RECEPCION: "Recepción", CARRITO_WEB: "Carrito web",
    EVENTO_STOCK: "Evento de stock", ENTREGA_PEDIDO: "Entrega de pedido web" };

async function loadLog() {
    try {
        const entries = await api("GET", "/api/store/sync-log");
        const filter = el("log-filter").value;
        const rows = entries
            .filter(e => !filter || (filter === "ERROR" ? e.result === "ERROR" : e.direction === filter))
            .map(e => `<tr>
                <td>${formatDate(e.occurredAt)}</td>
                <td>${e.kind === "ENTREGA_PEDIDO" ? "↑ Tienda → tienda web" : e.direction === "SUBIDA" ? "↑ Tienda → central" : "↓ Central → tienda"}</td>
                <td>${escapeHtml(KIND_LABEL[e.kind] || e.kind)}</td>
                <td class="mono">${escapeHtml(e.reference || "—")}</td>
                <td>${badge(e.result === "OK" ? "ok" : "error", e.result)}</td>
                <td>${escapeHtml(e.detail)}</td>
            </tr>`);
        el("log-table").innerHTML = table(["Fecha", "Dirección", "Tipo", "Referencia", "Resultado", "Detalle"], rows,
            "Sin movimientos de sincronización todavía.");
    } catch (error) {
        el("log-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

el("log-filter").addEventListener("change", loadLog);

// ---------------------------------------------------------------------

showSeller();
renderTicket();
loadInfo();
setInterval(() => {
    loadInfo();
    const tab = activeTab();
    if (tab !== "sale" && tab !== "transfers" && el("receipt-modal").classList.contains("hidden")) refreshTab(tab);
}, 10000);
