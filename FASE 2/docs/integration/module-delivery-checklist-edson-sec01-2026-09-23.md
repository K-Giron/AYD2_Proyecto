# Checklist de entrega de módulo — Edson Sosa (SEC-01)

Copia de `module-delivery-checklist.md` completada con evidencia local. No declara aceptación ni actualiza Trello.

## Identificación

- Tarjeta: SEC-01 (pruebas de autorización y fuga de PAN/CVV).
- Módulo y propietario: `security` (pruebas transversales) — Edson Sosa.
- Rama o PR: `docs/rev-edson-cruzadas`; sin push ni PR por indicación del equipo.
- Revisor distinto del autor: pendiente (Kevin por QA y Jairo por el cierre de EaC-12).
- Contratos implementados: EaC-12, R4, ADR-013, CDU-03 paso 3.

## Límites arquitectónicos

- [x] El cambio vive en pruebas de `security` y no modifica código de producción.
- [x] No accede a repositorios, entidades ni tablas de otros módulos fuera de las consultas de escaneo.
- [x] `ApplicationModules.verify()` y `ModularArchitectureTest` siguen en verde.
- [x] No mueve reglas de negocio a `shared`.
- [x] No modifica `pom.xml`, Compose ni configuración común.

## Datos y configuración

- [x] No hay migraciones nuevas.
- [x] No se incluyeron credenciales reales; el escaneo verifica que tampoco aparezcan PAN/CVV.
- [x] No hay variables nuevas.
- [x] El escaneo cubre base, payloads del Outbox, auditoría y logs.

## API y eventos

- [x] Sin token → `401 UNAUTHENTICATED`; rol insuficiente → `403 FORBIDDEN` en los grupos protegidos.
- [x] Referencias con PAN (13–19 dígitos) o CVV → `400 INVALID_PAYMENT_REQUEST` y no se persisten.
- [x] El escaneo incluye los eventos publicados por Outbox (`PaymentApproved`, `PaymentIndeterminate`, etc.).
- [x] `X-Correlation-Id` se mantiene en las respuestas de error.

## Verificación

- [x] `./mvnw clean verify` con Java 25: `BUILD SUCCESS`, 118 pruebas, 0 fallos, 0 errores, 0 omitidas.
- [x] `SensitiveDataLeakScanTest` verifica 0 apariciones y no vacuidad (columnas, logs, Outbox y auditoría).
- [x] `SecurityAuthorizationTest.roleMatrixRestrictsEndpoints` consolida la matriz 401/403.
- [x] Se adjunta evidencia en `docs/evidence/security/SEC-01-scan-pan-cvv.md` y en la bitácora.
- [x] Limitaciones documentadas: el escaneo no cubre colas en vuelo ni un agregador externo de logs.

## Resultado

- Decisión: **Corregir/Revisar antes de integrar**.
- Pendiente: revisión de Kevin (QA) y Jairo (cierre de EaC-12); QA-02/DEC-02.
- Evidencia: `docs/evidence/security/SEC-01-scan-pan-cvv.md`, `BITACORA_FASE_2.md` (2026-09-23) y reportes Surefire.
