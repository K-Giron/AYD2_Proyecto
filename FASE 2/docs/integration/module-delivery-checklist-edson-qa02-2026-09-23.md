# Checklist de entrega de módulo — Edson Sosa (DEC-02/QA-02)

Copia de `module-delivery-checklist.md` completada con evidencia local. No declara aceptación ni actualiza Trello.

## Identificación

- Tarjetas: DEC-02 (protocolo `T`/`8T`) y QA-02 (línea base y carga).
- Módulo y propietario: pruebas transversales de rendimiento — Edson Sosa.
- Rama o PR: `docs/rev-edson-cruzadas`; sin push ni PR por indicación del equipo.
- Revisor distinto del autor: pendiente (Jairo por ambiente/EaC-03 y Kevin por QA).
- Contratos implementados: EaC-01, EaC-02 y EaC-03; ADR-001 y ADR-006.

## Límites arquitectónicos

- [x] El cambio vive en pruebas, datos semilla y documentación; no altera módulos de negocio salvo la corrección del pool de auditoría.
- [x] No accede a tablas de otros módulos fuera de las consultas de siembra/escaneo.
- [x] `ApplicationModules.verify()` y `ModularArchitectureTest` siguen en verde.
- [x] No mueve reglas de negocio a `shared`.
- [x] `docker-compose.yml` cambió solo el intervalo del healthcheck de RabbitMQ, justificado por el hallazgo de CPU.

## Datos y configuración

- [x] `tests/data/seed-load.sql` es idempotente: 500 proyecciones y 10 000 eventos.
- [x] No se incluyeron credenciales reales.
- [x] No hay variables nuevas en la aplicación; los scripts usan `BASE_URL`, `VUS`, `DURATION` y credenciales demo.
- [x] No se registran PAN ni CVV en los datos de carga.

## API y eventos

- [x] Los escenarios usan los endpoints reales de consulta consolidada y evidencia con JWT.
- [x] No se modifican contratos de API ni de eventos.
- [x] Se conserva `X-Correlation-Id` en las peticiones.

## Verificación

- [x] `./mvnw clean verify` con Java 25: `BUILD SUCCESS`, 119 pruebas, 0 fallos, 0 errores, 0 omitidas.
- [x] `T` y `8T` medidos para ambos endpoints con p50/p95/p99, errores y recursos.
- [x] Hallazgos corregidos y con regresión: pool de conexiones en auditoría y healthcheck de RabbitMQ.
- [x] Evidencia en `docs/evidence/performance/QA-02-linea-base-y-8T.md` y protocolo en `docs/qa/DEC-02-protocolo-carga-T-8T.md`.
- [x] Limitaciones documentadas: una instancia, host de desarrollo y EaC-03 parcial hasta ORD-01 a ORD-05.

## Resultado

- Decisión: **Corregir/Revisar antes de integrar**.
- Pendiente: revisión de Jairo (ambiente/EaC-03) y Kevin (QA); completar EaC-03 con Orders/Saga.
- Evidencia: `docs/evidence/performance/QA-02-linea-base-y-8T.md`, `BITACORA_FASE_2.md` (2026-09-23) y reportes Surefire.
