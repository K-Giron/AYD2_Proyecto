# Checklist de entrega de módulo — Edson Sosa (AUD-02)

Copia de `module-delivery-checklist.md` completada con evidencia local. No declara aceptación ni actualiza Trello.

## Identificación

- Tarjeta: AUD-02 (consulta indexada de evidencia).
- Módulo y propietario: `audit` — Edson Sosa.
- Rama o PR: `docs/rev-edson-cruzadas`; sin push ni PR por indicación del equipo.
- Revisor distinto del autor: pendiente (Jairo por la API y Kevin por la evidencia de acceso).
- Contratos implementados: CDU-12 (pasos 5 y 6, A1 y E1), ADR-006 y modelo de datos §3.7.

## Límites arquitectónicos

- [x] El cambio permanece dentro del módulo `audit`.
- [x] No accede a repositorios, entidades ni tablas de otros módulos.
- [x] No introduce dependencias nuevas entre módulos.
- [x] `ApplicationModules.verify()` y `ModularArchitectureTest` siguen en verde.
- [x] No mueve reglas de negocio a `shared`.
- [x] No modifica `pom.xml`, Compose ni configuración común.

## Datos y configuración

- [x] Reutiliza `audit_events` (`V3002`) con los índices por orden, agregado y correlación.
- [x] No se incluyeron credenciales reales.
- [x] No hay variables nuevas.
- [x] La evidencia de acceso no contiene PAN, CVV ni secretos.

## API y eventos

- [x] `GET /api/v1/audit/events` por `orderId` o por `aggregateType`+`aggregateId`; criterio inválido → `400 INVALID_AUDIT_QUERY`.
- [x] `GET /api/v1/audit/orders/{orderId}/export` en CSV con `Content-Disposition`.
- [x] Sin evidencia → `404 AUDIT_EVIDENCE_NOT_FOUND`, con `AUDIT_SEARCH_FAILED` registrado y alerta a Finanzas.
- [x] Acceso y exportación registrados (`AUDIT_EVIDENCE_ACCESSED`, `AUDIT_EVIDENCE_EXPORTED`) en transacción independiente.
- [x] `X-Correlation-Id` se propaga y queda en la evidencia.
- [x] Autorización: solo `FINANCE`.

## Verificación

- [x] `./mvnw clean verify` con Java 25: `BUILD SUCCESS`, 97 pruebas, 0 fallos, 0 errores, 0 omitidas.
- [x] `AuditQueryIntegrationTest` cubre consulta, acceso, búsqueda fallida, exportación y **EaC-02 < 1 s** sobre 10 000 eventos.
- [x] Smoke en Compose: consulta ~63 ms, CSV correcto, 404 con evidencia y 403 para `CLIENT`.
- [x] Se adjunta evidencia en la bitácora y en `docs/qa/trazabilidad-cun-mvp.md`.
- [x] Limitaciones documentadas: EaC-07 queda parcial hasta que pagos, fiscal y logística registren sus acciones.

## Resultado

- Decisión: **Corregir/Revisar antes de integrar**.
- Pendiente: revisión de Jairo (API) y Kevin (evidencia de acceso); que los demás módulos sumen sus acciones al registro.
- Evidencia: `BITACORA_FASE_2.md` (2026-09-23), `docs/qa/QA-03-matriz-eac.md` y reportes Surefire.
