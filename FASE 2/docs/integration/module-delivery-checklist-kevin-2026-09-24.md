# Checklist de entrega de módulo — Kevin Girón (segunda entrega)

Copia de `module-delivery-checklist.md` para la entrega del 2026-09-24, posterior a la integración masiva del 2026-09-23 (Saga, Outbox/Inbox, JWT, auditoría, pagos, fiscal/ERP/POS). Complementa, no reemplaza, `module-delivery-checklist-kevin-2026-09-22.md`.

## Identificación

- Tarjeta Kanban: pendiente de actualizar en Trello.
- Módulo y propietario: `logistics`, `notifications`, `query` — Kevin Girón (extensión de la entrega del 22 de septiembre, ya integrada en `develop` vía PR #5).
- Rama: `feature/kevin-refinamiento-post-integracion` (local, sin publicar).
- Revisor distinto del autor: **pendiente**, igual que la entrega anterior.
- Contratos implementados: `SHIPMENT_STATUS_CHANGED` e `INCIDENT_REGISTERED`, eventos nuevos sobre el sobre v1 (`PROPOSED`) publicados por Logistics e Incidents y consumidos por Query y Notifications.

## Qué cambió en esta entrega

1. **Se revisó el estado real del repositorio** tras el pull: 74 commits nuevos de Edson (pagos, auditoría, JWT, Outbox), Jairo (carrito, Saga, E2E) y Andy (fiscal/ERP/POS/simuladores). Se confirmó que el PR #5 de Kevin ya estaba mergeado a `develop`.
2. **Se revisaron y aprobaron los ADR pendientes de Kevin**: ADR-009, ADR-012, ADR-013 y ADR-014 (ver `docs/contracts/contract-status.md`, tabla "Evidencia de aprobación").
3. **Se cerró la brecha señalada explícitamente en la bitácora** ("CUN-11 continúa parcial hasta recibir eventos de despacho/incidencias... y disponer de DLQ del trabajador"):
   - `ShipmentService` (logistics) y `IncidentService` (incidents) ahora publican eventos por el Outbox real (`integration.outbox.PublishEvent`) en la misma transacción del cambio de negocio.
   - `NotificationService` pasó de un reintento único a la misma secuencia 2/8/32 s que LOG-03, con `notification_dead_letters` (DLQ del trabajador) al agotar intentos.
   - Dos consumidores nuevos en `notifications` (`ShipmentStatusNotificationConsumer`, `IncidentNotificationConsumer`) resuelven el cliente mediante una tabla local `order_customer_links` (alimentada por el propio consumidor de `ORDER_STATUS_CHANGED`), porque Logistics/Incidents no conocen ni deben conocer `customerId`.
4. **Se cerró la brecha equivalente en QRY-01** (CUN-09, señalada en `QA-06-gate-trazabilidad-penalizaciones.md`: "faltan productores de... despacho, entrega e incidencia"): dos consumidores nuevos en `query` (`ShipmentStatusChangedConsumer`, `IncidentRegisteredConsumer`) proyectan `dispatchStatus`/`deliveryStatus`/`incidentStatus` automáticamente.
5. **Se corrigió una condición de carrera real** que la propia integración destapó: al envolver `ShipmentService.createDispatch`/`changeDeliveryMethod` en `@Transactional` (necesario para que `PublishEvent` tenga una transacción activa), la primera solicitud de guía podía programarse y ejecutarse en otro hilo antes del commit, sin encontrar el despacho. Se aplicó el mismo patrón *after-commit* que ya usaba `NotificationService.dispatchAfterCommit`.
6. **Se corrigió un bug de prueba propio** (no de producción): `NotificationServiceTest` comparaba IDs de dos objetos distintos por el mismo problema que ya se había corregido en `ShipmentServiceTest` el 22 de septiembre.

## Límites arquitectónicos

- [x] El cambio permanece dentro de los módulos propietarios (`logistics`, `notifications`, `query`).
- [x] No accede a repositorios, entidades o tablas de otro módulo — `order_customer_links` es una tabla propia de `notifications` que resuelve localmente lo que Logistics/Incidents no pueden conocer.
- [x] Las dependencias nuevas usan API pública aprobada: `integration.outbox.PublishEvent`/`OutboxEventRequest` (`@NamedInterface("outbox")`) y `messaging.BusinessEventEnvelope`/`InboxService` (ya usados por el resto del equipo).
- [x] No introduce ciclos según Spring Modulith (`ApplicationModules.verify()` en verde).
- [x] Se amplió `ModularArchitectureTest` para agregar `"integration"` a la lista permitida de `incidents` (antes solo tenía `audit`); `logistics` ya tenía `integration` permitido desde la ratificación del 23 de septiembre.
- [x] No modifica `pom.xml` ni Compose en esta entrega.

## Datos y configuración

- [x] Migración nueva propia del módulo: `V1005__notifications_create_dead_letters_and_order_links.sql`, dentro del rango V1001-V1099 ratificado el 23 de septiembre.
- [x] No se incluyeron credenciales reales.
- [x] No se registran cuerpos, tokens, PAN, CVV ni datos sensibles (estos módulos no manejan datos de pago).

## API y eventos

- [x] Eventos nuevos (`SHIPMENT_STATUS_CHANGED`, `INCIDENT_REGISTERED`) usan el sobre común (`aggregateType`, `aggregateId`, `eventType`, `eventVersion`, `correlationId`, `payload`) sobre el contrato v1 `PROPOSED`.
- [x] `X-Correlation-Id`/`correlationId` se propagan hasta el evento publicado.
- [ ] Los eventos de despacho/incidencia todavía no tienen su propia página en `docs/contracts/events/`; documentados por ahora en el Javadoc de `ShipmentService`/`IncidentService` y en este checklist.

## Verificación

- [x] `./mvnw clean verify` termina en `BUILD SUCCESS` dentro de un contenedor `eclipse-temurin:25.0.4_7-jdk` con Testcontainers reales (Postgres/RabbitMQ). Ver el conteo exacto en la entrada de bitácora del 2026-09-24.
- [x] Pruebas nuevas: `ShipmentServiceTest` (+2), `IncidentServiceTest` (+1 aserción de evento), `NotificationServiceTest` (retry/DLQ reescrito), `ShipmentStatusChangedConsumerTest`, `IncidentRegisteredConsumerTest` (query), más las positivas/negativas ya existentes.
- [ ] No se repitió el smoke de `docker compose up` con RabbitMQ real end-to-end para los dos eventos nuevos (sí se verificó dentro de `./mvnw clean verify`, que usa Testcontainers reales, pero no en el Compose completo). Pendiente antes de aceptar.

## Resultado

- Decisión: **Corregir** — igual que la entrega anterior, sigue faltando revisor distinto del autor y el smoke completo en Compose.
- Observaciones: esta entrega es principalmente integración y cierre de brechas ya señaladas por el equipo (bitácora de Edson/Jairo, QA-06), no funcionalidad nueva inventada. Todo lo agregado tiene evidencia de prueba.
- Fecha y evidencia: 2026-09-24; ver `BITACORA_FASE_2.md` (entrada de Kevin), `docs/contracts/contract-status.md` y `docs/qa/QA-06-gate-trazabilidad-penalizaciones.md`.
