# Checklist de entrega de módulo — Edson Sosa (PAY-01)

Copia de `module-delivery-checklist.md` completada con evidencia local. No declara aceptación ni actualiza Trello.

## Identificación

- Tarjeta: PAY-01 (puerto PayKlik e intentos de pago).
- Módulo y propietario: `payments` — Edson Sosa; consumidores futuros `orders` (ORD-03).
- Rama o PR: `docs/rev-edson-cruzadas`; sin push ni PR por indicación del equipo.
- Revisor distinto del autor: Jairo, pendiente (puerto de la Saga y contrato `PROPOSED`).
- Contratos implementados: CDU-03 pasos 2 y 3 y postcondición; ADR-003; ADR-012 (simulador); `saga-required-ports.md` (puerto Payments).

## Límites arquitectónicos

- [x] El cambio permanece en `payments` y su API pública `payments.api`.
- [x] No accede a repositorios, entidades ni tablas de otros módulos.
- [x] La API para la Saga usa `@NamedInterface("payments-api")`; el resto es interno.
- [x] `ApplicationModules.verify()` y `ModularArchitectureTest` siguen en verde.
- [x] No mueve reglas de negocio a `shared`.
- [x] No modifica `pom.xml`, Compose ni configuración común.

## Datos y configuración

- [x] `V3003__payments_create_tables.sql` se ejecuta contra PostgreSQL 17 real en la suite.
- [x] No se incluyeron credenciales reales.
- [x] No hay variables nuevas: reutiliza la configuración de BB-06.
- [x] No se registran PAN, CVV ni secretos; `PaymentRequest` rechaza esas referencias antes de llamar al proveedor.

## API y eventos

- [x] `POST /api/v1/payments/attempts` con `Idempotency-Key` (201 resultado terminal, 202 `PENDING`, 200 llave repetida).
- [x] `GET /api/v1/payments/attempts/{id}` y `GET /api/v1/payments/attempts?orderId=`.
- [x] `X-Correlation-Id` se propaga y se persiste en el intento y la evidencia.
- [x] `payments.api.PaymentPort` materializa el puerto de la Saga con orden, importe, referencia segura, correlación e idempotencia.
- [x] Un timeout no se reporta como aprobado; la frontera con PAY-02 queda documentada.

## Verificación

- [x] `./mvnw clean verify` con Java 25: `BUILD SUCCESS`, 107 pruebas, 0 fallos, 0 errores, 0 omitidas.
- [x] Pruebas positivas, negativas, de idempotencia (incluida concurrencia) y de ausencia de PAN/CVV.
- [x] Smoke en Compose: 201/200/400/403, auditoría y BD sin PAN/CVV.
- [x] Se adjunta evidencia en la bitácora y en `docs/qa/trazabilidad-cun-mvp.md`.
- [x] Limitaciones documentadas: sin circuit breaker, estado indeterminado, reintento ni conciliación (PAY-02/PAY-03).

## Resultado

- Decisión: **Corregir/Revisar antes de integrar**.
- Pendiente: revisión de Jairo del puerto Payments; PAY-02 y PAY-03.
- Evidencia: `BITACORA_FASE_2.md` (2026-09-23), `docs/diagrams/modelo-datos.md` §3.8 y reportes Surefire.
