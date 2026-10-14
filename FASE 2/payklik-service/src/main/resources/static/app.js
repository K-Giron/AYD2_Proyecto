// Portal de comercios de PayKlik: transacciones y estado de la API.

function el(id) { return document.getElementById(id); }
function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
function formatDate(iso) { return iso ? new Date(iso).toLocaleString("es-GT", { dateStyle: "short", timeStyle: "medium" }) : "—"; }
function money(value, currency) { return `${currency === "GTQ" ? "Q" : (currency || "") + " "}${Number(value || 0).toLocaleString("es-GT", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`; }
function badge(tone, label) { return `<span class="badge badge-${tone}">${escapeHtml(label)}</span>`; }
function toast(message, type) {
    const node = document.createElement("div");
    node.className = `toast ${type || ""}`;
    node.textContent = message;
    el("toast-stack").appendChild(node);
    setTimeout(() => node.remove(), 4500);
}
async function api(method, path, body) {
    const response = await fetch(path, {
        method, cache: "no-store",
        headers: body === undefined ? {} : { "Content-Type": "application/json" },
        body: body === undefined ? undefined : JSON.stringify(body),
    });
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) throw new Error((data && data.message) || `Error ${response.status}`);
    return data;
}

const MODE = { NORMAL: "Normal", RECHAZAR: "Emisor declina", LENTO: "Lento", FUERA_DE_SERVICIO: "Fuera de servicio" };
function storedUser() { try { return sessionStorage.getItem("pk.user"); } catch { return null; } }
function storeUser(name) { try { name ? sessionStorage.setItem("pk.user", name) : sessionStorage.removeItem("pk.user"); } catch { /* sin almacenamiento */ } }
let user = storedUser();
let transactions = [];

function renderUser() {
    el("user-gate").classList.toggle("hidden", !!user);
    el("user-name").textContent = user || "—";
    el("user-avatar").textContent = (user || "··").slice(0, 2).toUpperCase();
}
el("user-form").addEventListener("submit", event => { event.preventDefault(); user = new FormData(event.target).get("user").trim(); storeUser(user); renderUser(); });
el("change-user-btn").addEventListener("click", () => { user = null; storeUser(null); renderUser(); });

document.querySelectorAll(".nav-btn").forEach(button => button.addEventListener("click", () => {
    document.querySelectorAll(".nav-btn").forEach(b => b.classList.toggle("active", b === button));
    document.querySelectorAll(".view").forEach(v => v.classList.toggle("active", v.id === `view-${button.dataset.tab}`));
    el("topbar-title").textContent = button.dataset.title;
    el("topbar-lede").textContent = button.dataset.lede;
    if (button.dataset.tab === "transactions") loadTransactions(); else loadOperation();
}));

function renderTransactions() {
    const status = el("status-filter").value;
    const q = el("tx-search").value.trim().toLowerCase();
    const rows = transactions.filter(t => (!status || t.status === status)
        && (!q || [t.authorizationCode, t.orderReference, t.idempotencyKey].some(v => (v || "").toLowerCase().includes(q))));
    const approved = transactions.filter(t => t.approved);
    el("kpis").innerHTML = `<div><span>Aprobadas</span><strong>${approved.length}</strong></div>
        <div><span>Rechazadas</span><strong>${transactions.length - approved.length}</strong></div>
        <div><span>Monto aprobado</span><strong>${money(approved.reduce((s, t) => s + Number(t.amount), 0), "GTQ")}</strong></div>`;
    el("tx-table").innerHTML = rows.length ? `<table><thead><tr><th>Fecha</th><th>Autorización</th><th>Pedido</th><th>Medio</th><th>Monto</th><th>Estado</th></tr></thead><tbody>
        ${rows.map(t => `<tr>
            <td>${formatDate(t.processedAt)}</td>
            <td><span class="cell-primary">${escapeHtml(t.authorizationCode || "—")}</span><span class="cell-sub">llave ${escapeHtml(t.idempotencyKey.slice(0, 24))}</span></td>
            <td class="mono">${escapeHtml(t.orderReference.slice(0, 8))}</td>
            <td>${t.method === "TARJETA" ? "Tarjeta tokenizada" : "Transferencia"}<span class="cell-sub">${escapeHtml(t.paymentReference)}</span></td>
            <td class="num">${money(t.amount, t.currency)}</td>
            <td>${t.approved ? badge("ok", "Aprobada") : badge("error", "Rechazada")}${t.rejectionCode ? `<span class="cell-sub">${escapeHtml(t.rejectionCode)}</span>` : ""}</td>
        </tr>`).join("")}</tbody></table>` : `<p class="empty-state">${transactions.length ? "Ninguna transacción coincide." : "Aún no hay transacciones del comercio."}</p>`;
}
async function loadTransactions() {
    try { transactions = await api("GET", "/api/portal/transactions"); renderTransactions(); }
    catch (error) { el("tx-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`; }
}
el("status-filter").addEventListener("change", renderTransactions);
el("tx-search").addEventListener("input", renderTransactions);
el("refresh-btn").addEventListener("click", loadTransactions);

async function loadOperation() {
    try {
        const op = await api("GET", "/api/portal/operation");
        document.querySelectorAll("#mode-options .seg").forEach(b => b.classList.toggle("active", b.dataset.mode === op.mode));
        el("service-mode").textContent = `Servicio: ${MODE[op.mode]}`;
        el("mode-note").textContent = `Modo ${MODE[op.mode]} desde ${formatDate(op.updatedAt)} (${op.updatedBy}). Comercio afiliado: ${op.merchant}.`
            + (op.mode === "LENTO" ? ` Las respuestas tardan ${op.slowLatencyMs / 1000} s.` : "");
    } catch (error) { el("mode-note").textContent = error.message; }
}
el("mode-options").addEventListener("click", async event => {
    const button = event.target.closest("[data-mode]");
    if (!button) return;
    if (!user) { renderUser(); return; }
    try {
        await api("PUT", "/api/portal/operation", { mode: button.dataset.mode, updatedBy: user });
        toast(`Servicio en modo ${MODE[button.dataset.mode]}.`, "ok");
        loadOperation();
    } catch (error) { toast(error.message, "error"); }
});

renderUser();
loadTransactions();
loadOperation();
setInterval(() => { if (document.querySelector('[data-tab="transactions"]').classList.contains("active") && !document.hidden) loadTransactions(); }, 10000);
