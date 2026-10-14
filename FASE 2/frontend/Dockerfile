# UI-01: base de frontend (Kevin Girón). Página estática sin build tooling
# (HTML/CSS/JS planos), servida por nginx con proxy inverso a /api/ para
# evitar configurar CORS en el backend. Páginas: index (landing), cliente
# (tienda omnicanal), staff (consola operativa) y tienda-online
# (administración de la tienda online: recepción por folio y catálogo).
# El catálogo ya no es un archivo: lo sirve /api/v1/online-catalog.
FROM nginx:1.29-alpine

COPY index.html login.html registro.html cliente.html producto.html staff.html tienda-online.html /usr/share/nginx/html/
COPY session.js app.js app-cliente.js app-login.js app-producto.js app-tienda-online.js /usr/share/nginx/html/
COPY app-landing.js /usr/share/nginx/html/
COPY app-registro.js /usr/share/nginx/html/
COPY styles.css landing.css store.css tienda-online.css invoice.css /usr/share/nginx/html/
COPY favicon.svg /usr/share/nginx/html/
COPY assets/ /usr/share/nginx/html/assets/
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/ >/dev/null || exit 1
