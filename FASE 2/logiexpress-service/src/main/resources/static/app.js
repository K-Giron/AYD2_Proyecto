// Portal de operación de LogiExpress: mensajeros (recolección y entrega),
// rastreo de guías y estado de la API que consumen los comercios.

function el(id) { return document.getElementById(id); }
function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
function formatDate(iso) { return iso ? new Date(iso).toLocaleString("es-GT", { dateStyle: "short", timeStyle: "medium" }) : "—"; }
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

const STATUS = {
    CREADA: ["warn", "Por recolectar"], EN_TRANSITO: ["warn", "En tránsito"],
    EN_PUNTO_DE_RETIRO: ["ok", "En tienda de retiro"], ENTREGADA: ["ok", "Entregada"],
};
const MODE = { NORMAL: "Normal", LENTO: "Lento", FUERA_DE_SERVICIO: "Fuera de servicio" };
const statusBadge = status => badge(...(STATUS[status] || ["muted", status]));

function storedCourier() { try { return sessionStorage.getItem("lgx.courier"); } catch { return null; } }
function storeCourier(name) { try { name ? sessionStorage.setItem("lgx.courier", name) : sessionStorage.removeItem("lgx.courier"); } catch { /* sin almacenamiento */ } }
let courier = storedCourier();
let guides = [];

function renderCourier() {
    el("courier-gate").classList.toggle("hidden", !!courier);
    el("courier-name").textContent = courier || "—";
    el("courier-avatar").textContent = (courier || "··").slice(0, 2).toUpperCase();
}
el("courier-form").addEventListener("submit", event => {
    event.preventDefault();
    courier = new FormData(event.target).get("courier").trim();
    storeCourier(courier); renderCourier();
});
el("change-courier-btn").addEventListener("click", () => { courier = null; storeCourier(null); renderCourier(); });

document.querySelectorAll(".nav-btn").forEach(button => button.addEventListener("click", () => {
    document.querySelectorAll(".nav-btn").forEach(b => b.classList.toggle("active", b === button));
    document.querySelectorAll(".view").forEach(v => v.classList.toggle("active", v.id === `view-${button.dataset.tab}`));
    el("topbar-title").textContent = button.dataset.title;
    el("topbar-lede").textContent = button.dataset.lede;
    if (button.dataset.tab === "guides") loadGuides();
    if (button.dataset.tab === "operation") loadOperation();
}));

function actionFor(guide) {
    if (guide.status === "CREADA") return `<button class="btn btn-primary btn-sm" data-act="pickup" data-guide="${guide.trackingNumber}">Recolectar</button>`;
    if (guide.status === "EN_TRANSITO") {
        return guide.pickupPoint
            ? `<button class="btn btn-primary btn-sm" data-act="deliver" data-guide="${guide.trackingNumber}">Dejar en tienda ${escapeHtml(guide.pickupPoint)}</button>`
            : `<button class="btn btn-primary btn-sm" data-act="deliver" data-guide="${guide.trackingNumber}">Entregar</button>`;
    }
    return "";
}

function renderGuides() {
    const status = el("status-filter").value;
    const q = el("guide-search").value.trim().toLowerCase();
    const rows = guides.filter(g => (!status || g.status === status)
        && (!q || [g.trackingNumber, g.orderReference, g.destination, g.merchantReference].some(v => (v || "").toLowerCase().includes(q))));
    el("guides-table").innerHTML = rows.length ? `<table><thead><tr><th>Guía</th><th>Origen</th><th>Destino</th><th>Estado</th><th>Actualizada</th><th></th></tr></thead><tbody>
        ${rows.map(g => `<tr>
            <td><span class="cell-primary">${escapeHtml(g.trackingNumber)}</span><span class="cell-sub">Pedido ${escapeHtml((g.orderReference || "—").slice(0, 8))}</span></td>
            <td>${escapeHtml(g.origin)}</td>
            <td>${escapeHtml(g.destination)}${g.pickupPoint ? `<span class="cell-sub">Retiro en tienda ${escapeHtml(g.pickupPoint)}</span>` : ""}</td>
            <td>${statusBadge(g.status)}${g.courier ? `<span class="cell-sub">${escapeHtml(g.courier)}</span>` : ""}</td>
            <td>${formatDate(g.updatedAt)}</td>
            <td><div class="row-actions">${actionFor(g)}<button class="btn btn-ghost btn-sm" data-act="track" data-guide="${g.trackingNumber}">Rastreo</button></div></td>
        </tr>`).join("")}</tbody></table>` : `<p class="empty-state">${guides.length ? "Ninguna guía coincide con el filtro." : "Todavía no hay guías. Aparecen cuando un comercio solicita un envío."}</p>`;
    const pending = guides.filter(g => g.status === "CREADA" || g.status === "EN_TRANSITO").length;
    el("pending-count").textContent = pending;
    el("pending-count").classList.toggle("hidden", pending === 0);
}

async function loadGuides() {
    try { guides = await api("GET", "/api/portal/guides"); renderGuides(); }
    catch (error) { el("guides-table").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`; }
}
el("status-filter").addEventListener("change", renderGuides);
el("guide-search").addEventListener("input", renderGuides);
el("refresh-btn").addEventListener("click", loadGuides);

el("guides-table").addEventListener("click", async event => {
    const button = event.target.closest("[data-act]");
    if (!button) return;
    const number = button.dataset.guide;
    if (button.dataset.act === "track") {
        document.querySelector('[data-tab="tracking"]').click();
        el("tracking-input").value = number;
        return showTracking(number);
    }
    if (!courier) { renderCourier(); return; }
    button.disabled = true;
    try {
        const body = { courier };
        if (button.dataset.act === "deliver") {
            const receivedBy = prompt("¿Quién recibe? (opcional)") || "";
            body.receivedBy = receivedBy;
        }
        const guide = await api("POST", `/api/portal/guides/${encodeURIComponent(number)}/${button.dataset.act}`, body);
        toast(`${guide.trackingNumber}: ${STATUS[guide.status]?.[1] || guide.status}.`, "ok");
        await loadGuides();
    } catch (error) { toast(error.message, "error"); button.disabled = false; }
});

async function showTracking(number) {
    const box = el("tracking-result");
    try {
        const g = await api("GET", `/api/portal/guides/${encodeURIComponent(number.trim())}`);
        box.innerHTML = `<div class="guide-head"><strong>${escapeHtml(g.trackingNumber)}</strong>${statusBadge(g.status)}
                <span class="muted">${escapeHtml(g.origin)} → ${escapeHtml(g.destination)}</span></div>
            <div class="timeline">${g.events.map(e => `<div class="timeline-item"><span class="timeline-dot"></span>
                <div>${statusBadge(e.status)} ${escapeHtml(e.note || "")}<small>${formatDate(e.occurredAt)} · ${escapeHtml(e.actor)}</small></div></div>`).join("")}</div>`;
    } catch (error) { box.innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`; }
}
el("tracking-form").addEventListener("submit", event => { event.preventDefault(); showTracking(el("tracking-input").value); });

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
    if (!courier) { renderCourier(); return; }
    try {
        await api("PUT", "/api/portal/operation", { mode: button.dataset.mode, updatedBy: courier });
        toast(`Servicio en modo ${MODE[button.dataset.mode]}.`, "ok");
        loadOperation();
    } catch (error) { toast(error.message, "error"); }
});

renderCourier();
loadGuides();
loadOperation();
setInterval(() => { if (document.querySelector('[data-tab="guides"]').classList.contains("active") && !document.hidden) loadGuides(); }, 10000);
