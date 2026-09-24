# Estado de contratos de integración

**Estado del documento:** `PROPOSED`

**Responsable de consolidación:** Jairo Gómez

Esta tabla permite trabajar contra una línea base visible sin presentar como aprobado aquello que todavía requiere acuerdo del equipo.

| Contrato o decisión | Versión/estado | Propietarios que deben revisar | Bloquea |
|---|---|---|---|
| Convenciones HTTP | v1 / `PROPOSED` | Todos; seguridad por Edson | Endpoints funcionales |
| Sobre común de eventos | v1 / `PROPOSED` | Todos; Outbox por Edson | Productores y consumidores |
| Estados de orden | v1 / `PROPOSED` | Jairo y todos los módulos participantes | ORD-02 y Saga |
| Diseño de carrito ORD-01 | Borrador / `PROPOSED` | Jairo, Jorge y Kevin | Implementación ORD-01 |
| Diseño de máquina ORD-02 | Borrador / `PROPOSED` | Jairo, Edson y Andy | Implementación ORD-02 |
| Puertos requeridos por la Saga | Borrador / `PROPOSED` | Jairo, Jorge, Edson, Andy y Kevin | ORD-03 |
| ADR-009 | `Proposed` | Al menos tres integrantes | Cierre BB-02 y BB-05 |
| ADR-010, confirmación fiscal asíncrona | `Proposed`; aprobación de Edson registrada (2026-09-23); faltan Jairo | Andy, Edson y Jairo | Integración FIS-02 con ORD-03 |
| ADR-012, contratos de simuladores | `Proposed`; aprobación de Edson (2026-09-23) y Kevin (2026-09-24) registradas; falta Jairo | Andy, Edson, Kevin y Jairo | Ratificación de BB-06 |
| ADR-013, autenticación JWT, roles y sanitización | `Proposed`; decisión JWT del equipo (2026-09-23) y aprobación de Kevin (2026-09-24) registradas; faltan Jairo y Andy | Edson, Jairo, Andy y Kevin | Implementación BB-07 y SEC-01 |
| ADR-014, algoritmo explicable de origen de despacho | `Proposed`; aprobación de Kevin (2026-09-24) registrada; faltan Jairo, Edson y Andy | Jairo, Edson, Andy y Kevin | INV-04 y el puerto de despacho de LOG-01 |

## Implementaciones locales contra contratos propuestos

| Implementación | Estado local | Pendiente antes de integración |
|---|---|---|
| Puerto y flujo fiscal SAT | Implementado, probado por Andy y **revisado por Edson el 2026-09-23** | Ratificación de Jairo y conexión con la Saga |
| Simuladores PayKlik, SAT, ERP, POS, LogiExpress y notificaciones | Implementados, configurables y **revisados por Edson el 2026-09-23** | Ratificar ADR-012 con Kevin y Jairo; no equivalen a integración con proveedores reales |
| Lotes ERP y distribución POS | Implementados con persistencia, acuse, deduplicación y reintentos; `clean verify` con PostgreSQL/RabbitMQ reales ejecutado en la revisión | Corregir A3 (reclamo atómico de ventas) y definir A1 (fan-out POS); integrar BB-04/Outbox |
| Inventario, reservas y origen de despacho | Implementado y **revisado por Edson el 2026-09-23** | Externalizar configuración (J2); registrar riesgo J1 para ORD-04 |
| Outbox/Inbox transversal (BB-04) | Implementado y probado por Edson contra el sobre v1 `PROPOSED` (`V3001`, `integration.outbox`) | Revisión de Jairo de la API pública; ratificación del sobre; conexión con ORD-03 |
| Autenticación JWT, roles y sanitización (BB-07) | Implementado y probado por Edson: login, 401/403 por rol y `SensitiveDataSanitizer` | Revisión de la matriz de autorización por Jairo/Andy/Kevin; ratificar ADR-013 |
| Intentos de pago, resiliencia y conciliación (PAY-01 a PAY-03) | Implementado y probado por Edson contra el puerto de BB-06 y el contrato `PROPOSED` de la Saga (`payment_attempts`/`payment_reconciliations`, `V3003`/`V3004`): timeout, circuit breaker, estado indeterminado, eventos por Outbox y conciliación idempotente ≤ 60 s sin segundo cobro | Ratificación del puerto Payments en `saga-required-ports.md` por Jairo |

La revisión cruzada completa, con fortalezas, hallazgos y evidencia, está en
`docs/evidence/functional/REV-EDSON-2026-09-23-revision-cruzada.md`. Ninguna
fila de esta tabla convierte un contrato `PROPOSED` en `ACCEPTED`.

## Uso permitido de la línea base

- Los documentos `PROPOSED` sirven para revisión, estimación y preparación de pruebas.
- No autorizan implementación interna en módulos ajenos.
- Una implementación experimental debe indicar expresamente contra qué versión propuesta trabaja.
- Ningún documento pasa a `ACCEPTED` sin evidencia de aprobación.
- Si una propuesta cambia de forma incompatible, se crea una nueva versión y se registra el impacto.

## Evidencia de aprobación

| Documento | Integrante | Decisión | Fecha | Enlace o captura |
|---|---|---|---|---|
| ADR-009 | Edson Sosa | Aprobar (revisión de stack y topología) | 2026-09-23 | `docs/evidence/functional/REV-EDSON-2026-09-23-revision-cruzada.md` |
| ADR-010 | Edson Sosa | Aprobar (resiliencia y compuerta fiscal) | 2026-09-23 | `docs/evidence/functional/REV-EDSON-2026-09-23-revision-cruzada.md` |
| ADR-012 | Edson Sosa | Aprobar (contratos de simuladores y protección de datos) | 2026-09-23 | `docs/evidence/functional/REV-EDSON-2026-09-23-revision-cruzada.md` |
| ADR-009 | Kevin Girón | Aprobar (stack/topología; sin objeciones desde logistics/notifications/query/incidents/frontend) | 2026-09-24 | `BITACORA_FASE_2.md`, entrada de Kevin del 2026-09-24 |
| ADR-012 | Kevin Girón | Aprobar (revisé `SimulatedLogiExpressAdapter`/`SimulatedNotificationProviderAdapter` migrados a `SimulatorMode`: preservan la semántica TIMEOUT/OUTAGE que LOG-03/NOT-01 necesitan) | 2026-09-24 | `logistics/SimulatedLogiExpressAdapter.java`, `notifications/SimulatedNotificationProviderAdapter.java` |
| ADR-013 | Kevin Girón | Aprobar (matriz de roles verificada contra `SecurityConfig`: `/api/v1/shipments`, `/api/v1/notifications` e `/api/v1/incidents` quedan correctos para CLIENT/SUPPORT/LOGISTICS; `login.html`/`session.js` ya envían el JWT real) | 2026-09-24 | `security/SecurityConfig.java`, `frontend/session.js` |
| ADR-014 | Kevin Girón | Aprobar (el puerto de despacho de `saga-required-ports.md` que soy dueño de proveer ya recibe `origin`/`originCode` calculados por este algoritmo; no exige nada adicional de Logistics) | 2026-09-24 | `logistics/api/DispatchRequest.java`, `orders/OrderSagaService.java` |
| ADR-009 | Jairo, Jorge y Andy | Pendiente | — | — |
