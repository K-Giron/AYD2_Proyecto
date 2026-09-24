# Checklist de entrega de módulo — Edson Sosa (PAY-03)

Copia de `module-delivery-checklist.md` completada con evidencia local. No declara aceptación ni actualiza Trello.

## Identificación

- Tarjeta: PAY-03 (conciliación/reintento idempotente ≤ 60 s).
- Módulo y propietario: `payments` — Edson Sosa.
- Rama o PR: `docs/rev-edson-cruzadas`; sin push ni PR por indicación del equipo.
- Revisor distinto del autor: pendiente (Jairo por los eventos de pago y la Saga).
- Contratos implementados: CDU-03 E1, ADR-003, ADR-002 (eventos por Outbox) y `saga-required-ports.md`.

## Límites arquitectónicos

- [x] El cambio permanece en `payments`; los eventos salen por la API pública de `integration.outbox` y la alerta por `audit.api`.
- [x] No accede a repositorios, entidades ni tablas de otros módulos.
- [x] `ApplicationModules.verify()` y `ModularArchitectureTest` siguen en verde.
- [x] No mueve reglas de negocio a `shared`.
- [x] No modifica `pom.xml`, Compose ni configuración común salvo las variables nuevas justificadas por PAY-03.

## Datos y configuración

- [x] `V3004__payments_create_reconciliation_tables.sql` crea `payment_reconciliations` y agrega `payment_attempts.payment_reference`; se ejecuta contra PostgreSQL 17 real.
- [x] No se incluyeron credenciales reales.
- [x] `SGO_PAYMENT_RECONCILIATION_*` aparece en `.env.example`, Compose, `application.yml` y README.
- [x] `payment_reference` es una referencia segura validada; no se almacenan PAN completo ni CVV.

## API y eventos

- [x] El primer trabajo se programa dentro de los 60 s (`first-delay` por defecto 30 s) con un único trabajo abierto por intento.
- [x] La conciliación reutiliza la misma `Idempotency-Key` y referencia del intento: no genera un segundo cobro.
- [x] Al resolver publica `PaymentApproved`/`PaymentRejected` por Outbox en la misma transacción; el payload no contiene PAN/CVV.
- [x] Al agotar reintentos o superar 5 min queda `DEAD_LETTER` con alerta auditable `PAYMENT_RECONCILIATION_ALERT`.
- [x] Reclamo con `FOR UPDATE SKIP LOCKED`: varios workers no concilian dos veces el mismo intento.

## Verificación

- [x] `./mvnw clean verify` con Java 25: `BUILD SUCCESS`, 116 pruebas, 0 fallos, 0 errores, 0 omitidas.
- [x] `PaymentReconciliationServiceTest`: misma llave, no-op si el intento ya está resuelto y alerta al agotar.
- [x] `PaymentReconciliationIntegrationTest`: resolución sin segundo cobro ni segundo evento.
- [x] `PaymentIndeterminateIntegrationTest` verifica el delay de programación ≤ 60 s.
- [x] Smoke en Compose: resolución `APPROVED` con evento y, con `TIMEOUT`, bandeja de fallidos sin perder la orden.
- [x] Limitaciones documentadas: la alerta es un log y un registro auditable, sin canal externo de notificaciones en el MVP.

## Resultado

- Decisión: **Corregir/Revisar antes de integrar**.
- Pendiente: revisión de Jairo de los eventos de pago; EaC-04 cerrado.
- Evidencia: `BITACORA_FASE_2.md` (2026-09-23), `docs/qa/QA-03-matriz-eac.md` y reportes Surefire.
