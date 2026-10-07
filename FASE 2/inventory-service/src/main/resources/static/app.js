"use strict";

const SESSION_KEY = "almacen.session";

// Declarados aquí arriba (no junto a cada sección) para que estén
// inicializados antes de que corra cualquier código — bindMainPage()
// dispara las cargas iniciales de forma síncrona más abajo en el mismo
// script, así que declarar estas variables `let` más adelante en el
// archivo causaba un "can't access lexical declaration before
// initialization" si la carga se resolvía antes de llegar a esa línea.
let currentArticles = [];
let currentStock = [];
let currentPointsOfSale = [];
let currentMovements = [];

function el(id) {
    return document.getElementById(id);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
}

function formatDate(iso) {
    if (!iso) return "";
    try {
        return new Date(iso).toLocaleString("es-GT", { dateStyle: "short", timeStyle: "short" });
    } catch {
        return iso;
    }
}

function money(value) {
    return "Q" + Number(value || 0).toLocaleString("es-GT", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function initials(text) {
    const parts = String(text || "").split(/[@.\s]/).filter(Boolean);
    return ((parts[0]?.[0] || "") + (parts[1]?.[0] || "")).toUpperCase() || "··";
}

function toast(message, type) {
    const stack = el("toast-stack");
    const node = document.createElement("div");
    node.className = `toast ${type || ""}`.trim();
    node.textContent = message;
    stack.appendChild(node);
    setTimeout(() => node.remove(), 4500);
}

function badge(tone, label) {
    return `<span class="badge badge-${tone}">${escapeHtml(label)}</span>`;
}

function table(headers, rows, emptyMessage) {
    if (!rows.length) return `<p class="empty-state">${escapeHtml(emptyMessage)}</p>`;
    return `<table><thead><tr>${headers.map(h => `<th>${h}</th>`).join("")}</tr></thead><tbody>${rows.join("")}</tbody></table>`;
}

// ---------------------------------------------------------------------
// Sesión (JWT por rol: ALMACEN, FINANZAS, LOGISTICA)
// ---------------------------------------------------------------------

function getSession() {
    try { return JSON.parse(sessionStorage.getItem(SESSION_KEY)); } catch { return null; }
}

function saveSession(session) {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

function clearSession() {
    sessionStorage.removeItem(SESSION_KEY);
}

function hasRole(role) {
    const session = getSession();
    return !!session && Array.isArray(session.roles) && session.roles.includes(role);
}

async function apiCall(method, path, body) {
    const session = getSession();
    const headers = {};
    if (session?.accessToken) headers.Authorization = "Bearer " + session.accessToken;
    if (body !== undefined) headers["Content-Type"] = "application/json";
    const response = await fetch(path, {
        method, headers, cache: "no-store",
        body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    if (response.status === 401) {
        clearSession();
        location.href = "login.html";
        throw new Error("Sesión expirada.");
    }
    const text = await response.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = { message: text }; }
    if (!response.ok) {
        throw new Error((data && data.message) || `HTTP ${response.status}`);
    }
    return data;
}

// ---------------------------------------------------------------------
// Página de login
// ---------------------------------------------------------------------

const loginForm = el("login-form");
if (loginForm) {
    document.querySelectorAll(".demo-account-btn").forEach(button => {
        button.addEventListener("click", () => {
            el("login-username").value = button.dataset.user;
            el("login-password").value = button.dataset.pass;
        });
    });

    loginForm.addEventListener("submit", async event => {
        event.preventDefault();
        const submitButton = event.currentTarget.querySelector("button[type=submit]");
        const values = Object.fromEntries(new FormData(event.currentTarget).entries());
        submitButton.disabled = true;
        try {
            const response = await fetch("/api/v1/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username: values.username, password: values.password }),
            });
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || "Credenciales inválidas.");
            saveSession({ username: values.username, accessToken: data.accessToken, roles: data.roles });
            location.href = "index.html";
        } catch (error) {
            toast(error.message, "error");
            submitButton.disabled = false;
        }
    });
}

// ---------------------------------------------------------------------
// Página principal: sesión requerida, navegación, roles
// ---------------------------------------------------------------------

const isMainPage = !!el("view-dashboard");

if (isMainPage) {
    const session = getSession();
    if (!session) {
        location.href = "login.html";
    } else {
        el("session-user").textContent = session.username;
        el("session-role").textContent = session.roles.join(", ");
        el("session-avatar").textContent = initials(session.username);
        el("logout-btn").addEventListener("click", () => { clearSession(); location.href = "login.html"; });
        applyRoleVisibility(session.roles);
        bindMainPage();
    }
}

function applyRoleVisibility(roles) {
    const canAlmacen = roles.includes("ALMACEN");
    const canFinanzas = roles.includes("FINANZAS");
    const canLogistica = roles.includes("LOGISTICA");

    const canRegisterMovements = canAlmacen || canLogistica;
    el("article-form").querySelector("button[type=submit]").disabled = !canAlmacen;
    el("articles-role-hint").classList.toggle("hidden", canAlmacen);
    el("movement-form").querySelector("button[type=submit]").disabled = !canRegisterMovements;
    el("movements-role-hint").classList.toggle("hidden", canRegisterMovements);
    el("invoice-form").querySelector("button[type=submit]").disabled = !canFinanzas;
    el("finance-role-hint").classList.toggle("hidden", canFinanzas);
    el("pos-form").querySelector("button[type=submit]").disabled = !canAlmacen;
    el("pos-role-hint").classList.toggle("hidden", canAlmacen);
    window.__canLogistica = canLogistica;
}

function bindMainPage() {
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

    el("article-form").addEventListener("submit", async event => {
        event.preventDefault();
        const form = event.currentTarget;
        const values = Object.fromEntries(new FormData(form).entries());
        try {
            const article = await apiCall("POST", "/api/v1/articles", {
                sku: values.sku,
                name: values.name,
                description: values.description || undefined,
                price: Number(values.price),
            });
            toast(`Artículo ${article.sku} creado con existencia 0. Cárgala en Existencias.`, "ok");
            form.reset();
            await refreshAll();
        } catch (error) {
            toast(error.message, "error");
        }
    });

    el("refresh-stock-btn").addEventListener("click", loadStock);
    el("overview-search").addEventListener("input", renderOverview);
    el("overview-only-stock").addEventListener("change", renderOverview);
    el("refresh-overview-btn").addEventListener("click", loadOverview);

    el("movement-article-select").addEventListener("change", showMovementAvailable);

    // Movimientos solo registra salidas hacia tienda; las entradas se cargan en Existencias.
    el("movement-form").addEventListener("submit", async event => {
        event.preventDefault();
        const form = event.currentTarget;
        const values = Object.fromEntries(new FormData(form).entries());
        const submit = form.querySelector("button[type=submit]");
        submit.disabled = true;
        try {
            const movement = await apiCall("POST", "/api/v1/stock-movements", {
                articleId: values.articleId,
                type: "SALIDA",
                quantity: Number(values.quantity),
                reason: values.reason || undefined,
                pointOfSaleId: values.pointOfSaleId,
            });
            el("movement-result").innerHTML = `<div class="row-actions" style="justify-content:flex-start;margin-top:.9rem">${badge("ok", "Folio " + movement.folio)}<span style="font-size:.78rem;color:var(--text-muted)">hacia ${escapeHtml(movement.pointOfSaleCode)} · quedan ${movement.quantityOnHandAfter} en el central</span></div>`;
            toast(`Salida ${movement.folio} hacia ${movement.pointOfSaleCode}. La tienda la recibe con ese folio.`, "ok");
            form.reset();
            await Promise.all([refreshAll(), loadShipments()]);
            showMovementAvailable();
        } catch (error) {
            el("movement-result").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
            toast(error.message, "error");
        } finally {
            submit.disabled = false;
        }
    });

    el("refresh-invoices-btn").addEventListener("click", loadInvoices);
    el("refresh-store-invoices-btn").addEventListener("click", loadStoreInvoices);
    el("invoice-form").addEventListener("submit", async event => {
        event.preventDefault();
        const form = event.currentTarget;
        const values = Object.fromEntries(new FormData(form).entries());
        try {
            await apiCall("POST", "/api/v1/invoices", {
                type: values.type,
                counterparty: values.counterparty,
                description: values.description || undefined,
                amount: Number(values.amount),
                relatedFolio: values.relatedFolio || undefined,
                dueDate: values.dueDate,
            });
            toast("Cuenta registrada.", "ok");
            form.reset();
            await Promise.all([loadInvoices(), loadDashboard()]);
        } catch (error) {
            toast(error.message, "error");
        }
    });

    el("pos-form").addEventListener("submit", async event => {
        event.preventDefault();
        const form = event.currentTarget;
        const values = Object.fromEntries(new FormData(form).entries());
        try {
            await apiCall("POST", "/api/v1/points-of-sale", {
                code: values.code,
                name: values.name,
            });
            toast("Punto de venta creado con su propio almacén.", "ok");
            form.reset();
            await loadPointsOfSale();
        } catch (error) {
            toast(error.message, "error");
        }
    });

    el("refresh-shipments-btn").addEventListener("click", loadShipments);

    el("refresh-pos-stock-btn").addEventListener("click", () => loadPosStock());
    el("pos-stock-filter").addEventListener("change", event => loadPosStock(event.target.value || undefined));
    el("pos-events-status").addEventListener("change", renderPosEvents);
    el("pos-events-table").addEventListener("click", async event => {
        const button = event.target.closest("[data-retry-pos-event]");
        if (!button) return;
        button.disabled = true;
        try {
            await apiCall("POST", `/api/v1/pos-stock-events/${button.dataset.retryPosEvent}/retry`);
            toast("Evento reenviado al punto de venta.", "ok");
            setTimeout(loadPosEvents, 4000);
        } catch (error) { toast(error.message, "error"); button.disabled = false; }
    });

    el("refresh-audit-btn").addEventListener("click", () => loadAuditLog());
    el("audit-folio-filter").addEventListener("input", () => {
        clearTimeout(window.__auditFilterTimer);
        window.__auditFilterTimer = setTimeout(() => loadAuditLog(el("audit-folio-filter").value.trim() || undefined), 300);
    });

    loadPointsOfSale();
    loadDashboard();
    loadStock().then(loadArticles);
    loadMovements();
    loadInvoices();
    loadStoreInvoices();
    loadShipments();
    loadPosStock();
    loadAuditLog();
    setInterval(() => { const tab = activeTab(); if (tab) refreshTab(tab); }, 15000);
}

function showMovementAvailable() {
    const articleId = el("movement-article-select").value;
    const stock = currentStock.find(s => s.articleId === articleId);
    el("movement-available").textContent = articleId
        ? `Disponible en el almacén central: ${stock ? stock.quantityOnHand : 0} unidad(es).`
        : "";
}

// ---------------------------------------------------------------------
// Artículos
// ---------------------------------------------------------------------

async function loadArticles() {
    try {
        currentArticles = await apiCall("GET", "/api/v1/articles");
        const onHand = Object.fromEntries((currentStock || []).map(s => [s.articleId, s.quantityOnHand]));
        const rows = currentArticles.map(a => `<tr>
            <td><span class="cell-primary">${escapeHtml(a.name)}</span><span class="cell-sub">${escapeHtml(a.description || "Sin descripción")}</span></td>
            <td class="mono">${escapeHtml(a.sku)}</td>
            <td class="num">${money(a.price)}</td>
            <td class="num">${onHand[a.id] ?? "—"}</td>
            <td>${formatDate(a.createdAt)}</td>
        </tr>`);
        el("articles-table").innerHTML = table(["Artículo", "SKU", "Precio", "Existencia", "Creado"], rows, "Todavía no hay artículos.");
        populateArticleSelect();
    } catch (error) {
        el("articles-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

function populateArticleSelect() {
    const select = el("movement-article-select");
    const chosen = select.value;
    select.innerHTML = currentArticles.length
        ? currentArticles.map(article => `<option value="${escapeHtml(article.id)}">${escapeHtml(article.name)} (${escapeHtml(article.sku)})</option>`).join("")
        : `<option value="">No hay artículos — crea uno primero</option>`;
    if (chosen && currentArticles.some(article => article.id === chosen)) select.value = chosen;
    showMovementAvailable();
}

// ---------------------------------------------------------------------
// Existencias
// ---------------------------------------------------------------------

async function loadStock() {
    try {
        currentStock = await apiCall("GET", "/api/v1/stock");
        const rows = currentStock.map(s => `<tr>
            <td><span class="cell-primary">${escapeHtml(s.name)}</span><span class="cell-sub mono">${escapeHtml(s.sku)}</span></td>
            <td>${badge(s.quantityOnHand > 0 ? "ok" : "error", `${s.quantityOnHand} unidad(es)`)}</td>
            <td><span class="cell-primary">${escapeHtml(s.warehouseCode)}</span><span class="cell-sub">${escapeHtml(s.warehouseName)}</span></td>
            <td>${formatDate(s.updatedAt)}</td>
            <td class="row-actions">${addStockButton(s.articleId)}</td>
        </tr>`);
        el("stock-table").innerHTML = table(["Artículo", "Existencia", "Almacén", "Última actualización", ""], rows, "Todavía no hay existencias.");
        bindAddStock("stock-table");
    } catch (error) {
        el("stock-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

// ---------------------------------------------------------------------
// Puntos de venta (cada uno con su propio almacén, creado junto con él)
// ---------------------------------------------------------------------

async function loadPointsOfSale() {
    try {
        currentPointsOfSale = await apiCall("GET", "/api/v1/points-of-sale");
        const rows = currentPointsOfSale.map(pos => `<tr>
            <td class="mono">${escapeHtml(pos.code)}</td>
            <td class="cell-primary">${escapeHtml(pos.name)}</td>
            <td><span class="cell-primary">${escapeHtml(pos.warehouseCode)}</span><span class="cell-sub">${escapeHtml(pos.warehouseName)}</span></td>
        </tr>`);
        el("pos-table").innerHTML = table(["Código", "Punto de venta", "Su almacén"], rows, "Todavía no hay puntos de venta.");
        const posSelect = el("movement-pos-select");
        const chosenPos = posSelect.value;
        // Sin tienda preseleccionada: la salida exige elegir a quién va.
        posSelect.innerHTML = currentPointsOfSale.length
            ? `<option value="">Elige la tienda destino…</option>` + currentPointsOfSale.map(pos => `<option value="${escapeHtml(pos.id)}">${escapeHtml(pos.code)} — ${escapeHtml(pos.name)}</option>`).join("")
            : `<option value="">No hay puntos de venta — crea uno en Logística</option>`;
        posSelect.value = chosenPos;
        const filterSelect = el("pos-stock-filter");
        const previousValue = filterSelect.value;
        filterSelect.innerHTML = `<option value="">Todos los puntos de venta</option>` +
            currentPointsOfSale.map(pos => `<option value="${escapeHtml(pos.id)}">${escapeHtml(pos.code)} — ${escapeHtml(pos.name)}</option>`).join("");
        filterSelect.value = previousValue;
    } catch (error) {
        el("pos-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

function pointOfSaleLabel(m) {
    if (!m.pointOfSaleCode) return "";
    const direction = m.type === "ENTRADA" ? "desde" : "hacia";
    return `<span class="cell-sub">${direction} ${escapeHtml(m.pointOfSaleCode)} · ${escapeHtml(m.pointOfSaleName)}</span>`;
}

// ---------------------------------------------------------------------
// Agregar existencia a un artículo ya registrado (entrada con su folio)
// ---------------------------------------------------------------------

function canMoveStock() { return hasRole("ALMACEN") || hasRole("LOGISTICA"); }

function addStockButton(articleId) {
    return `<button class="btn btn-ghost btn-sm" data-add-stock="${escapeHtml(articleId)}" ${canMoveStock() ? "" : "disabled title=\"Solo Almacén o Logística\""}>+ Existencia</button>`
        + `<button class="btn btn-ghost btn-sm" data-adjust-stock="${escapeHtml(articleId)}" ${hasRole("ALMACEN") ? "" : "disabled title=\"Solo Almacén\""}>− Ajuste</button>`;
}

function bindAddStock(tableId) {
    el(tableId).querySelectorAll("[data-add-stock]").forEach(button => {
        button.addEventListener("click", () => openAddStock(button, button.dataset.addStock, "ENTRADA"));
    });
    el(tableId).querySelectorAll("[data-adjust-stock]").forEach(button => {
        button.addEventListener("click", () => openAddStock(button, button.dataset.adjustStock, "AJUSTE"));
    });
}

/** ENTRADA suma existencia; AJUSTE la baja (merma, daño, corrección) con motivo obligatorio. Ambos con folio. */
function openAddStock(button, articleId, type) {
    const row = button.closest("tr");
    const open = row.nextElementSibling?.classList.contains("add-stock-row") ? row.nextElementSibling : null;
    if (open) {
        open.remove();
        if (open.dataset.type === type) return;
    }
    const adjust = type === "AJUSTE";
    const columns = row.children.length;
    const formRow = document.createElement("tr");
    formRow.className = "add-stock-row";
    formRow.dataset.type = type;
    formRow.innerHTML = `<td colspan="${columns}">
        <form class="inline-form">
            <label>${adjust ? "Cantidad a dar de baja" : "Cantidad a agregar"}<input type="number" name="quantity" min="1" value="1" required /></label>
            <label class="grow">Motivo${adjust ? " (obligatorio)" : ""}<input type="text" name="reason" maxlength="255" ${adjust ? "required" : ""}
                placeholder="${adjust ? "Ej. merma, producto dañado, corrección de conteo" : "Ej. recepción de proveedor"}" /></label>
            <button type="submit" class="btn ${adjust ? "btn-danger" : "btn-primary"} btn-sm">${adjust ? "Registrar ajuste" : "Agregar entrada"}</button>
            <button type="button" class="btn btn-ghost btn-sm" data-cancel>Cancelar</button>
        </form></td>`;
    row.after(formRow);
    const form = formRow.querySelector("form");
    form.quantity.focus();
    form.quantity.select();
    formRow.querySelector("[data-cancel]").addEventListener("click", () => formRow.remove());
    form.addEventListener("submit", async event => {
        event.preventDefault();
        form.querySelector("button[type=submit]").disabled = true;
        try {
            const movement = await apiCall("POST", "/api/v1/stock-movements", {
                articleId,
                type,
                quantity: Number(form.quantity.value),
                reason: form.reason.value.trim() || "Ingreso de existencia",
            });
            toast(`${adjust ? "Ajuste" : "Entrada"} ${movement.folio}: ${adjust ? "−" : "+"}${movement.quantity}. Existencia actual: ${movement.quantityOnHandAfter}.`, "ok");
            formRow.remove();
            await refreshAll();
        } catch (error) {
            toast(error.message, "error");
            form.querySelector("button[type=submit]").disabled = false;
        }
    });
}

// ---------------------------------------------------------------------
// Vista general: existencias de toda la red por almacén
// ---------------------------------------------------------------------

const LOCATION_KIND_LABEL = { CENTRAL: "Central", ONLINE: "Tienda online", STORE: "Tienda" };
let overviewData = null;

async function loadOverview() {
    try {
        overviewData = await apiCall("GET", "/api/v1/stock-overview");
        renderOverview();
    } catch (error) {
        el("overview-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

function renderOverview() {
    if (!overviewData) return;
    const text = el("overview-search").value.trim().toLowerCase();
    const onlyStock = el("overview-only-stock").checked;
    const locations = overviewData.locations;
    const articles = overviewData.articles
        .filter(a => !text || a.sku.toLowerCase().includes(text) || a.name.toLowerCase().includes(text))
        .filter(a => !onlyStock || a.total > 0);
    const totals = Object.fromEntries(locations.map(l => [l.code, overviewData.articles.reduce((sum, a) => sum + (a.quantities[l.code] || 0), 0)]));
    el("overview-summary").innerHTML = locations.map(l => `<div class="overview-chip ${l.kind.toLowerCase()}">
        <span>${escapeHtml(LOCATION_KIND_LABEL[l.kind])}</span><strong>${escapeHtml(l.name)}</strong>
        <em>${totals[l.code]} unidad(es)</em><small class="mono">${escapeHtml(l.warehouseCode || "")}</small></div>`).join("");
    const header = ["Artículo", ...locations.map(l => l.code === "CENTRAL" ? "Central" : l.code), "Total red"];
    const rows = articles.map(a => `<tr>
        <td><span class="cell-primary">${escapeHtml(a.name)}</span><span class="cell-sub mono">${escapeHtml(a.sku)} · ${money(a.price)}</span></td>
        ${locations.map(l => {
            const quantity = a.quantities[l.code] || 0;
            return `<td class="num ${quantity ? "" : "zero"}">${quantity || "—"}</td>`;
        }).join("")}
        <td class="num"><strong>${a.total}</strong></td>
    </tr>`);
    el("overview-table").innerHTML = table(header, rows, onlyStock ? "Ningún artículo tiene existencia todavía." : "Todavía no hay artículos.");
}


// ---------------------------------------------------------------------
// Actualización: al cambiar de pestaña y cada 15 s (las tiendas y la web
// mueven existencias sin pasar por esta pantalla)
// ---------------------------------------------------------------------

const TAB_LOADERS = {
    dashboard: () => loadDashboard(),
    overview: () => loadOverview(),
    articles: () => loadStock().then(loadArticles),
    stock: () => loadStock(),
    movements: () => loadMovements(),
    finance: () => Promise.all([loadInvoices(), loadStoreInvoices()]),
    logistics: () => Promise.all([loadPointsOfSale(), loadShipments()]),
    "pos-stock": () => loadPosStock(el("pos-stock-filter").value || undefined),
    audit: () => loadAuditLog(el("audit-folio-filter").value.trim() || undefined),
};

function refreshTab(tab) {
    // No se redibuja una tabla mientras alguien está escribiendo en ella.
    if (document.querySelector(`#view-${tab} .add-stock-row`)) return Promise.resolve();
    return (TAB_LOADERS[tab] || (() => Promise.resolve()))();
}

function refreshAll() {
    return Promise.all([loadStock().then(loadArticles), loadMovements(), loadDashboard()]);
}

function activeTab() {
    return document.querySelector(".nav-btn.active")?.dataset.tab;
}

// ---------------------------------------------------------------------
// Movimientos
// ---------------------------------------------------------------------

function movementKind(m) {
    if (m.type === "SALIDA") return badge("warn", "Salida");
    if (m.type === "AJUSTE") return badge("error", "Ajuste (baja)");
    if ((m.externalReference || "").startsWith("TRA:")) return badge("muted", "Entrada · traslado de tienda");
    return badge("ok", "Entrada");
}

function movementRow(m) {
    return `<tr>
        <td class="mono">${escapeHtml(m.folio)}${(m.externalReference || "").startsWith("TRA:") ? `<span class="cell-sub mono">${escapeHtml(m.externalReference.slice(4))}</span>` : ""}</td>
        <td><span class="cell-primary">${escapeHtml(m.articleName)}</span><span class="cell-sub mono">${escapeHtml(m.articleSku)}</span></td>
        <td>${movementKind(m)}</td>
        <td class="num pill-qty">${m.type === "ENTRADA" ? "+" : "−"}${m.quantity}</td>
        <td><span class="cell-primary">${escapeHtml(m.sourceWarehouseCode)}</span></td>
        <td><span class="cell-primary">${escapeHtml(m.reason || "Sin motivo")}</span>${pointOfSaleLabel(m)}</td>
        <td>${formatDate(m.occurredAt)}</td>
    </tr>`;
}

async function loadMovements() {
    try {
        currentMovements = await apiCall("GET", "/api/v1/stock-movements");
        el("movements-table").innerHTML = table(["Folio", "Artículo", "Tipo", "Cantidad", "Almacén", "Motivo", "Fecha"],
            currentMovements.map(movementRow), "Todavía no hay movimientos.");
    } catch (error) {
        el("movements-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

// ---------------------------------------------------------------------
// Finanzas — cuentas por pagar/cobrar
// ---------------------------------------------------------------------

async function loadInvoices() {
    try {
        const invoices = await apiCall("GET", "/api/v1/invoices");
        const rows = invoices.map(invoice => `<tr>
            <td><span class="cell-primary">${escapeHtml(invoice.counterparty)}</span>${invoice.relatedFolio ? `<span class="cell-sub mono">${escapeHtml(invoice.relatedFolio)}</span>` : ""}</td>
            <td>${badge(invoice.type === "PAYABLE" ? "warn" : "ok", invoice.type === "PAYABLE" ? "Por pagar" : "Por cobrar")}</td>
            <td class="num">${money(invoice.amount)}</td>
            <td>${invoice.dueDate}</td>
            <td>${badge(invoice.status === "PAID" ? "ok" : "muted", invoice.status === "PAID" ? "Pagada" : "Pendiente")}</td>
            <td class="row-actions">${invoice.status === "PENDING" ? `<button class="btn btn-ghost btn-sm" data-pay="${escapeHtml(invoice.id)}" ${hasRole("FINANZAS") ? "" : "disabled"}>Marcar pagada</button>` : "—"}</td>
        </tr>`);
        el("invoices-table").innerHTML = table(["Contraparte", "Tipo", "Monto", "Vence", "Estado", ""], rows, "Todavía no hay cuentas registradas.");
        el("invoices-table").querySelectorAll("[data-pay]").forEach(button => {
            button.addEventListener("click", async () => {
                try {
                    await apiCall("PATCH", `/api/v1/invoices/${button.dataset.pay}/pay`);
                    toast("Cuenta marcada como pagada.", "ok");
                    await Promise.all([loadInvoices(), loadDashboard()]);
                } catch (error) {
                    toast(error.message, "error");
                }
            });
        });
    } catch (error) {
        el("invoices-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

const PAYMENT_LABEL = { EFECTIVO: "Efectivo", TARJETA: "Tarjeta", TRANSFERENCIA: "Transferencia" };

async function loadStoreInvoices() {
    try {
        const invoices = await apiCall("GET", "/api/v1/store-sync/invoices");
        const rows = invoices.map(invoice => `<tr>
            <td><span class="cell-primary mono">${escapeHtml(invoice.centralFolio)}</span><span class="cell-sub mono">${escapeHtml(invoice.ticketFolio)}</span></td>
            <td><span class="cell-primary">${escapeHtml(invoice.pointOfSaleCode)}</span><span class="cell-sub">${escapeHtml(invoice.seller)}</span></td>
            <td><span class="cell-primary">${escapeHtml(invoice.customerName)}</span><span class="cell-sub mono">${escapeHtml(invoice.taxId)}</span></td>
            <td>${escapeHtml(invoice.lines.map(line => `${line.quantity} × ${line.name}`).join(", "))}</td>
            <td>${badge("muted", PAYMENT_LABEL[invoice.paymentMethod] || invoice.paymentMethod)}</td>
            <td class="num">${money(invoice.total)}</td>
            <td>${formatDate(invoice.issuedAt)}<span class="cell-sub">recibida ${formatDate(invoice.receivedAt)}</span></td>
        </tr>`);
        el("store-invoices-table").innerHTML = table(["Folio / ticket", "Tienda / vendedor", "Cliente", "Detalle", "Pago", "Total", "Emitida"],
            rows, "Ninguna tienda ha subido facturas todavía.");
    } catch (error) {
        el("store-invoices-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

// ---------------------------------------------------------------------
// Logística — envíos por folio
// ---------------------------------------------------------------------

const SHIPMENT_NEXT_STATUS = { PENDIENTE: "EN_TRANSITO", EN_TRANSITO: "ENTREGADO" };
const SHIPMENT_LABEL = { PENDIENTE: "Pendiente", EN_TRANSITO: "En tránsito", ENTREGADO: "Entregado" };
const SHIPMENT_STEPS = ["PENDIENTE", "EN_TRANSITO", "ENTREGADO"];

function shipmentProgress(status) {
    const idx = SHIPMENT_STEPS.indexOf(status);
    return `<div class="progress-steps">${SHIPMENT_STEPS.map((_, i) => `<span class="progress-step ${i < idx ? "done" : i === idx ? "current" : ""}"></span>`).join("")}</div>`;
}

function shipmentRow(shipment) {
    const next = SHIPMENT_NEXT_STATUS[shipment.status];
    return `<tr>
        <td class="mono">${escapeHtml(shipment.folio)}</td>
        <td><span class="cell-primary">${escapeHtml(shipment.pointOfSaleCode)} — ${escapeHtml(shipment.pointOfSaleName)}</span><span class="cell-sub">${escapeHtml(shipment.warehouseCode)} · ${escapeHtml(shipment.warehouseName)}</span></td>
        <td>${shipmentProgress(shipment.status)}</td>
        <td>${badge(shipment.status === "ENTREGADO" ? "ok" : "warn", SHIPMENT_LABEL[shipment.status])}</td>
        <td>${formatDate(shipment.updatedAt)}</td>
        <td class="row-actions">${next ? `<button class="btn btn-ghost btn-sm" data-advance="${escapeHtml(shipment.folio)}" data-next="${next}" ${window.__canLogistica ? "" : "disabled"}>${SHIPMENT_LABEL[next]}</button>` : "—"}</td>
    </tr>`;
}

async function loadShipments() {
    try {
        const shipments = await apiCall("GET", "/api/v1/shipments");
        el("shipments-table").innerHTML = table(["Folio", "Destino", "Progreso", "Estado", "Actualizado", ""],
            shipments.map(shipmentRow), "Todavía no hay envíos.");
        el("shipments-table").querySelectorAll("[data-advance]").forEach(button => {
            button.addEventListener("click", async () => {
                try {
                    await apiCall("PATCH", `/api/v1/shipments/${button.dataset.advance}/status`, { status: button.dataset.next });
                    toast("Envío actualizado.", "ok");
                    await Promise.all([loadShipments(), loadDashboard()]);
                } catch (error) {
                    toast(error.message, "error");
                }
            });
        });
    } catch (error) {
        el("shipments-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

// ---------------------------------------------------------------------
// Resumen (dashboard)
// ---------------------------------------------------------------------

async function loadDashboard() {
    try {
        const [articles, stock, movements, invoices, shipments] = await Promise.all([
            apiCall("GET", "/api/v1/articles"),
            apiCall("GET", "/api/v1/stock"),
            apiCall("GET", "/api/v1/stock-movements"),
            apiCall("GET", "/api/v1/invoices"),
            apiCall("GET", "/api/v1/shipments"),
        ]);

        const priceByArticle = new Map(articles.map(a => [a.id, Number(a.price)]));
        const stockValue = stock.reduce((sum, s) => sum + (priceByArticle.get(s.articleId) || 0) * s.quantityOnHand, 0);

        el("kpi-articles").textContent = articles.length;
        el("kpi-stock-value").textContent = money(stockValue);
        el("kpi-invoices-pending").textContent = invoices.filter(i => i.status === "PENDING").length;
        el("kpi-shipments-active").textContent = shipments.filter(s => s.status !== "ENTREGADO").length;

        el("dashboard-movements").innerHTML = table(["Folio", "Artículo", "Tipo", "Cantidad", "Almacén", "Motivo", "Fecha"],
            movements.slice(0, 5).map(movementRow), "Sin movimientos todavía.");
        el("dashboard-shipments").innerHTML = table(["Folio", "Destino", "Progreso", "Estado", "Actualizado", ""],
            shipments.slice(0, 5).map(shipmentRow), "Sin envíos todavía.");
        el("dashboard-shipments").querySelectorAll("[data-advance]").forEach(button => {
            button.addEventListener("click", async () => {
                try {
                    await apiCall("PATCH", `/api/v1/shipments/${button.dataset.advance}/status`, { status: button.dataset.next });
                    toast("Envío actualizado.", "ok");
                    await Promise.all([loadShipments(), loadDashboard()]);
                } catch (error) {
                    toast(error.message, "error");
                }
            });
        });
    } catch (error) {
        toast(error.message, "error");
    }
}

// ---------------------------------------------------------------------
// Stock por tienda — existencia propia de cada punto de venta
// ---------------------------------------------------------------------

async function loadPosStock(pointOfSaleId) {
    try {
        const query = pointOfSaleId ? `?pointOfSaleId=${encodeURIComponent(pointOfSaleId)}` : "";
        const [stock, movements] = await Promise.all([
            apiCall("GET", `/api/v1/pos-stock${query}`),
            apiCall("GET", `/api/v1/pos-stock/movements${query}`),
        ]);
        const stockRows = stock.map(s => `<tr>
            <td><span class="cell-primary">${escapeHtml(s.pointOfSaleCode)}</span><span class="cell-sub">${escapeHtml(s.pointOfSaleName)}</span></td>
            <td><span class="cell-primary">${escapeHtml(s.articleName)}</span><span class="cell-sub mono">${escapeHtml(s.articleSku)}</span></td>
            <td>${badge(s.quantityOnHand > 0 ? "ok" : "error", `${s.quantityOnHand} unidad(es)`)}</td>
            <td>${formatDate(s.updatedAt)}</td>
        </tr>`);
        el("pos-stock-table").innerHTML = table(["Punto de venta", "Artículo", "Existencia", "Última actualización"],
            stockRows, "Todavía no hay existencias registradas en puntos de venta.");

        const POS_MOVEMENT = {
            ENTRADA: { tone: "ok", label: "Entrada", sign: "+" },
            VENTA: { tone: "warn", label: "Venta", sign: "−" },
            TRASLADO: { tone: "muted", label: "Traslado", sign: "−" },
        };
        const movementRows = movements.map(m => {
            const kind = POS_MOVEMENT[m.type] || POS_MOVEMENT.ENTRADA;
            const origin = m.type === "VENTA" ? (m.externalReference || "—") : m.originWarehouseFolio;
            return `<tr>
            <td class="mono">${escapeHtml(m.folio)}</td>
            <td>${badge(kind.tone, kind.label)}</td>
            <td><span class="cell-primary">${escapeHtml(m.pointOfSaleCode)}</span></td>
            <td><span class="cell-primary">${escapeHtml(m.articleName)}</span><span class="cell-sub mono">${escapeHtml(m.articleSku)}</span></td>
            <td class="num pill-qty">${kind.sign}${m.quantity}</td>
            <td class="mono">${escapeHtml(origin)}</td>
            <td>${formatDate(m.occurredAt)}</td>
        </tr>`;
        });
        el("pos-stock-movements-table").innerHTML = table(
            ["Folio tienda", "Tipo", "Punto de venta", "Artículo", "Cantidad", "Origen / referencia", "Fecha"],
            movementRows, "Todavía no hay movimientos en puntos de venta.");

        await loadPosEvents();

        const transfers = await apiCall("GET", "/api/v1/store-sync/transfers");
        const transferRows = transfers.map(t => `<tr>
            <td><span class="cell-primary mono">${escapeHtml(t.centralFolio)}</span><span class="cell-sub mono">${escapeHtml(t.localFolio)}</span></td>
            <td>${escapeHtml(t.originPosCode)} → ${escapeHtml(t.destination === "CENTRAL" ? "Almacén central" : t.destination)}</td>
            <td><span class="cell-primary">${escapeHtml(t.articleName)}</span><span class="cell-sub mono">${escapeHtml(t.sku)}</span></td>
            <td class="num pill-qty">${t.quantity}</td>
            <td class="mono">${escapeHtml(t.destinationFolio)}</td>
            <td>${escapeHtml(t.reason || "—")}<span class="cell-sub">${escapeHtml(t.seller)}</span></td>
            <td>${formatDate(t.occurredAt)}</td>
        </tr>`);
        el("store-transfers-table").innerHTML = table(["Folio / local", "Ruta", "Artículo", "Cantidad", "Folio de entrada", "Motivo / vendedor", "Fecha"],
            transferRows, "Ninguna tienda ha trasladado mercadería todavía.");
    } catch (error) {
        el("pos-stock-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
        el("pos-stock-movements-table").innerHTML = "";
    }
}

// Eventos de stock hacia el sistema de cada punto de venta (POS local)
const POS_EVENT_STATUS = {
    ENTREGADO: ["ok", "Entregado"], PENDIENTE: ["warn", "Pendiente"], REINTENTO: ["warn", "Reintentando"],
    FALLIDO: ["error", "Fallido"], SIN_TERMINAL: ["muted", "Sin sistema conectado"],
};
let posEvents = [];

function renderPosEvents() {
    const filter = el("pos-events-status").value;
    const rows = posEvents.filter(e => !filter || (filter === "PENDIENTES" ? ["PENDIENTE", "REINTENTO"].includes(e.status) : e.status === filter))
        .map(e => {
            const [tone, label] = POS_EVENT_STATUS[e.status] || ["muted", e.status];
            return `<tr>
            <td>${formatDate(e.createdAt)}</td>
            <td><span class="cell-primary">${escapeHtml(e.posCode)}</span></td>
            <td><span class="cell-primary">${escapeHtml(e.movement)} ${e.delta > 0 ? "+" : "−"}${Math.abs(e.delta)}</span><span class="cell-sub mono">${escapeHtml(e.folio)}${e.reference ? " · " + escapeHtml(e.reference) : ""}</span></td>
            <td><span class="cell-primary">${escapeHtml(e.articleName)}</span><span class="cell-sub mono">${escapeHtml(e.sku)}</span></td>
            <td class="num pill-qty">${e.quantityAfter}</td>
            <td>${badge(tone, label)}<span class="cell-sub">${e.attempts} intento(s)${e.lastError ? " · " + escapeHtml(e.lastError) : ""}${e.acknowledgement ? " · " + escapeHtml(e.acknowledgement.slice(0, 20)) + "…" : ""}</span></td>
            <td class="row-actions">${e.status === "FALLIDO" ? `<button class="btn btn-ghost btn-sm" data-retry-pos-event="${escapeHtml(e.id)}" ${canMoveStock() ? "" : "disabled"}>Reenviar</button>` : ""}</td>
        </tr>`;
        });
    el("pos-events-table").innerHTML = table(["Fecha", "Punto de venta", "Movimiento", "Artículo", "Existencia resultante", "Entrega", ""],
        rows, posEvents.length ? "Ningún evento con ese estado." : "Todavía no hay eventos de stock: se generan con cada entrada, venta o traslado de un punto de venta.");
}

async function loadPosEvents() {
    try {
        posEvents = await apiCall("GET", "/api/v1/pos-stock-events");
        renderPosEvents();
    } catch (error) {
        el("pos-events-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}

// ---------------------------------------------------------------------
// Bitácora — trazo de folios, envíos y cuentas
// ---------------------------------------------------------------------

const AUDIT_EVENT_TONE = {
    ENTRADA_REGISTRADA: "ok",
    SALIDA_REGISTRADA: "warn",
    POS_ENTRADA_REGISTRADA: "ok",
    POS_VENTA_REGISTRADA: "warn",
    AJUSTE_REGISTRADO: "error",
    FACTURA_TIENDA_RECIBIDA: "ok",
    TRASLADO_TIENDA_REGISTRADO: "muted",
    DISCREPANCIA_STOCK_TIENDA: "error",
    ENVIO_ACTUALIZADO: "muted",
    CUENTA_REGISTRADA: "muted",
    CUENTA_PAGADA: "ok",
    ARTICULO_CREADO: "muted",
    PUNTO_VENTA_CREADO: "muted",
};

async function loadAuditLog(relatedFolio) {
    try {
        const query = relatedFolio ? `?relatedFolio=${encodeURIComponent(relatedFolio)}` : "";
        const entries = await apiCall("GET", `/api/v1/audit-log${query}`);
        const rows = entries.map(entry => `<tr>
            <td>${badge(AUDIT_EVENT_TONE[entry.eventType] || "muted", entry.eventType.replace(/_/g, " "))}</td>
            <td>${escapeHtml(entry.description)}</td>
            <td class="mono">${escapeHtml(entry.relatedFolio || "—")}</td>
            <td>${escapeHtml(entry.actor)}</td>
            <td>${formatDate(entry.occurredAt)}</td>
        </tr>`);
        el("audit-table").innerHTML = table(["Evento", "Descripción", "Folio", "Actor", "Fecha"],
            rows, "Todavía no hay eventos registrados.");
    } catch (error) {
        el("audit-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
    }
}
