# Checklist de entrega de módulo — Edson Sosa

Copia de `module-delivery-checklist.md` completada con evidencia local. No declara aceptación ni actualiza Trello.

## Identificación

- Tarjeta: BB-04 (Outbox transaccional, dispatcher, reintento y DLQ).
- Módulo y propietario: `integration` (subpaquete público `integration.outbox`) — Edson Sosa.
- Rama o PR: `docs/rev-edson-cruzadas`; sin push ni PR por indicación del equipo.
- Revisor distinto del autor: Jairo Gómez, pendiente (la API pública `integration.outbox` toca un contrato común).
- Contratos implementados: sobre de eventos `docs/contracts/events/event-envelope-v1.md` (`PROPOSED`) y ADR-002.

## Límites arquitectónicos

- [x] El cambio permanece dentro del módulo propietario (`integration.outbox` + migración `V3001`).
- [x] No accede a repositorios, entidades, adaptadores ni tablas de otro módulo.
- [x] La dependencia de otros módulos se limita a la API pública `integration.outbox`, declarada con `@NamedInterface("outbox")`.
- [x] `ApplicationModules.verify()` y `ModularArchitectureTest` pasan con la excepción aprobada.
- [x] No mueve reglas de negocio a `shared`.
- [x] Los cambios comunes de configuración se justifican por BB-04 y quedan externalizados.

## Datos y configuración

- [x] `V3001__integration_create_outbox_tables.sql` se ejecuta contra PostgreSQL 17 real en la suite.
- [x] No se incluyeron credenciales reales.
- [x] `SGO_OUTBOX_*` aparece en `.env.example`, Compose, `application.yml` y README.
- [x] No se registran cuerpos, tokens, PAN ni CVV; solo identificadores y tipos de evento.

## API y eventos

- [x] `PublishEvent.publish(...)` exige transacción activa y escribe en la misma transacción del negocio.
- [x] El dispatcher publica el sobre con `eventId`, `eventType`, `eventVersion`, `occurredAt`, correlación, agregado y payload.
- [x] Cabeceras AMQP `X-Correlation-Id` y `X-Event-Type`; `messageId = eventId`; entrega persistente.
- [x] Reintentos 2/8/32 s, estado `FAILED` y topología `sgo.events` / `sgo.events.dlx` / `sgo.events.dlq`.
- [x] `IdempotentConsumer` deduplica reenvíos con `UNIQUE (consumer, event_id)` e `INSERT ... ON CONFLICT DO NOTHING`.
- [x] El contrato del sobre permanece rotulado `PROPOSED`.

## Verificación

- [x] `./mvnw clean verify` con Java 25: `BUILD SUCCESS`, 74 pruebas, 0 fallos, 0 errores, 0 omitidas.
- [x] Pruebas de atomicidad, publicación real, concurrencia `SKIP LOCKED`, deduplicación y backoff.
- [x] Smoke en Compose: evento `PENDING` → `PUBLISHED` y sobre capturado desde RabbitMQ.
- [x] Se adjunta evidencia en la bitácora y en `docs/qa/trazabilidad-cun-mvp.md`.
- [x] Limitaciones documentadas: ningún flujo de negocio consume todavía el Outbox (depende de ORD-03); el contrato del sobre sigue `PROPOSED`.

## Resultado

- Decisión: **Corregir/Revisar antes de integrar**.
- Pendiente: revisión de Jairo sobre la API pública y el `ModularArchitectureTest`; push/PR cuando el equipo lo autorice.
- Evidencia: `BITACORA_FASE_2.md` (2026-09-23), `docs/qa/trazabilidad-cun-mvp.md` y reportes Surefire.
