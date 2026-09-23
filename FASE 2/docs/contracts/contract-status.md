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
| ADR-012, contratos de simuladores | `Proposed`; aprobación de Edson registrada (2026-09-23); faltan Kevin y Jairo | Andy, Edson, Kevin y Jairo | Ratificación de BB-06 |

## Implementaciones locales contra contratos propuestos

| Implementación | Estado local | Pendiente antes de integración |
|---|---|---|
| Puerto y flujo fiscal SAT | Implementado, probado por Andy y **revisado por Edson el 2026-09-23** | Ratificación de Jairo y conexión con la Saga |
| Simuladores PayKlik, SAT, ERP, POS, LogiExpress y notificaciones | Implementados, configurables y **revisados por Edson el 2026-09-23** | Ratificar ADR-012 con Kevin y Jairo; no equivalen a integración con proveedores reales |
| Lotes ERP y distribución POS | Implementados con persistencia, acuse, deduplicación y reintentos; `clean verify` con PostgreSQL/RabbitMQ reales ejecutado en la revisión | Corregir A3 (reclamo atómico de ventas) y definir A1 (fan-out POS); integrar BB-04/Outbox |
| Inventario, reservas y origen de despacho | Implementado y **revisado por Edson el 2026-09-23** | Externalizar configuración (J2); registrar riesgo J1 para ORD-04 |

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
| ADR-009 | Jairo, Jorge, Andy y Kevin | Pendiente | — | — |
