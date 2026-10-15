"use strict";

const PUBLIC_CORRELATION_ID = crypto.randomUUID();
const publicSession = typeof sgoGetSession === "function" ? sgoGetSession() : null;
const FAVORITES_KEY = publicSession?.customerId
    ? `sgo.megastore.favorites.${publicSession.customerId}`
    : "sgo.megastore.favorites.public";
const PURCHASE_INTENT_KEY = "sgo.purchaseIntent";
let publicCatalog = [];
let publicStocks = new Map();
let activeCategory = "all";
const MAX_FEATURED_CATEGORIES = 4;
const CATEGORY_PRIORITY = ["Hogar", "Moda", "Tecnología", "Calzado"];

function landingMoney(value) { return new Intl.NumberFormat("es-GT", { style: "currency", currency: "GTQ" }).format(value); }
function landingEscape(value) { return String(value ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;"); }
function getFavorites() { try { return new Set(JSON.parse(localStorage.getItem(FAVORITES_KEY)) || []); } catch { return new Set(); } }
function saveFavorites(favorites) { localStorage.setItem(FAVORITES_KEY, JSON.stringify([...favorites])); }

function catalogCategories() {
    const counts = new Map();
    publicCatalog.forEach(product => {
        const category = String(product.category || "General").trim() || "General";
        counts.set(category, (counts.get(category) || 0) + 1);
    });
    return [...counts.entries()]
        .map(([name, count]) => ({ name, count }))
        .sort((left, right) => {
            const leftPriority = CATEGORY_PRIORITY.indexOf(left.name);
            const rightPriority = CATEGORY_PRIORITY.indexOf(right.name);
            if (leftPriority !== -1 || rightPriority !== -1) {
                if (leftPriority === -1) return 1;
                if (rightPriority === -1) return -1;
                return leftPriority - rightPriority;
            }
            return left.name.localeCompare(right.name, "es", { sensitivity: "base" });
        });
}

function categoryButton(category, count, menuItem = false) {
    const active = activeCategory === category;
    if (menuItem) {
        return `<button type="button" data-category="${landingEscape(category)}" class="category-menu-item${active ? " active" : ""}" aria-pressed="${active}"><span>${landingEscape(category)}</span><small>${count} producto${count === 1 ? "" : "s"}</small><i aria-hidden="true">→</i></button>`;
    }
    return `<button type="button" data-category="${landingEscape(category)}" class="${active ? "active" : ""}" aria-pressed="${active}">${landingEscape(category)}</button>`;
}

function renderCategoryNavigation() {
    const categories = catalogCategories();
    document.getElementById("featured-categories").innerHTML = categories
        .slice(0, MAX_FEATURED_CATEGORIES)
        .map(category => categoryButton(category.name, category.count))
        .join("");
    document.getElementById("all-category-options").innerHTML = categories.length
        ? categories.map(category => categoryButton(category.name, category.count, true)).join("")
        : `<p class="category-menu-empty">Las categorías aparecerán cuando existan productos publicados.</p>`;
    document.getElementById("category-total").textContent = String(categories.length);
}

function selectCategory(category) {
    activeCategory = category;
    document.querySelectorAll("[data-category]").forEach(item => {
        const active = item.dataset.category === category;
        item.classList.toggle("active", active);
        item.setAttribute("aria-pressed", String(active));
    });
    document.getElementById("all-categories").removeAttribute("open");
    renderPublicCatalog();
    document.getElementById("catalogo").scrollIntoView({ behavior: "smooth", block: "start" });
}

async function publicCartCount() {
    if (publicSession?.role !== "CLIENTE") return 0;
    try {
        const response = await fetch(`/api/v1/carts/active?customerId=${publicSession.customerId}`, { headers: { "X-Correlation-Id": PUBLIC_CORRELATION_ID } });
        if (!response.ok) return 0;
        return Number((await response.json()).itemCount || 0);
    } catch { return 0; }
}

async function renderAccountNavigation() {
    const nav = document.getElementById("public-account-nav");
    const count = await publicCartCount();
    if (publicSession?.role === "CLIENTE") {
        nav.innerHTML = `<a href="cliente.html#favorites"><span aria-hidden="true">♡</span><small>Ver</small>Favoritos</a><a href="cliente.html#tracking"><span aria-hidden="true">▤</span><small>Mis</small>Pedidos</a><button type="button" data-public-logout><span aria-hidden="true">○</span><small>${landingEscape(publicSession.name)}</small>Cerrar sesión</button><a href="cliente.html#checkout" class="header-cart"><span aria-hidden="true">Cart</span><small>Mi carrito</small>${count} producto${count === 1 ? "" : "s"}</a>`;
        nav.querySelector("[data-public-logout]").addEventListener("click", sgoLogout);
    } else {
        nav.innerHTML = `<a href="login.html"><span aria-hidden="true">○</span><small>Mi cuenta</small>Iniciar sesión</a><a href="registro.html"><span aria-hidden="true">+</span><small>¿Eres nuevo?</small>Crear cuenta</a><a href="login.html" class="header-cart"><span aria-hidden="true">Cart</span><small>Mi carrito</small>0 productos</a>`;
    }
}

function renderPublicCatalog() {
    const query = document.getElementById("public-search").value.trim().toLowerCase();
    const favorites = getFavorites();
    const products = publicCatalog.filter(product => {
        const category = String(product.category || "General").trim() || "General";
        return (activeCategory === "all" || category === activeCategory)
            && `${product.name} ${category} ${product.sku}`.toLowerCase().includes(query);
    });
    const container = document.getElementById("public-catalog-grid");
    const status = document.getElementById("public-catalog-status");
    const categoryLabel = activeCategory === "all" ? "todas las categorías" : activeCategory;
    status.textContent = query
        ? `${products.length} resultado${products.length === 1 ? "" : "s"} en ${categoryLabel}`
        : `${products.length} producto${products.length === 1 ? "" : "s"} en ${categoryLabel}`;
    if (!products.length) { container.innerHTML = `<p class="catalog-empty">No encontramos productos con esos filtros.</p>`; return; }
    container.innerHTML = products.map(product => {
        const stock = publicStocks.get(product.id);
        const isFavorite = favorites.has(product.id);
        const stockText = stock === undefined ? "Inventario en consulta" : stock > 0 ? `${stock} unidades disponibles` : "Agotado";
        return `<article class="market-product"><a class="market-product-image" href="producto.html?id=${encodeURIComponent(product.id)}"><img src="${landingEscape(product.image)}" alt="${landingEscape(product.name)}" width="900" height="900" loading="lazy" /></a><button class="favorite-button ${isFavorite ? "active" : ""}" data-favorite-product="${product.id}" aria-label="${isFavorite ? "Quitar de favoritos" : "Agregar a favoritos"}" aria-pressed="${isFavorite}">♡</button><div class="market-product-copy"><p>${landingEscape(product.category)} <span>${landingEscape(product.sku)}</span></p><a href="producto.html?id=${encodeURIComponent(product.id)}"><h3>${landingEscape(product.name)}</h3></a><strong>${landingMoney(product.price)}</strong><span class="market-stock ${stock === 0 ? "out" : ""}">${stockText}</span><div><a href="producto.html?id=${encodeURIComponent(product.id)}">Ver detalle</a><button data-buy-product="${product.id}" ${stock === 0 ? "disabled" : ""}>${stock === 0 ? "Agotado" : "Agregar"}</button></div></div></article>`;
    }).join("");
}

async function loadPublicCatalog() {
    const status = document.getElementById("public-catalog-status");
    try {
        publicCatalog = await sgoLoadCatalog();
        publicCatalog.forEach(product => publicStocks.set(product.id, product.available));
        renderCategoryNavigation();
        renderPublicCatalog();
    } catch {
        status.textContent = "Catálogo no disponible";
        document.getElementById("public-catalog-grid").innerHTML = `<p class="catalog-empty">No pudimos cargar los productos. Intenta nuevamente.</p>`;
    }
}

function requestAuthentication(productId) {
    sessionStorage.setItem(PURCHASE_INTENT_KEY, JSON.stringify({ productId, createdAt: new Date().toISOString() }));
    if (publicSession?.role === "CLIENTE") { location.href = "cliente.html#checkout"; return; }
    document.getElementById("auth-required-dialog").showModal();
}

document.getElementById("public-catalog-grid").addEventListener("click", event => {
    const favoriteButton = event.target.closest("[data-favorite-product]");
    if (favoriteButton) {
        const favorites = getFavorites();
        favorites.has(favoriteButton.dataset.favoriteProduct) ? favorites.delete(favoriteButton.dataset.favoriteProduct) : favorites.add(favoriteButton.dataset.favoriteProduct);
        saveFavorites(favorites); renderPublicCatalog(); return;
    }
    const buyButton = event.target.closest("[data-buy-product]");
    if (buyButton) requestAuthentication(buyButton.dataset.buyProduct);
});
document.getElementById("department-nav").addEventListener("click", event => {
    const button = event.target.closest("[data-category]");
    if (button) selectCategory(button.dataset.category);
});
document.addEventListener("click", event => {
    const menu = document.getElementById("all-categories");
    if (menu.open && !menu.contains(event.target)) menu.removeAttribute("open");
});
document.addEventListener("keydown", event => {
    if (event.key === "Escape") document.getElementById("all-categories").removeAttribute("open");
});
document.getElementById("public-search").addEventListener("input", renderPublicCatalog);
document.getElementById("public-search-btn").addEventListener("click", () => document.getElementById("catalogo").scrollIntoView({ behavior: "smooth" }));
document.querySelector("[data-close-auth]").addEventListener("click", () => document.getElementById("auth-required-dialog").close());
const initialSearch = new URLSearchParams(location.search).get("search");
if (initialSearch) document.getElementById("public-search").value = initialSearch;
renderAccountNavigation(); loadPublicCatalog();
