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
| ADR-010, confirmación fiscal asíncrona | `Proposed` | Andy, Edson y Jairo | Integración FIS-02 con ORD-03 |
| ADR-012, contratos de simuladores | `Proposed` | Andy, Edson, Kevin y Jairo | Ratificación de BB-06 |

## Implementaciones locales contra contratos propuestos

| Implementación | Estado local | Pendiente antes de integración |
|---|---|---|
| Puerto y flujo fiscal SAT | Implementado y probado por Andy | Revisión de Edson, ratificación de Jairo y conexión con la Saga |
| Simuladores PayKlik, SAT, ERP, POS, LogiExpress y notificaciones | Implementados y configurables | Ratificar ADR-012; no equivalen a integración con proveedores reales |
| Lotes ERP y distribución POS | Implementados con persistencia, acuse, deduplicación y reintentos | Integrar BB-04/Outbox y ejecutar pruebas con PostgreSQL/RabbitMQ reales |

## Uso permitido de la línea base

- Los documentos `PROPOSED` sirven para revisión, estimación y preparación de pruebas.
- No autorizan implementación interna en módulos ajenos.
- Una implementación experimental debe indicar expresamente contra qué versión propuesta trabaja.
- Ningún documento pasa a `ACCEPTED` sin evidencia de aprobación.
- Si una propuesta cambia de forma incompatible, se crea una nueva versión y se registra el impacto.

## Evidencia de aprobación

| Documento | Integrante | Decisión | Fecha | Enlace o captura |
|---|---|---|---|---|
| Pendiente | Pendiente | Pendiente | Pendiente | Pendiente |
