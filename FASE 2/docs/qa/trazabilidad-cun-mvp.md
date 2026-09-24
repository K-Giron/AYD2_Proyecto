# Trazabilidad CUN/CDU → código → datos → despliegue (MVP Fase 2)

**Responsable de esta revisión:** Edson Sosa
**Fecha de corte:** 2026-09-23
**Fuentes:** `Docs/DDA.md` §3 (CDU expandidos), §4 (EaC), §5 (ADR), §6 (vistas),
`Docs/diagramas/despliegue.dot`, `docs/diagrams/modelo-datos.md`,
`docs/qa/QA-03-matriz-eac.md` y el código de `backend/`.

## 1. Propósito

La tabla de penalización por omisión de trazabilidad exige que cada capacidad
del MVP aparezca en **cuatro artefactos**: CDU expandido (CUN), modelo de
datos, diagrama de despliegue y código. Esta matriz verifica los cuatro para
los doce CUN y marca con honestidad lo que todavía no existe, para evitar
código huérfano (funcionalidad sin sustento documental) y para que cada
tarjeta pendiente nazca ya trazada.

Estados usados:

- **Implementado:** código, tablas y pruebas presentes en `develop`.
- **Parcial:** existe una parte y el resto está en una tarjeta identificada.
- **Planificado:** la tarjeta existe en la planificación y este documento fija
  su trazabilidad antes de programar.
- **Fuera del MVP:** explícitamente excluido por el DDA o por la planificación.

## 2. Matriz principal

| CUN / CDU | Estado | Código (módulo, clases, endpoints) | Modelo de datos | Despliegue | EaC | ADR | Pruebas |
|---|---|---|---|---|---|---|---|
| CUN-01 Carrito omnicanal / CDU-01 | **Planificado** (ORD-01, Jairo) | `orders` (no existe); frontend `cliente.html` es vitrina sin backend | `carts`, `cart_items` (planificado, §4.1 modelo) | SGO-Logic + Base operacional | EaC-01, EaC-09, EaC-11 | ADR-001, ADR-005 | Pendiente |
| CUN-02 Reservar inventario / CDU-02 | **Implementado** (INV-01 a INV-05, Jorge) | `inventory`: `InventoryController` (`GET /inventory/locations`, `GET /inventory/stock`, `POST /inventory/reservations`, `GET /inventory/reservations/{id}`), `InventoryReservationService`, `InventoryExpirationService`, `InventoryQueryService` | `inventory_locations`, `inventory_items`, `inventory_reservations` (`V1`, `V2`) | Base operacional + SGO-Logic + Registro de auditoría (pendiente AUD-01) | EaC-03, EaC-07 | ADR-001, ADR-005, ADR-011, ADR-014 | `InventoryReservationServiceTest` (9), `InventoryConcurrencyIntegrationTest` (2), `InventoryExpirationServiceTest` (3), `InventoryQueryServiceTest` (5) |
| CUN-03 Procesar pago / CDU-03 | **Parcial** (BB-06 y PAY-01 a PAY-03 hechos; el cambio de estado de la orden depende de ORD-03, Edson/Jairo) | `payments`: `PayKlikGatewayPort`, `SimulatedPayKlikAdapter`, `ResilientPayKlikClient` (timeout + CB), `PaymentAttemptService`, `PaymentReconciliationService`, `PaymentController`, `payments.api.PaymentPort` | `payment_attempts` y `payment_reconciliations` (**implementados**, `V3003`/`V3004`, modelo §3.8) | Adaptadores de salida (timeout + Circuit Breaker), Outbox, Broker, Base operacional | EaC-04, EaC-07, EaC-12 | ADR-003, ADR-005, ADR-012 | `PaymentAttemptServiceTest` (6), `PaymentIntegrationTest` (4), `ResilientPayKlikClientTest` (3), `PaymentIndeterminateIntegrationTest` (1), `PaymentReconciliationServiceTest` (3), `PaymentReconciliationIntegrationTest` (1) y `SimulatedPayKlikAdapterTest` (3) |
| CUN-04 Documentación fiscal / CDU-04 | **Implementado** (FIS-01/FIS-02, Andy) | `fiscal`: `FiscalController` (`POST/GET /fiscal-documents`, `GET .../orders/{orderId}`, `GET .../confirmation`), `FiscalService`, `FiscalXmlGenerator`, `SatGatewayPort`, `SimulatedSatGatewayAdapter` | `fiscal_documents` (`V2001`) | Adaptadores de salida → SAT, estado fiscal pendiente, Registro de auditoría | EaC-02, EaC-07 | ADR-002, ADR-007, ADR-010 | `FiscalServiceTest` (6) |
| CUN-05 Sincronizar ERP / CDU-05 | **Implementado** (ERP-01/ERP-02, Andy) | `integration`: `ErpIntegrationService`, `ErpIntegrationController` (`POST /integrations/erp/sales`, `POST/GET .../batches`), `SimulatedErpGatewayAdapter` | `erp_batches`, `erp_sales` (`V2002`) | Trabajadores de integración, Área de intercambio ERP, Broker/DLQ | EaC-07, EaC-11 | ADR-002, ADR-004 | `ErpIntegrationServiceTest` (4) |
| CUN-06 Eventos a POS / CDU-06 | **Parcial** (POS-01, Andy; fan-out y consumo del Outbox pendientes) | `integration`: `PosIntegrationService`, `PosIntegrationController` (`POST /integrations/pos/events`, `GET .../deliveries/{id}`, `GET .../events/{eventId}`), `SimulatedPosGatewayAdapter` | `pos_deliveries` (`V2003`) | Trabajadores → POS, Broker, Mensajes fallidos/DLQ | EaC-06, EaC-07 | ADR-002, ADR-004, ADR-012 | `PosIntegrationServiceTest` (3) |
| CUN-07 Gestionar despacho / CDU-07 | **Implementado** (LOG-01/02/03, Kevin) | `logistics`: `ShipmentController` (`POST/GET /shipments`, `PATCH .../delivery-method`), `ShipmentService`, `ShipmentDeadLetterRepository` | `shipments`, `shipment_tracking_events`, `shipment_dead_letters` (`V1001`) | Trabajadores, Broker, DLQ, Registro de auditoría | EaC-05, EaC-06, EaC-07 | ADR-004, ADR-005, ADR-006 | `ShipmentServiceTest` (5) |
| CUN-08 Integrar LogiExpress / CDU-08 | **Implementado** (LOG-02/LOG-03, Kevin) | `logistics`: `LogiExpressGatewayPort`, `SimulatedLogiExpressAdapter`, reintentos 2/8/32 s en `ShipmentService` | `shipments.tracking_number`, `shipment_tracking_events`, `shipment_dead_letters` (`V1001`) | Adaptadores de salida (retry/backoff), Broker, DLQ | EaC-05 | ADR-004, ADR-012 | `ShipmentServiceTest` (5) |
| CUN-09 Consultar orden / CDU-09 | **Implementado** (QRY-01, Kevin) | `query`: `OrderSummaryController` (`GET /orders/{orderId}/summary`), `OrderSummaryQueryService`; ingesta temporal `POST /query/order-summary` | `order_summary_projections` (`V1003`) | SGO-Logic, Base operacional, Outbox/Broker (alimentación real pendiente BB-04) | EaC-01, EaC-07 | ADR-005, ADR-006 | `OrderSummaryQueryServiceTest` (3) |
| CUN-10 Devoluciones y reembolsos / CDU-10 | **Parcial** (INC-01, Kevin; falta reembolso, reintegro de inventario y nota de crédito integrada) | `incidents`: `IncidentController` (`POST/GET /incidents`, `PATCH .../resolution`), `IncidentService`; `fiscal` ya soporta `CREDIT_NOTE` (CDU-04 A1) | `incidents`, `incident_items` (`V1004`); `fiscal_documents` (`V2001`) | SGO-Logic, Base operacional, Adaptadores → SAT | EaC-02, EaC-07 | ADR-002, ADR-005, ADR-007 | `IncidentServiceTest` (2) |
| CUN-11 Notificar cliente / CDU-11 | **Implementado** (NOT-01, Kevin) | `notifications`: `NotificationController` (`POST /notifications` → 202, `GET ...`), `NotificationService`, `NotificationProviderPort`, `SimulatedNotificationProviderAdapter` | `notification_requests` (`V1002`) | Trabajadores, Broker, cola de notificaciones | EaC-08, EaC-07 | ADR-002, ADR-008 | `NotificationServiceTest` (2) |
| CUN-12 Auditar operaciones / CDU-12 | **Parcial** (AUD-01/AUD-02 implementados; faltan más productores, Edson) | `audit`: `AuditService`, `AuditRecorder`, `AuditQueryService`, `AuditController`; `incidents` ya registra evidencia | `audit_events` (`V3002`, modelo §3.7) | Registro y consulta indexada implementados; pagos, fiscal y logística aún no suman sus acciones | EaC-02, EaC-07 | ADR-002, ADR-006, ADR-007 | `AuditIntegrationTest` (4), `AuditQueryIntegrationTest` (5) e `IncidentServiceTest` (2) |

## 3. Trazabilidad de las tarjetas pendientes de Edson

Esta sección existe para que ninguna de las tarjetas en curso se implemente sin
respaldo documental previo.

| Tarjeta | CUN/CDU que la respalda | Artefacto de datos | Elemento de despliegue | EaC | ADR/contrato |
|---|---|---|---|---|---|
| BB-04 Outbox/Inbox/retry/DLQ | CDU-06 precondición explícita; CDU-03 E1; CDU-04, CDU-05, CDU-11 (entrega asíncrona) | `outbox_event`, `inbox_event` (**implementado**, `V3001`, modelo §3.6) | Outbox transaccional, Broker, Mensajes fallidos/DLQ | EaC-04, EaC-05, EaC-06, EaC-07 | ADR-002, ADR-004; `event-envelope-v1` (`PROPOSED`) |
| BB-07 Seguridad, roles y sanitización | CDU-12 precondición "solicitante autorizado"; CDU-03 paso 3 (sin PAN/CVV) | Usuarios demo en memoria (**implementado**, sin tablas); `audit_events.actor` al llegar AUD-01 | Entrada segura + SGO-Logic | EaC-12 | **ADR-013** (`Proposed`), DEC-06, R4 |
| PAY-01 Intentos de pago | CDU-03 pasos 2 y 3, postcondición de registro auditable | `payment_attempts` (**implementado**, `V3003`, modelo §3.8) | Base operacional, Adaptadores de salida | EaC-12 | ADR-003 |
| PAY-02 Timeout/Circuit Breaker/indeterminado | CDU-03 E1 y postcondición "Pago pendiente de confirmación" | `payment_attempts.status = INDETERMINATE` (**implementado**, `V3003`) + evento `PaymentIndeterminate` | Adaptadores de salida (timeout + CB), Outbox | EaC-04 | ADR-003, ADR-010 como precedente |
| PAY-03 Conciliación idempotente ≤ 60 s | CDU-03 E1 ("antes de reintentar debe consultar o conciliar") | `payment_reconciliations` (**implementado**, `V3004`, modelo §3.8) | Broker, cola de conciliación/reintentos | EaC-04 | ADR-003 |
| AUD-01 Registro auditable | CDU-12 pasos 2 y 3; postcondición de toda operación relevante | `audit_events` (**implementado**, `V3002`, modelo §3.7) | Registro y lectura de auditoría | EaC-02, EaC-07 | ADR-002, ADR-006, ADR-007 |
| AUD-02 Consulta indexada < 1 s | CDU-12 paso 5 y 6 (registrar acceso); A1 exportación; E1 búsqueda fallida | `audit_events` con índices por orden, agregado y correlación (**implementado**, `V3002`) | Registro y lectura de auditoría | EaC-02 | ADR-006 |
| SEC-01 Pruebas 401/403 y fuga | CDU-12 precondición; CDU-03 paso 3 | `audit_events`, `payment_attempts`, `outbox_event` (**escaneados**, evidencia en `docs/evidence/security/SEC-01-scan-pan-cvv.md`) | Entrada segura, SGO-Logic | EaC-12 | ADR-013 |
| QA-02 Línea base `T` y carga `8T` | EaC-01, EaC-02, EaC-03 | 500 proyecciones y 10 000 eventos (**implementado**, `tests/data/seed-load.sql`) | SGO-Logic (una instancia), Broker | EaC-01, EaC-02, EaC-03 | ADR-001, ADR-006; **DEC-02 cerrado** con `docs/qa/DEC-02-protocolo-carga-T-8T.md` y resultados en `docs/evidence/performance/QA-02-linea-base-y-8T.md` |

## 4. Elementos técnicos sin CUN de negocio (justificación)

No son código huérfano: cumplen una función técnica y están respaldados por un
artefacto, pero no representan una capacidad de negocio del DDA.

| Elemento | Respaldo documental | Motivo |
|---|---|---|
| `GET /api/v1/platform/status` | BB-01; ADR-009 | Endpoint técnico de verificación; no sustituye Actuator ni es funcionalidad de negocio |
| `POST /api/v1/simulators/payklik/authorizations` | BB-06; ADR-012 | Endpoint de laboratorio para verificar el simulador; los simuladores no son proveedores reales |
| `POST /api/v1/query/order-summary` | QRY-01; modelo de datos §3.5 | Ingesta temporal del modelo de lectura mientras BB-04 no existe; reemplazable por consumidor de eventos |
| Login de `frontend/` (`login.html`, `session.js`) | UI-01; nota explícita en el propio HTML | Simulación de interfaz; **no** es autenticación real. BB-07 la sustituye; no debe presentarse como seguridad implementada |
| `PATCH /api/v1/shipments/{id}/delivery-method` | CDU-07 A1 | Cambio de método de entrega posterior a la compra |
| `PATCH /api/v1/incidents/{id}/resolution` | CDU-10 E1 | Resolución manual mientras no existe integración por eventos |
| `POST /api/v1/integrations/erp/batches` | ERP-01 | Ejecución manual de la ventana para demostración y pruebas; la programada es de 30 minutos |
| `FiscalDocumentType.CREDIT_NOTE` | CDU-04 A1; CDU-10 paso 6 | Documento fiscal de devolución relacionado con la venta original |
| `shipment_dead_letters` | LOG-03; modelo de datos §3.4 | Cola local explícitamente temporal hasta que BB-04 provea la DLQ del broker |

## 5. Desviaciones documento ↔ código detectadas

Estas diferencias no bloquean la entrega, pero deben ratificarse o corregirse
para que la documentación no contradiga al código (riesgo señalado en la
planificación, sección 12).

| # | Documento | Código real | Recomendación |
|---|---|---|---|
| D1 | `STACK_TECNOLOGICO_FASE_2.md` §7 exige estructura `api/domain/application/adapter` por módulo | Los módulos usan paquetes planos (`FiscalService`, `ShipmentService`, etc.) | Ratificar la estructura plana en un ADR o actualizar el stack; el plan §4.1 permite adaptar nombres si las responsabilidades siguen visibles |
| D2 | `STACK_TECNOLOGICO_FASE_2.md` §3.3 prescribe React 19 + TypeScript + Vite + Vitest | `frontend/` es HTML/CSS/JS estático servido por nginx, sin build | Documentar la decisión de UI mínima del MVP (ya justificada en el README) y actualizar el stack o registrar ADR de excepción |
| D3 | Checklist de entrega exige que toda variable nueva aparezca en `.env.example` y README | `sgo-logic.inventory.reservation-ttl` y `expiration-sweep-interval` solo tienen valor por defecto en código | Agregar las variables a `.env.example`, Compose y README en la rama de inventario |
| D4 | Planificación menciona JUnit 5 | El BOM de Spring Boot 4.1.1 resuelve JUnit Jupiter 6.0.3 | Ratificar la versión administrada por el BOM (ya anotado en la bitácora) |
| D5 | Plan §5.4 asigna `integration` a Edson (Outbox) | `integration` contiene también adaptadores ERP/POS de Andy | Ya aclarado en el README; al implementar BB-04 usar subpaquetes (`integration/outbox`) para no mezclar propiedad |

## 6. Evidencia de verificación

- `./mvnw clean verify` con Java 25 (`eclipse-temurin:25.0.4_7-jdk-alpine-3.23`)
  y Testcontainers (PostgreSQL 17.11 + RabbitMQ 4.3.5): **BUILD SUCCESS**,
  33 clases, **127 pruebas, 0 fallos, 0 errores, 0 omitidas** (2026-09-23),
  incluida la regresión `AsyncIntegrationPersistenceTest` (bloqueo optimista
  de flujos asíncronos, ver REV-EDSON §7), las pruebas de Outbox
  (`OutboxEventTest`, `OutboxDispatcherTest` y `OutboxIntegrationTest`), las
  de seguridad (`SecurityAuthorizationTest`, `SensitiveDataSanitizerTest` y
  `SensitiveDataLeakScanTest` para SEC-01), las de auditoría
  (`AuditIntegrationTest` y `AuditQueryIntegrationTest`, esta última con la
  medida de EaC-02) y las de pagos (`PaymentAttemptServiceTest`,
  `PaymentIntegrationTest`, `ResilientPayKlikClientTest`,
  `PaymentIndeterminateIntegrationTest`, `PaymentReconciliationServiceTest` y
  `PaymentReconciliationIntegrationTest`).
- Migraciones Flyway ejecutadas contra PostgreSQL real durante la suite de
  integración: `V1`, `V2`, `V1001`–`V1004`, `V2001`–`V2003`.
- El detalle de la revisión cruzada está en
  `docs/evidence/functional/REV-EDSON-2026-09-23-revision-cruzada.md`.
