# SGO-Logic — Fase 2

## Objetivo

Materializar el DDA de la Fase 1 como un MVP reproducible de gestión de órdenes y logística omnicanal. El repositorio contiene el backbone técnico y una Saga reentrante de carrito, reserva, pago, confirmación fiscal y creación automática del despacho.

## Arquitectura

El backend es un único JAR organizado como monolito modular. Cada paquete directo bajo `com.megastore.sgologic` es un módulo Spring Modulith con identidad estable. Cada módulo será propietario de su lógica y datos; la colaboración futura será mediante APIs públicas o eventos, nunca mediante repositorios, entidades JPA, adaptadores o tablas privadas ajenas. `shared` contiene solo infraestructura transversal mínima.

Docker Compose levanta **12 contenedores** en una red bridge dedicada al proyecto: SGO-Logic (`frontend`, `backend`, `postgres`, `rabbitmq`) y cuatro sistemas propios, cada uno con su base de datos (almacén central, tienda física POS-01, PayKlik y LogiExpress; ver "Sistemas propios de Fase 2" y ADR-015). La red no se comparte con otros proyectos; sus puertos publicados permiten el acceso local requerido. En un ambiente real, PostgreSQL y RabbitMQ deben permanecer sin exposición pública.

## Estructura

```text
FASE 2/
├── backend/              SGO-Logic: Spring Boot, módulos y pruebas
├── frontend/             Tienda web, consola interna y tienda online (nginx)
├── inventory-service/    Almacén central (sistema propio, :8082)
├── store-service/        Tienda física POS-01, POS local (:8084)
├── payklik-service/      Pasarela PayKlik (:8086)
├── logiexpress-service/  Transportista LogiExpress (:8085)
├── scripts/              reset-demo-data.sh (limpia los datos de operación)
├── simulator-data/   Salidas reproducibles futuras de simuladores
├── tests/            Arquitectura, smoke, E2E, k6, seguridad y datos
├── docs/             ADR, contratos, integración, evidencia y diagramas
├── .env.example
├── .gitignore
└── docker-compose.yml

.github/workflows/
└── qa-integracion-base.yml
```

`frontend/` (Kevin Girón, UI-01) es un sitio estático, sin build tooling (HTML/CSS/JS planos), servido por nginx con proxy inverso hacia `/api/` para evitar configurar CORS:

- `index.html` — landing que elige entre las dos experiencias siguientes.
- `cliente.html` (tema claro, `store.css`) — tienda omnicanal con catálogo público, carrito persistente, checkout real mediante `OrderSagaService`, pedidos, seguimiento y devoluciones. La Saga se puede repetir para continuar una confirmación SAT pendiente sin duplicar reservas, cobros, facturas ni despachos.
- `registro.html` y `login.html` — registro de clientes y acceso por rol.
- `tienda-online.html` — administración de la tienda online (POS-ONLINE): recepción por folio, inventario CD-WEB, catálogo publicado con imagen, ventas y facturas, traslados y bitácora.
- `staff.html` (tema oscuro, `styles.css`) — consola operativa. Desde Fase 2 incluye lotes ERP, eventos POS, informe de ventas conciliado, kárdex de CD-WEB y call center por correo. Funciones originales: para Personal de logística/atención al cliente (actores reales de CDU-07, 09, 10 y 11): buscar una orden y gestionar despacho (LOG-01/02/03), notificaciones (NOT-01), resumen consolidado (QRY-01) y devoluciones (INC-01).

## Tecnologías y versiones

| Elemento | Versión |
|---|---|
| Java / Eclipse Temurin | 25 / 25.0.4_7 en Docker |
| Spring Boot | 4.1.1 |
| Spring Modulith | 2.1.1 |
| Maven | 3.9.11 |
| Maven Wrapper | 3.3.4 (scripts del proyecto) |
| PostgreSQL | 17.11, Alpine 3.23 |
| RabbitMQ | 4.3.5 Management, Alpine |
| Testcontainers | 2.0.5, administrado por Spring Boot |
| JUnit Jupiter | 6.0.3, administrado por Spring Boot |
| AssertJ | 3.27.7, administrado por Spring Boot |
| Empaquetado | JAR |

## Propietarios

| Responsable | Módulos o áreas |
|---|---|
| Jairo Gómez | plataforma, `orders`, Saga e integración E2E |
| Jorge Pumay | `inventory`, reservas y modelo relacional |
| Edson Sosa | `payments`, `audit`, `security`, Outbox e infraestructura de integración |
| Andy Sánchez | `fiscal`, SAT, ERP, POS y simuladores |
| Kevin Girón | `logistics`, `notifications`, `query`, `incidents` y frontend |

`integration` contiene los adaptadores legacy ERP/POS entregados por Andy. `messaging` implementa Outbox, publicación RabbitMQ e Inbox idempotente; `audit` conserva evidencia append-only y registra sus accesos. Query y Notifications ya consumen eventos de orden; las conexiones hacia ERP y POS continúan bajo revisión técnica de Edson y coordinación de Jairo.

## Requisitos locales

- JDK 25 para ejecución fuera de Docker.
- Docker Engine y Docker Compose.
- `curl` o `wget`, `tar` y `sha512sum` para la primera descarga del wrapper en Linux/macOS.
- No se requiere Maven global.

## Variables de ambiente

Copie manualmente `.env.example` como `.env` solo si desea personalizar el entorno; `.env` está ignorado y nunca debe versionarse. Las variables disponibles son:

- `BACKEND_PORT`
- `FRONTEND_PORT`
- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_HOST`, `POSTGRES_PORT`
- `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `RABBITMQ_HOST`, `RABBITMQ_PORT`
- `RABBITMQ_MANAGEMENT_PORT`
- `SPRING_PROFILES_ACTIVE`
- `SGO_SIMULATORS_{PAYKLIK,SAT,ERP,POS,LOGIEXPRESS,NOTIFICATIONS}_MODE`
- `SGO_SIMULATORS_{PAYKLIK,SAT,ERP,POS,LOGIEXPRESS,NOTIFICATIONS}_LATENCY_MS`
- `SGO_SIMULATORS_{PAYKLIK,SAT,ERP,POS,LOGIEXPRESS,NOTIFICATIONS}_FAILURE_RATE`
- `SGO_SIMULATORS_POS_UNAVAILABLE_STORES`
- `SGO_ERP_FORMAT`, `SGO_ERP_INTERVAL_MS`, `SGO_ERP_INITIAL_DELAY_MS`
- `SGO_OUTBOX_ENABLED`, `SGO_OUTBOX_POLL_INTERVAL_MS`, `SGO_OUTBOX_BATCH_SIZE`, `SGO_OUTBOX_MAX_ATTEMPTS`, `SGO_OUTBOX_RETRY_DELAYS`
- `SGO_OUTBOX_EXCHANGE`, `SGO_OUTBOX_DLX`, `SGO_OUTBOX_DLQ`
- `SGO_SECURITY_ENABLED`, `SGO_SECURITY_JWT_SECRET`, `SGO_SECURITY_JWT_ISSUER`, `SGO_SECURITY_JWT_TTL`
- `SGO_SECURITY_{CLIENT,SUPPORT,LOGISTICS,FINANCE}_{USER,PASSWORD}`
- `SGO_PAYKLIK_TIMEOUT_MS`, `SGO_PAYKLIK_CALL_POOL_SIZE`, `SGO_PAYKLIK_FAILURE_RATE_THRESHOLD`, `SGO_PAYKLIK_SLIDING_WINDOW_SIZE`, `SGO_PAYKLIK_MINIMUM_CALLS`, `SGO_PAYKLIK_OPEN_STATE_WAIT`, `SGO_PAYKLIK_HALF_OPEN_CALLS`
- Fase 2 (ADR-015):
  - `SGO_PAYKLIK_BASE_URL`, `PAYKLIK_MERCHANT_API_KEY` y `SGO_LOGIEXPRESS_BASE_URL`, `LOGIEXPRESS_MERCHANT_API_KEY`, `SGO_LOGIEXPRESS_TIMEOUT_MS`, `SGO_LOGIEXPRESS_TRACKING_INTERVAL_MS`. Con la URL vacía (pruebas) se usan los simuladores.
  - `SGO_ERP_OUTBOX_DIR` (carpeta del ERP), `SGO_POS_STORE_01_ENDPOINT` (POS real de la tienda 1), `SGO_POS_TIMEOUT_MS`.
  - `SGO_INVENTORY_RESERVATION_TTL` (15 min por defecto; `PT2M` para probar la liberación) y `SGO_INVENTORY_EXPIRATION_SWEEP_INTERVAL`.
  - `SGO_MAIL_USERNAME` y `SGO_MAIL_PASSWORD` (correo real por Gmail con contraseña de aplicación; vacíos = simulador), `SGO_PUBLIC_SITE_URL`.
  - Cuentas: `SGO_SECURITY_ONLINE_STORE_*`, `SGO_SECURITY_STORE_TERMINAL_*`, `SGO_SECURITY_CENTRAL_EVENTS_*`.
  - Almacén central: `INVENTORY_*` (base, JWT, cuentas por rol y por punto de venta, URLs de eventos de stock).
  - Tienda física: `STORE_*`. Proveedores: `PAYKLIK_*` y `LOGIEXPRESS_*` (bases y puertos).
- `SGO_PAYMENT_RECONCILIATION_ENABLED`, `SGO_PAYMENT_RECONCILIATION_POLL_INTERVAL_MS`, `SGO_PAYMENT_RECONCILIATION_BATCH_SIZE`, `SGO_PAYMENT_RECONCILIATION_MAX_ATTEMPTS`, `SGO_PAYMENT_RECONCILIATION_FIRST_DELAY`, `SGO_PAYMENT_RECONCILIATION_STALE_PENDING_AFTER`, `SGO_PAYMENT_RECONCILIATION_RETRY_DELAYS`, `SGO_PAYMENT_RECONCILIATION_ALERT_AFTER`

Los valores predeterminados son exclusivamente locales y ficticios.

## Pruebas y construcción

Desde `FASE 2/backend`:

```bash
./mvnw clean verify
```

La verificación incluye pruebas unitarias, MVC, arquitectura modular e integración con contenedores reales de PostgreSQL y RabbitMQ. Docker debe estar activo para ejecutar la suite completa.

La suite integrada cubre PostgreSQL/RabbitMQ, concurrencia de inventario, arquitectura modular, HTTP, carrito y órdenes, pagos y conciliación, fiscal/SAT, ERP, POS, LogiExpress, notificaciones, consulta, incidencias, Outbox/Inbox, auditoría y seguridad JWT. El conteo y resultado vigente se registra después de cada `./mvnw clean verify` en la bitácora. La cobertura de pagos incluye la validación de referencias seguras con separadores y variantes de CVV, el timeout interrumpible, la conciliación sin doble cobro y la recuperación de intentos `PENDING` huérfanos.

Los cinco recorridos E2E de ORD-05/QA-04 se ejecutan con:

```bash
./mvnw -Dtest=OrderJourneysE2ETest test
```

## Verificación completa con un comando

Desde `FASE 2`:

```bash
./tests/smoke/verify-platform.sh
```

El script ejecuta Maven, valida Compose sin imprimir credenciales, construye los servicios, espera los tres health checks, comprueba Actuator y correlation ID, y finalmente ejecuta `docker compose down` sin `-v`. Si Maven ya se ejecutó previamente:

```bash
./tests/smoke/verify-platform.sh --skip-maven
```

## Ejecución con Docker Compose

Desde `FASE 2`:

```bash
docker compose config
docker compose up --build -d
docker compose ps
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/api/v1/platform/status
docker compose down
```

Para empezar con datos de operación limpios en todos los sistemas (conserva catálogos semilla, ubicaciones y cuentas):

```bash
./scripts/reset-demo-data.sh --yes
```

No use `docker compose down -v` salvo que se quiera eliminar deliberadamente los datos locales. La API queda en `8080`, el frontend (UI-01) en `8081` (`http://localhost:8081` = landing, `/cliente.html` = tienda, `/staff.html` = consola operativa), RabbitMQ Management en `15672` y PostgreSQL en `5432` por defecto. PostgreSQL se expone solo para comodidad local; en un ambiente real debe permanecer interno.

## Sistemas propios de Fase 2 (Kevin Girón, ADR-015)

| Sistema | URL local | Cuenta | Qué hace | Pruebas |
|---|---|---|---|---|
| Tienda web | `http://localhost:8081` | Cliente: regístrese en `/registro.html` (o `cliente@megastore.com` / `Cliente123`) | Catálogo publicado, carrito omnicanal, checkout con PayKlik, retiro en tienda, Mis pedidos | — |
| Consola interna | `http://localhost:8081/staff.html` | `tienda-online@megastore.com` / `TiendaOnline123` (también `finanzas@`, `logistica@`, `staff@`) | Órdenes, despacho, ERP/POS, informe de ventas, call center, auditoría | — |
| Almacén central | `http://localhost:8082` | `almacen@almacencentral.local` / `Almacen123` (`finanzas@` y `logistica@` con `Finanzas123` y `Logistica123`) | Artículos, existencias, folios, stock por tienda, eventos de stock, vista general | `inventory-service`: `./mvnw clean verify` (40) |
| Tienda física POS-01 | `http://localhost:8084` | Solo el nombre del vendedor | Venta CF/NIT, recepción por folio, traslados, pedidos web y entrega al cliente, bitácora | `store-service` (15) |
| LogiExpress | `http://localhost:8085` | Solo el nombre del mensajero | Guías, recolección y entrega, rastreo, estado del servicio | `logiexpress-service` (4) |
| PayKlik | `http://localhost:8086` | Solo el nombre del operador | Transacciones y estado del servicio (normal, declina, lento, fuera de servicio) | `payklik-service` (4) |

Los portales de PayKlik y LogiExpress permiten provocar fallas reales del
proveedor desde "Estado del servicio" para demostrar reintentos y conciliación
(EaC-04/EaC-05). Las pruebas de cada sistema se ejecutan con
`./mvnw clean verify` dentro de su carpeta (requieren Docker por
Testcontainers).

Flujo de demostración sugerido:
1. En el central, crear un artículo, darle existencia y sacar folios a POS-ONLINE y POS-01.
2. Recibirlos en la tienda online y en la tienda física, y publicar el artículo.
3. Comprar en la web con retiro en Tienda Centro.
4. En LogiExpress, "Recolectar" y luego "Dejar en tienda".
5. En la tienda, "Entregar al cliente".
6. En la consola, "Generar lote ERP ahora" e "Informe de ventas".

## Ejecución desde IntelliJ IDEA

1. Abra `FASE 2/backend/pom.xml` como proyecto Maven.
2. Seleccione un JDK 25.
3. Levante dependencias desde `FASE 2` con `docker compose up -d postgres rabbitmq`.
4. Defina `SPRING_PROFILES_ACTIVE=local` y, si cambió valores, las variables correspondientes.
5. Ejecute `com.megastore.sgologic.SgoLogicApplication`.

## Ejecución mediante Maven

Con PostgreSQL y RabbitMQ disponibles:

```bash
cd "FASE 2/backend"
./mvnw spring-boot:run
```

## Imagen Docker

Desde `FASE 2`:

```bash
docker build -t sgo-logic-backend:0.0.1-phase2 backend
```

El Dockerfile compila mediante el wrapper, usa etapas separadas, ejecuta con usuario no root y comprueba readiness mediante Actuator.

## Endpoints técnicos

- `GET /api/v1/platform/status`: nombre, estado, timestamp UTC y correlation ID.
- `GET /actuator/health`: salud agregada.
- `GET /actuator/health/liveness`: probe de vida.
- `GET /actuator/health/readiness`: probe de disponibilidad.
- `GET /actuator/info`: información pública mínima.

Actuator expone únicamente `health` e `info`.

## Endpoints de negocio (Kevin Girón)

- `POST /api/v1/shipments` (`Idempotency-Key` requerido), `GET /api/v1/shipments/{id}`, `GET /api/v1/shipments?orderId=`, `GET/POST /api/v1/shipments/{id}/tracking-events`, `PATCH /api/v1/shipments/{id}/delivery-method` — LOG-01/02/03.
- `POST /api/v1/notifications` (responde `202`), `GET /api/v1/notifications/{id}`, `GET /api/v1/notifications?sourceAggregateId=` — NOT-01.
- `GET /api/v1/orders/{orderId}/summary` — QRY-01. Orders alimenta automáticamente esta proyección mediante RabbitMQ e Inbox idempotente; `POST /api/v1/query/order-summary` queda como ingesta temporal para pagos, despacho, entrega e incidencias.
- `POST /api/v1/incidents`, `GET /api/v1/incidents/{id}`, `GET /api/v1/incidents?orderId=`, `PATCH /api/v1/incidents/{id}/resolution` — INC-01.

## Endpoints de negocio (Jorge Pumay)

- `GET /api/v1/inventory/locations` — INV-01, las 34 ubicaciones (2 centros de distribución + 32 tiendas).
- `GET /api/v1/inventory/stock?productId=&locationId=` (`locationId` opcional) — INV-01, disponibilidad por producto y ubicación (`quantityAvailable = quantityOnHand - quantityReserved`).
- `POST /api/v1/inventory/reservations` (`Idempotency-Key` requerido), `GET /api/v1/inventory/reservations/{id}` — INV-02, reserva atómica e idempotente (CDU-02). `locationId` es opcional en el cuerpo: si se omite, INV-04/ADR-014 elige automáticamente la mejor ubicación con stock (centros de distribución antes que tiendas, luego por código). La respuesta incluye `locationType`/`locationCode` de la ubicación elegida, para que un consumidor (p. ej. Logistics/LOG-01) no necesite una segunda llamada a `/locations` para mapear el origen. `409 INSUFFICIENT_STOCK` cuando no hay disponibilidad en la ubicación indicada (incluye el caso de dos solicitudes compitiendo por la última unidad); `409 NO_AVAILABLE_ORIGIN` cuando ninguna ubicación candidata tiene stock; `404 INVENTORY_ITEM_NOT_FOUND` cuando el producto no tiene stock registrado en la ubicación indicada.
- Datos semilla de demostración: tres `productId` fijos documentados en `V2__inventory_seed_locations_and_stock.sql` (SGO-Logic no gestiona catálogo de productos, ver DDA §2.2).

Todos usan el formato común de error de `shared.http.ApiError` y propagan `X-Correlation-Id`.

## Endpoints de carrito y órdenes (Jairo Gómez)

- `POST /api/v1/carts` (`Idempotency-Key` requerido) abre o recupera el carrito activo del cliente desde `WEB`, `MOBILE`, `STORE` o `CALL_CENTER`.
- `GET /api/v1/carts/{cartId}` y `GET /api/v1/carts/active?customerId=` consultan el carrito omnicanal.
- `PUT /api/v1/carts/{cartId}/items/{productId}` agrega o reemplaza cantidad y `DELETE` en la misma ruta elimina el artículo.
- `POST /api/v1/carts/{cartId}/checkout` (`Idempotency-Key` requerido) crea una única orden a partir de la fotografía inmutable del carrito.
- `GET /api/v1/orders/{orderId}` y `GET /api/v1/orders?customerId=` consultan órdenes.
- `POST /api/v1/orders/{orderId}/inventory-reservations` reserva atómicamente todas las líneas antes de avanzar la Saga.
- `POST /api/v1/orders/{orderId}/payment-attempts` persiste e invoca PayKlik con una referencia tokenizada; timeout o caída conserva la orden como pago indeterminado y agenda conciliación.
- `GET /api/v1/payment-attempts/{id}` y `GET /api/v1/payment-attempts?orderId=` exponen la evidencia del intento sin PAN ni CVV.
- `POST /api/v1/orders/{orderId}/fiscal-documents` inicia la compuerta SAT y `POST /api/v1/orders/{orderId}/fiscal-synchronization` confirma la orden únicamente si SAT aprobó el documento.
- `POST /api/v1/orders/{orderId}/saga` (`Idempotency-Key` requerido) inicia o reanuda reserva, pago, SAT, consumo de inventario y despacho automático.
- `POST /api/v1/orders/{orderId}/cancel` compensa una orden todavía no cobrada y libera sus reservas activas de forma idempotente.
- `PATCH /api/v1/orders/{orderId}/status` aplica la máquina de estados; cualquier salto que omita reserva, pago o aprobación fiscal devuelve `409` y conserva evidencia en el historial propietario de Orders.

ORD-01 a ORD-05 están implementados mediante contratos públicos entre módulos, sin leer repositorios ni tablas ajenas. La confirmación fiscal materializa el consumo de las reservas y crea el despacho desde el origen asignado por Inventory.

## Outbox y auditoría transversal

- Cada transición de orden persiste `order_status_history`, `outbox_event` y `audit_events` dentro de la misma transacción local.
- El publicador consulta mensajes pendientes, publica en el exchange durable `sgo.events`, marca la entrega y aplica backoff hasta el estado fallido y la DLQ configurada.
- `GET /api/v1/audit/events?orderId=` o `?transactionId=` recupera evidencia para los roles `FINANCE` y `SAT_AUDITOR` usando los encabezados provisionales `X-Actor-Id` y `X-Actor-Role`.
- `GET /api/v1/audit/exports?orderId=` descarga la evidencia y registra el acceso/exportación.
- Cada evidencia incluye actor, acción, estado anterior/nuevo, resultado, correlación y hash SHA-256.

Los encabezados de actor son un contrato provisional hasta implementar autenticación/autorización en `security`; no deben presentarse como seguridad productiva.

## Endpoints fiscal e integraciones (Andy Sánchez)

- `POST /api/v1/fiscal-documents` (`Idempotency-Key` requerido, responde `202`), `GET /api/v1/fiscal-documents/{id}`, `GET /api/v1/fiscal-documents/orders/{orderId}` y `GET /api/v1/fiscal-documents/orders/{orderId}/confirmation` — FIS-01/FIS-02.
- `POST /api/v1/integrations/erp/sales`, `POST /api/v1/integrations/erp/batches` y `GET /api/v1/integrations/erp/batches/{id}` — ERP-01/ERP-02. La ventana programada es de 30 minutos por defecto y no envía archivos vacíos.
- `POST /api/v1/integrations/pos/events`, `GET /api/v1/integrations/pos/deliveries/{id}` y `GET /api/v1/integrations/pos/events/{eventId}` — POS-01, tiendas 1–32, acuse y deduplicación por evento+tienda.
- `POST /api/v1/simulators/payklik/authorizations` — contrato simulado BB-06 para PayKlik; solo acepta referencias seguras, nunca PAN/CVV.

Fiscal, ERP y POS persisten estado, correlación, intentos y resultados. Los fallos transitorios usan reintentos 2/8/32 s; la entrega durable después de un reinicio se conectará al Outbox de BB-04 cuando Edson lo entregue.

Los simuladores de PayKlik, SAT, ERP, POS, LogiExpress y notificaciones admiten `SUCCESS`, `REJECTION`, `LATENCY`, `TIMEOUT`, `OUTAGE` y `RANDOM` (ADR-012). Desde Fase 2, en Compose, **PayKlik y LogiExpress son sistemas propios por REST**, la tienda 1 recibe sus eventos en `store-service`, el lote ERP se escribe como archivo en `/var/sgo/erp-outbox` y el correo sale por SMTP si hay credenciales. Los simuladores quedan para SAT, SMS/push, los POS 2–32 y las pruebas automáticas.

Endpoints agregados en Fase 2 (ADR-015):
- ERP: `GET /api/v1/integrations/erp/batches`, `GET /batches/{id}/file`, `POST /batches/{id}/retry` y `GET /sales/{orderId}`.
- POS: `GET /api/v1/integrations/pos/deliveries?orderId=`.
- `GET /api/v1/reports/sales` y `/export` (FINANCE): informe de ventas conciliado y auditado.
- `POST /api/v1/shipments/{id}/label-retry` (reintentar guía) y `POST /api/v1/store-pickups/{orderId}/handoff` (rol STORE): entrega en la tienda de retiro.
- `GET /api/v1/invoices/orders/{orderId}`: factura del cliente.
- `/api/v1/online-store/**` (rol ONLINE_STORE) y `GET /api/v1/online-catalog`: tienda online y catálogo publicado.

## Outbox e Inbox (Edson Sosa, BB-04)

El mecanismo transversal de publicación confiable (ADR-002) ya está implementado en `integration.outbox` con migración `V3001`:

- Un módulo de negocio registra un evento con `PublishEvent.publish(...)` **dentro de su propia transacción**; la implementación exige transacción activa (`MANDATORY`) y el evento se confirma o revierte junto con el cambio de negocio.
- Un dispatcher programado publica los pendientes en el exchange topic `sgo.events` con el sobre de `docs/contracts/events/event-envelope-v1.md`; ante fallo aplica backoff 2/8/32 s y tras agotar los intentos el evento queda `FAILED` visible en la tabla.
- El lote se reclama con `FOR UPDATE SKIP LOCKED`, por lo que varios dispatchers no duplican publicaciones.
- Los consumidores deduplican reenvíos con `IdempotentConsumer.registerProcessed(...)` sobre `inbox_event` (`UNIQUE (consumer, event_id)`).
- Topología declarada: exchange `sgo.events`, dead-letter exchange `sgo.events.dlx` y cola `sgo.events.dlq`.

Ningún flujo de negocio consume todavía el Outbox: la conexión de la Saga (ORD-03, Jairo) y de los trabajadores de cada módulo es el siguiente paso de integración.

## Autenticación, roles y sanitización (Edson Sosa, BB-07)

La API está protegida con JWT HMAC-SHA256 según ADR-013:

- `POST /api/v1/auth/login` recibe `{"username","password"}` y devuelve `accessToken`, `tokenType`, `expiresAt` y `roles`. Es el único endpoint de negocio público, junto con Actuator y el estado técnico de plataforma.
- El resto de endpoints requiere `Authorization: Bearer <token>`: sin token responden `401 UNAUTHENTICATED` y con rol insuficiente `403 FORBIDDEN`, ambos con `ApiError` y correlation ID.
- Autorización: `FINANCE` para fiscal/ERP/simuladores y para la consulta/exportación de auditoría; `CLIENT`/`SUPPORT` para pagos; `LOGISTICS` para crear despachos, guías y eventos POS, mientras que `GET /shipments/**` (seguimiento, CDU-09) también lo ven `CLIENT` y `SUPPORT`; `CLIENT`/`SUPPORT` leen y crean incidencias y solo `SUPPORT` las resuelve; `CLIENT`/`SUPPORT` leen notificaciones y solo `SUPPORT` las emite; `CLIENT`/`SUPPORT`/`LOGISTICS` para inventario; `SUPPORT`/`LOGISTICS` para la ingesta de query. Detalle en `docs/adr/ADR-013-autenticacion-jwt-y-roles.md`.
- Cuentas de demostración locales, configurables por `SGO_SECURITY_*`: `cliente@megastore.com` / `Cliente123` (`CLIENT`), `staff@megastore.com` / `Staff123` (`SUPPORT` + `LOGISTICS`, la consola interna completa), `logistica@megastore.com` / `Logistica123` (`LOGISTICS`), `finanzas@megastore.com` / `Finanzas123` (`FINANCE`).
- `SensitiveDataSanitizer` enmascara PAN (13–19 dígitos) y campos CVV antes de cualquier log; `RestSecurityErrorWriter` registra la ruta enmascarada de los 401/403.
- `sgo.security.enabled=false` permite que las pruebas de negocio corran sin token; `SecurityAuthorizationTest` lo habilita para validar login, 401, 403 y sanitización.
- SEC-01: `SensitiveDataLeakScanTest` genera actividad real, rechaza PAN/CVV con `400` y escanea base de datos, payloads del Outbox, auditoría y logs: 0 apariciones. Evidencia en `docs/evidence/security/SEC-01-scan-pan-cvv.md`.
- La UI (`frontend/`) usa el login real: guarda el JWT y lo envía en cada llamada.

## Auditoría (Edson Sosa, AUD-01/AUD-02)

El registro auditable transversal (CDU-12) está implementado y protegido con rol `FINANCE`:

- `GET /api/v1/audit/events?orderId={uuid}` o `?aggregateType={tipo}&aggregateId={uuid}` recupera la evidencia en orden cronológico.
- `GET /api/v1/audit/orders/{orderId}/export` descarga la evidencia en CSV (`Content-Disposition: attachment`).
- Cada acceso y cada exportación quedan registrados como evidencia (`AUDIT_EVIDENCE_ACCESSED`, `AUDIT_EVIDENCE_EXPORTED`); una búsqueda sin resultados responde `404 AUDIT_EVIDENCE_NOT_FOUND`, registra `AUDIT_SEARCH_FAILED` y deja una alerta para Finanzas sin inventar información (CDU-12 E1).
- Medida EaC-02: `AuditQueryIntegrationTest` verifica < 1 s sobre el conjunto declarado de 10 000 eventos (1 000 de una misma orden); en el smoke real la consulta respondió en ~63 ms.
- `AuditRecorder` (AUD-01) escribe la evidencia en la misma transacción del cambio de negocio; `incidents` ya lo usa.

## Pagos (Edson Sosa, PAY-01 a PAY-03)

El módulo `payments` materializa la capacidad "autorización de pago" de `docs/contracts/orders/saga-required-ports.md`:

- `POST /api/v1/payments/attempts` con `Idempotency-Key` registra el intento con orden, canal (`WEB`/`MOBILE`/`STORE`/`CONTACT_CENTER`), método (`CARD`/`TRANSFER`/`DIGITAL_WALLET`), importe, moneda y referencia segura. Responde `201` con el resultado (aprobado, rechazado o indeterminado) y `200` si la llave ya existía.
- `GET /api/v1/payments/attempts/{id}` y `GET /api/v1/payments/attempts?orderId=` consultan los intentos; solo `CLIENT` y `SUPPORT`.
- Rechaza referencias inseguras antes de llamar a PayKlik (`400 INVALID_PAYMENT_REQUEST`): PAN de 13 o más dígitos incluso con separadores (`4111-1111-1111-1111`, `4111 1111 1111 1111`, `4111.1111.1111.1111`) y variantes CVV/CVC (`cvv`, `cvv2`, `cvc`, `cvc2`, `securityCode`). No almacena PAN/CVV.
- PAY-02: el adaptador está envuelto con límite de tiempo y circuit breaker (Resilience4j). Un timeout, una caída o el circuito abierto dejan el intento `INDETERMINATE` (nunca aprobado), conservan la orden y publican `PaymentIndeterminate` por Outbox en la misma transacción. Al expirar el límite, la llamada al proveedor se interrumpe y el pool de llamadas está acotado (`SGO_PAYKLIK_CALL_POOL_SIZE`).
- Una segunda solicitud simultánea con la misma `Idempotency-Key` puede recibir el intento en `PENDING` mientras la primera sigue en vuelo; la conciliación lo resuelve y la Saga debe consultar o esperar el resultado.
- PAY-03: al quedar indeterminado se programa una conciliación dentro de los 60 s (`SGO_PAYMENT_RECONCILIATION_FIRST_DELAY`, por defecto 30 s). Reutiliza la misma `Idempotency-Key` y la referencia segura del intento, por lo que **nunca genera un segundo cobro**; reintenta con backoff y, si sigue sin resolver tras 5 min o agota los intentos, queda `DEAD_LETTER` con alerta auditable (`PAYMENT_RECONCILIATION_ALERT`). Al resolver publica `PaymentApproved`/`PaymentRejected`.
- Un intento que quedó `PENDING` más allá de `SGO_PAYMENT_RECONCILIATION_STALE_PENDING_AFTER` (caída del proceso o excepción entre la creación y el resultado) se recupera como `INDETERMINATE` con código `STALE_PENDING` y entra al circuito de conciliación.
- Cada intento y su resultado dejan evidencia auditable (`PAYMENT_ATTEMPT_CREATED`, `PAYMENT_APPROVED`, `PAYMENT_REJECTED`, `PAYMENT_INDETERMINATE`, `PAYMENT_RECONCILIATION_ALERT`).
- `payments.api.PaymentPort` es la API pública para ORD-03.

## Rendimiento y carga (Edson Sosa, DEC-02/QA-02)

La línea base `T` y la carga `8T` se midieron con k6 sobre el conjunto declarado de 500 proyecciones de orden y 10 000 eventos de auditoría (`tests/data/seed-load.sql`):

| Escenario | Endpoint | req/s | p50 | p95 | p99 | Errores |
|---|---|---:|---:|---:|---:|---:|
| `T` (10 VUs) | Consulta consolidada (EaC-01) | 19.75 | 3.35 ms | 5.88 ms | 7.10 ms | 0 % |
| `8T` (80 VUs) | Consulta consolidada | 158.86 | 2.07 ms | 4.22 ms | 8.30 ms | 0 % |
| `T` (10 VUs) | Evidencia (EaC-02) | 19.59 | 5.86 ms | 17.20 ms | 28.90 ms | 0 % |
| `8T` (80 VUs) | Evidencia | 157.47 | 3.02 ms | 7.23 ms | 22.56 ms | 0 % |

`8T` es ≈ 8.04× la tasa de `T` sin degradar los umbrales. Protocolo en `docs/qa/DEC-02-protocolo-carga-T-8T.md`, resultados y hallazgos en `docs/evidence/performance/QA-02-linea-base-y-8T.md` y comandos en `tests/k6/README.md`. La prueba encontró y corrigió un agotamiento del pool de conexiones en la consulta de auditoría y el consumo excesivo del healthcheck de RabbitMQ.

## Rendimiento de Fase 3 (Andy Sánchez)

El arnés de Fase 3 se encuentra en `tests/k6/phase3` y separa cinco escenarios
de carga de cinco escenarios de estrés. Incluye ejecución reproducible para
PowerShell/Bash, umbrales, rutas configurables y captura de resúmenes k6. El
reporte controlado está en
`docs/evidence/performance/FASE3-REPORTE-CARGA-ESTRES.md` y el Manual Técnico en
`docs/manuals/MANUAL_TECNICO_FASE_3.md`.

Los resultados permanecen marcados como pendientes hasta medir un SHA o release
que contenga el módulo de crédito, el checkout dual y el ambiente autorizado.
Los scripts no convierten rutas provisionales ni valores estimados en evidencia.

## Integración continua — QA-01

El workflow `.github/workflows/qa-integracion-base.yml` usa dos jobs: primero compila y prueba el backend; después construye Compose y ejecuta el smoke test. Se activa en pull requests, push a `develop`/`main` y manualmente. Puede utilizar estos secretos de GitHub Actions:

- `SGO_POSTGRES_DB`
- `SGO_POSTGRES_USER`
- `SGO_POSTGRES_PASSWORD`
- `SGO_RABBITMQ_USER`
- `SGO_RABBITMQ_PASSWORD`

Use valores exclusivos y no productivos para CI. Como no existe despliegue y los contenedores son efímeros, cada secreto tiene un valor ficticio de respaldo; un secreto ausente no detiene el pipeline. El workflow no imprime la configuración Compose interpolada. QA-01 permanece en validación hasta observar una ejecución verde en GitHub.

## Guías y propuestas para integración

- `docs/contracts/contract-status.md`: estado y aprobaciones de contratos.
- `docs/adr/ADR-010-confirmacion-fiscal-asincrona.md`: compuerta fiscal y política ante SAT.
- `docs/adr/ADR-012-contratos-de-simuladores.md`: modos y contratos BB-06.
- `docs/diagrams/modelo-datos.md`: modelo de datos del MVP con ERD, propiedad por esquema y mapeo a CDU/EaC/migraciones.
- `docs/qa/trazabilidad-cun-mvp.md`: trazabilidad CUN/CDU → código → datos → despliegue → EaC/ADR y elementos técnicos justificados.
- `docs/evidence/functional/REV-EDSON-2026-09-23-revision-cruzada.md`: revisión cruzada de fiscal/ERP/POS e inventario con evidencia `clean verify`.
- `docs/contracts/compatibility-policy.md`: reglas para cambios compatibles.
- `docs/contracts/orders/ord-01-cart-proposal.md`: casos preparatorios del carrito.
- `docs/contracts/orders/ord-02-state-machine-proposal.md`: matriz candidata de estados.
- `docs/contracts/orders/saga-required-ports.md`: capacidades requeridas de otros módulos.
- `docs/diagrams/saga-happy-path-proposed.md`: secuencia nominal sin implementación.
- `docs/integration/module-delivery-checklist.md`: control de entregas por integrante.
- `docs/integration/integration-order.md`: orden basado en dependencias.
- `docs/integration/release-readiness.md`: criterios previos a REL-01.
- `docs/integration/qa-01-ci-contract.md`: comportamiento y secretos del workflow.
- `docs/evidence/functional/VALIDACION_CRUZADA_BB01_BB05.md`: procedimiento y evidencia pendiente para una persona no autora.

Las propuestas de diseño siguen sujetas a ratificación del equipo, pero Orders ya posee entidades, transiciones ejecutables y contratos con Inventory, Payments y Fiscal. Logística, notificaciones, ERP, POS, consulta e incidencias tienen implementaciones locales; falta enlazar automáticamente el tramo posterior a `CONFIRMED` y las compensaciones completas.

## Alcance de este incremento

Se prepararon la estructura oficial, backend Spring Boot, módulos, configuración local/test, correlación HTTP, errores comunes, endpoint técnico, Dockerfile, Compose, documentación, contratos preliminares y pruebas iniciales.

Kevin Girón agregó, sobre esa base: `logistics` (LOG-01/02/03, con integración simulada de LogiExpress y reintentos 2/8/32 s), `notifications` (NOT-01, despacho no bloqueante), `query` (QRY-01, vista consolidada con indicador de dato desactualizado) e `incidents` (INC-01, devoluciones/incidencias), cada uno con su propia migración Flyway y pruebas. El frontend de cliente fue integrado al flujo real de carrito, reserva, pago y SAT, con identidad azul/cian compartida por portada, login y tienda.

En la segunda parte de Fase 2 (2026-10-01 al 2026-10-13, ADR-015) se agregaron:
- Los sistemas propios `inventory-service`, `store-service`, `payklik-service` y `logiexpress-service`.
- Los módulos `onlinestore`, `inventorysync`, `customers`, `legacyfeed` (ERP y POS automáticos), `salesreport` (informe de ventas) y `fulfillment`.
- Los adaptadores REST reales de PayKlik y LogiExpress, el retiro en tienda, los eventos de stock a los POS y el correo real.

Andy Sánchez agregó `fiscal` (XML, CUI/serie, estado `FISCAL_PENDING`, SAT asíncrono, idempotencia y compuerta de confirmación), lotes ERP CSV/XML cada 30 minutos, distribución POS a 32 tiendas y los simuladores configurables BB-06, además de las migraciones `V2001`–`V2003`, ADR-010/ADR-012 y pruebas de FIS-03.

Edson Sosa agregó el Outbox/Inbox transversal (BB-04: `V3001`, `PublishEvent`, dispatcher con reintentos y DLQ, `IdempotentConsumer`) y la seguridad del MVP (BB-07/ADR-013: login JWT, autorización por roles, respuestas 401/403 con `ApiError` y sanitización de PAN/CVV).

Jairo Gómez agregó el catálogo público, carrito recuperable, órdenes y estados ejecutables, el recorrido hasta confirmación fiscal y consumidores de eventos para Query y Notifications. Continúan pendientes el despacho automático posterior a `CONFIRMED`, las compensaciones completas de Saga, productores para las demás proyecciones y la ejecución remota verde de QA-01.

BB-02 permanece parcial porque ADR-009 está `Proposed`. BB-05 también permanece parcial hasta integrar frontend y componentes compartidos pendientes. Los contratos de estados, eventos y API son propuestas sujetas a aprobación del equipo.

## Limitaciones actuales

- El endpoint de plataforma no representa funcionalidad de negocio ni sustituye Actuator.
- El camino completo funciona sin intervención: carrito, reserva, pago (con conciliación), SAT, confirmación, despacho con LogiExpress, rastreo y entrega (a domicilio o en la tienda de retiro). Las compensaciones cubren pago rechazado y cancelación antes del cobro.
- El Outbox transversal está operativo y Orders publica sus transiciones; Query y Notifications las consumen con Inbox idempotente. Fiscal, ERP y POS todavía deben completar su participación en el flujo transversal.
- La autenticación JWT y la autorización por roles están implementadas (BB-07/ADR-013); las cuentas son de demostración local y no hay refresh token, revocación ni límite de intentos.
- `audit` registra evidencia transversal atómica (AUD-01, `V3002`) y expone consulta indexada y exportación (AUD-02); `incidents` y `payments` son sus consumidores. Fiscal y logística aún deben sumar sus acciones al registro para completar la reconstrucción de CDU-12.
- `payments` registra intentos idempotentes con orden, canal, fecha, referencia y resultado, aplica timeout y circuit breaker (PAY-01/PAY-02) y concilia los indeterminados sin segundo cobro (PAY-03); la alerta operativa es un log y un registro auditable, sin canal externo de notificaciones.
- El escaneo de PAN/CVV de SEC-01 cubre la base de pruebas, los payloads persistidos del Outbox y los logs del proceso; no inspecciona colas en vuelo ni un agregador externo de logs.
- Cada consulta o exportación de auditoría agrega un evento `AuditEvidence`; no hay política de retención o purga en el MVP, así que la tabla crece con el uso (≈ 9 500 filas/min en la prueba `8T`).
- El entorno Compose es local y no define redundancia, capacidad, RTO, RPO ni topología productiva.
- Las mediciones de QA-02 son de una sola instancia y host de desarrollo; EaC-03 queda parcial hasta contar con Orders/Saga (ORD-01 a ORD-05) y un ambiente con balanceo.
- La planificación menciona JUnit 5, pero el BOM de Spring Boot 4.1.1 resuelve JUnit Jupiter 6.0.3. No se sobrescribió manualmente el BOM; el equipo debe ratificar esta diferencia o cambiar de versión de plataforma mediante ADR.
- En Compose, `logistics` usa LogiExpress real (`logiexpress-service`) y `notifications` envía correo real por SMTP si `SGO_MAIL_*` tiene credenciales; SMS y push siguen simulados.
- `logistics` (`shipment_dead_letters`) conserva una cola de fallidos local; todavía no está conectado al Outbox transversal ya disponible para eventos de orden.
- `query` consume automáticamente los eventos de orden, pago, despacho e incidencia con Inbox idempotente; `POST /api/v1/query/order-summary` queda solo como ingesta manual de respaldo.
- Los portales de `store-service`, `payklik-service` y `logiexpress-service` no piden contraseña al operador: están pensados para la red interna (decisión de alcance, ADR-015). SAT, SMS/push y los POS de las tiendas 2–32 siguen simulados.
- `shared` se declaró `@ApplicationModule(type = OPEN)` y `ModularArchitectureTest` se amplió para permitir que un módulo de negocio dependa de `shared`; ambos cambios tocan código compartido y están pendientes de revisión por Jairo Gómez (arquitecto integrador).
- Las migraciones `V3001`-`V3002` reservan el rango posterior a las entregas existentes y fueron validadas sobre una base persistente que ya estaba en `V2003`.
- Las 14 migraciones se ejecutaron correctamente contra PostgreSQL 17 en Docker durante la suite y el recorrido HTTP del MVP.
- Los ejecutores asíncronos existentes de cada módulo conservan su estado y reintentan dentro del proceso. El Outbox ya recupera eventos de orden después de reinicios; los demás agregados todavía deben adoptarlo.
