# Checklist de entrega de módulo — Edson Sosa (AUD-01)

Copia de `module-delivery-checklist.md` completada con evidencia local. No declara aceptación ni actualiza Trello.

## Identificación

- Tarjeta: AUD-01 (registro auditable transversal).
- Módulo y propietario: `audit` — Edson Sosa; primer consumidor `incidents` (Kevin Girón).
- Rama o PR: `docs/rev-edson-cruzadas`; sin push ni PR por indicación del equipo.
- Revisor distinto del autor: pendiente (Jairo por la API transversal y Kevin por el consumo desde `incidents`).
- Contratos implementados: CDU-12, ADR-002/ADR-006/ADR-007 y modelo de datos §3.7.

## Límites arquitectónicos

- [x] El cambio permanece en `audit` y en una llamada declarada de `incidents`.
- [x] No accede a repositorios, entidades ni tablas de otros módulos.
- [x] La dependencia de `incidents` usa la API pública `audit.api`, declarada con `@NamedInterface`.
- [x] `ApplicationModules.verify()` y `ModularArchitectureTest` pasan con la excepción aprobada.
- [x] No mueve reglas de negocio a `shared`.
- [x] No modifica `pom.xml`, Compose ni configuración común.

## Datos y configuración

- [x] `V3002__audit_create_tables.sql` se ejecuta contra PostgreSQL 17 real en la suite.
- [x] No se incluyeron credenciales reales.
- [x] No hay variables nuevas: no se requiere configuración adicional.
- [x] La evidencia no registra PAN, CVV ni secretos; solo identificadores, acción, resultado y estados.

## API y eventos

- [x] `AuditRecorder.record(...)` exige transacción activa y escribe en la misma transacción del negocio.
- [x] Actor desde el contexto autenticado o `system`; correlación y causalidad persistidas.
- [x] Repositorio de solo anexado: extiende `Repository`, sin actualización ni borrado.
- [x] Índices por orden, agregado y correlación listos para la consulta de AUD-02.

## Verificación

- [x] `./mvnw clean verify` con Java 25: `BUILD SUCCESS`, 91 pruebas, 0 fallos, 0 errores, 0 omitidas.
- [x] Pruebas de atomicidad, rechazo sin transacción, actor autenticado y evidencia desde incidencias.
- [x] Se adjunta evidencia en la bitácora y en `docs/qa/trazabilidad-cun-mvp.md`.
- [x] Limitaciones documentadas: la consulta < 1 s, el registro de acceso, la búsqueda fallida y la exportación quedan para AUD-02.

## Resultado

- Decisión: **Corregir/Revisar antes de integrar**.
- Pendiente: revisión de Jairo (API transversal) y Kevin (consumo desde `incidents`); AUD-02.
- Evidencia: `BITACORA_FASE_2.md` (2026-09-23), `docs/diagrams/modelo-datos.md` §3.7 y reportes Surefire.
