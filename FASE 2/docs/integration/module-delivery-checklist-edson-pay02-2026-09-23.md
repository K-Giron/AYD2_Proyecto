# Checklist de entrega de módulo — Edson Sosa (PAY-02)

Copia de `module-delivery-checklist.md` completada con evidencia local. No declara aceptación ni actualiza Trello.

## Identificación

- Tarjeta: PAY-02 (timeout, circuit breaker y estado indeterminado).
- Módulo y propietario: `payments` — Edson Sosa.
- Rama o PR: `docs/rev-edson-cruzadas`; sin push ni PR por indicación del equipo.
- Revisor distinto del autor: pendiente (Jairo por el evento Outbox y la Saga).
- Contratos implementados: CDU-03 E1, ADR-003, ADR-002 (evento por Outbox) y `saga-required-ports.md`.

## Límites arquitectónicos

- [x] El cambio permanece en `payments`; el evento sale por la API pública de `integration.outbox`.
- [x] No accede a repositorios, entidades ni tablas de otros módulos.
- [x] `ApplicationModules.verify()` y `ModularArchitectureTest` siguen en verde.
- [x] No mueve reglas de negocio a `shared`.
- [x] `pom.xml` cambió solo para agregar `resilience4j-circuitbreaker` y `resilience4j-timelimiter` (módulos core, sin starter de plataforma), justificado por PAY-02.

## Datos y configuración

- [x] Reutiliza `payment_attempts` (`V3003`); no hay migración nueva.
- [x] No se incluyeron credenciales reales.
- [x] `SGO_PAYKLIK_*` aparece en `.env.example`, Compose, `application.yml` y README.
- [x] El payload del evento no contiene PAN, CVV ni secretos.

## API y eventos

- [x] Un timeout, una caída o el circuito abierto dejan el intento `INDETERMINATE` y nunca se reportan como aprobados.
- [x] `PaymentIndeterminate` se publica por Outbox en la misma transacción del cambio de estado.
- [x] `PaymentIndeterminate` usa el sobre común v1 y `aggregateType=PaymentAttempt`.
- [x] La respuesta REST expone el estado y el código de fallo (`PAYKLIK_TIMEOUT`, `PAYKLIK_UNAVAILABLE`, `PAYKLIK_CIRCUIT_OPEN`).

## Verificación

- [x] `./mvnw clean verify` con Java 25: `BUILD SUCCESS`, 112 pruebas, 0 fallos, 0 errores, 0 omitidas.
- [x] `ResilientPayKlikClientTest` cubre timeout, apertura del circuito y éxito.
- [x] `PaymentIndeterminateIntegrationTest` cubre 0 órdenes perdidas, evidencia auditable, evento por Outbox y `PAYKLIK_CIRCUIT_OPEN`.
- [x] Smoke en Compose con modo `TIMEOUT`: dos `INDETERMINATE` y un `CIRCUIT_OPEN` sin llamar al proveedor.
- [x] Limitaciones documentadas: el reintento/conciliación ≤ 60 s y la alerta a los 5 min pertenecen a PAY-03.

## Resultado

- Decisión: **Corregir/Revisar antes de integrar**.
- Pendiente: revisión de Jairo del evento `PaymentIndeterminate`; PAY-03.
- Evidencia: `BITACORA_FASE_2.md` (2026-09-23), `docs/qa/QA-03-matriz-eac.md` y reportes Surefire.
