"use strict";

const DETAIL_CORRELATION_ID = crypto.randomUUID();
const DETAIL_FAVORITES_KEY = "sgo.megastore.favorites";
const detailSession = sgoGetSession();
let detailProduct = null;

function detailEscape(value) { return String(value ?? "").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;"); }
function detailMoney(value) { return new Intl.NumberFormat("es-GT", { style:"currency", currency:"GTQ" }).format(value); }
function detailFavorites() { try { return new Set(JSON.parse(localStorage.getItem(DETAIL_FAVORITES_KEY)) || []); } catch { return new Set(); } }

async function renderDetailNavigation() {
    const nav = document.getElementById("detail-account-nav");
    if (detailSession?.role === "CLIENTE") {
        nav.innerHTML = `<a href="cliente.html#favorites"><span aria-hidden="true">♡</span><small>Ver</small>Favoritos</a><a href="cliente.html#tracking"><span aria-hidden="true">▤</span><small>Mis</small>Pedidos</a><button type="button" data-detail-logout><span aria-hidden="true">○</span><small>${detailEscape(detailSession.name)}</small>Cerrar sesión</button><a href="cliente.html#checkout" class="header-cart"><span aria-hidden="true">Cart</span><small>Mi carrito</small>Ver compra</a>`;
        nav.querySelector("[data-detail-logout]").addEventListener("click", sgoLogout);
    } else {
        nav.innerHTML = `<a href="login.html"><span aria-hidden="true">○</span><small>Mi cuenta</small>Iniciar sesión</a><a href="login.html" class="header-cart"><span aria-hidden="true">Cart</span><small>Mi carrito</small>0 productos</a>`;
    }
}

async function loadProductDetail() {
    const productId = new URLSearchParams(location.search).get("id");
    const shell = document.getElementById("product-detail");
    try {
        const catalogResponse = await fetch("catalog.json", { cache:"no-store" });
        const catalog = await catalogResponse.json();
        detailProduct = catalog.find(product => product.id === productId);
        if (!detailProduct) throw new Error("Producto no encontrado");
        const stockResponse = await fetch(`/api/v1/inventory/stock?productId=${detailProduct.id}`, { headers:{ "X-Correlation-Id":DETAIL_CORRELATION_ID } });
        const stockRows = stockResponse.ok ? await stockResponse.json() : [];
        const available = stockRows.reduce((sum,row) => sum + Number(row.quantityAvailable ?? row.availableQuantity ?? 0), 0);
        const favorite = detailFavorites().has(detailProduct.id);
        document.title = `${detailProduct.name} | MegaStore`;
        document.getElementById("detail-breadcrumb").textContent = detailProduct.name;
        shell.innerHTML = `<div class="product-detail-media"><img src="${detailEscape(detailProduct.image)}" alt="${detailEscape(detailProduct.name)}" width="900" height="900" /></div><article class="product-detail-copy"><p class="detail-category">${detailEscape(detailProduct.category)} <span>${detailEscape(detailProduct.sku)}</span></p><h1>${detailEscape(detailProduct.name)}</h1><p class="detail-description">${detailEscape(detailProduct.description)}</p><dl class="detail-specs"><div><dt>Marca</dt><dd>${detailEscape(detailProduct.brand)}</dd></div><div><dt>Color</dt><dd>${detailEscape(detailProduct.color)}</dd></div><div><dt>Material</dt><dd>${detailEscape(detailProduct.material)}</dd></div><div><dt>Garantía</dt><dd>${detailEscape(detailProduct.warranty)}</dd></div></dl><strong class="detail-price">${detailMoney(detailProduct.price)}</strong><p class="detail-stock ${available === 0 ? "out" : ""}">${available > 0 ? `${available} unidades disponibles` : "Producto agotado"}</p><div class="detail-actions"><button id="detail-buy" ${available === 0 ? "disabled" : ""}>${available === 0 ? "Agotado" : "Agregar al carrito"}</button><button id="detail-favorite" class="secondary" aria-pressed="${favorite}">${favorite ? "Quitar de favoritos" : "Guardar en favoritos"}</button></div><section class="stock-locations"><h2>Disponibilidad por ubicación</h2>${stockRows.length ? stockRows.map(row => `<div><span>${detailEscape(row.locationName)}</span><strong>${Number(row.quantityAvailable ?? row.availableQuantity ?? 0)} disponibles</strong></div>`).join("") : `<p>No fue posible consultar el detalle de inventario.</p>`}</section></article>`;
        document.getElementById("detail-buy")?.addEventListener("click", buyDetailProduct);
        document.getElementById("detail-favorite").addEventListener("click", toggleDetailFavorite);
    } catch {
        shell.innerHTML = `<div class="product-detail-error"><h1>Producto no disponible</h1><p>No encontramos el artículo solicitado.</p><a href="index.html#catalogo">Volver al catálogo</a></div>`;
    }
}

function buyDetailProduct() {
    sessionStorage.setItem("sgo.purchaseIntent", JSON.stringify({ productId:detailProduct.id, createdAt:new Date().toISOString() }));
    if (detailSession?.role === "CLIENTE") { location.href = "cliente.html#checkout"; return; }
    document.getElementById("detail-auth-dialog").showModal();
}

function toggleDetailFavorite() {
    const favorites = detailFavorites();
    favorites.has(detailProduct.id) ? favorites.delete(detailProduct.id) : favorites.add(detailProduct.id);
    localStorage.setItem(DETAIL_FAVORITES_KEY, JSON.stringify([...favorites]));
    const active = favorites.has(detailProduct.id);
    const button = document.getElementById("detail-favorite");
    button.textContent = active ? "Quitar de favoritos" : "Guardar en favoritos";
    button.setAttribute("aria-pressed", String(active));
}

document.getElementById("detail-search-btn").addEventListener("click", () => { location.href = `index.html?search=${encodeURIComponent(document.getElementById("detail-search").value)}#catalogo`; });
document.getElementById("detail-search").addEventListener("keydown", event => { if (event.key === "Enter") document.getElementById("detail-search-btn").click(); });
document.querySelector("[data-close-detail-auth]").addEventListener("click", () => document.getElementById("detail-auth-dialog").close());
renderDetailNavigation(); loadProductDetail();
