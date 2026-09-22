# UI-01: base de frontend (Kevin Girón). Página estática sin build tooling
# (HTML/CSS/JS planos), servida por nginx con proxy inverso a /api/ para
# evitar configurar CORS en el backend. Tres páginas: index (landing),
# cliente (tienda omnicanal) y staff (consola operativa).
FROM nginx:1.29-alpine

COPY index.html login.html cliente.html staff.html /usr/share/nginx/html/
COPY session.js app.js app-cliente.js app-login.js /usr/share/nginx/html/
COPY styles.css landing.css store.css /usr/share/nginx/html/
COPY favicon.svg /usr/share/nginx/html/
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/ >/dev/null || exit 1
