# QA-02 — Línea base `T` y carga `8T` (DEC-02)

**Responsable:** Edson Sosa, con apoyo de Jairo Gómez
**Fecha:** 2026-09-23
**Tarjetas:** DEC-02 y QA-02
**Sustento:** EaC-01, EaC-02, EaC-03, ADR-001, ADR-006
**Protocolo:** `docs/qa/DEC-02-protocolo-carga-T-8T.md`

## 1. Ambiente y datos

| Elemento | Valor |
|---|---|
| Backend | Spring Boot 4.1.1, Java 25, una instancia en Docker Compose |
| Infraestructura | PostgreSQL 17.11 y RabbitMQ 4.3.5 locales |
| Cliente | `grafana/k6` con `--network host` |
| Datos | 500 proyecciones de orden y 10 000 eventos de auditoría (`tests/data/seed-load.sql`) |
| Pausa | 0.5 s por iteración; `T` = 10 VUs, `8T` = 80 VUs, 1 min cada uno |

`T` se documenta como la tasa estable medida en este ambiente local; no se
afirma tráfico productivo.

## 2. Resultados

### 2.1 Consulta consolidada (EaC-01, `consult-order-summary.js`)

| Escenario | Iteraciones | req/s | p50 | p95 | p99 | Errores | Umbral |
|---|---:|---:|---:|---:|---:|---:|---|
| `T` | 1 190 | 19.75 | 3.35 ms | 5.88 ms | 7.10 ms | 0.00 % | ✓ p95 < 5000 ms |
| `8T` | 9 600 | 158.86 | 2.07 ms | 4.22 ms | 8.30 ms | 0.00 % | ✓ p95 < 5000 ms |

100 % de las consultas por debajo de 5 s en ambos escenarios. `8T` es
≈ 8.04× la tasa de `T`.

### 2.2 Evidencia indexada (EaC-02, `audit-evidence.js`)

| Escenario | Iteraciones | req/s | p50 | p95 | p99 | Errores | Umbral |
|---|---:|---:|---:|---:|---:|---:|---|
| `T` | 1 180 | 19.59 | 5.86 ms | 17.20 ms | 28.90 ms | 0.00 % | ✓ p95 < 1000 ms |
| `8T` | 9 540 | 157.47 | 3.02 ms | 7.23 ms | 22.56 ms | 0.00 % | ✓ p95 < 1000 ms |

`8T` es ≈ 8.04× la tasa de `T`, con p95 muy por debajo de 1 s.

### 2.3 Recursos durante `8T` (`docker stats --no-stream`)

| Contenedor | CPU (consulta) | CPU (auditoría) | Memoria |
|---|---:|---:|---:|
| backend | 8.34 % | 21.20 % | ~497 MiB |
| postgres | 2.10 % | 6.08 % | ~59–61 MiB |
| rabbitmq | 0.07 % | 0.08 % | ~109 MiB |
| frontend | 0.00 % | 0.00 % | ~13 MiB |

## 3. Hallazgos corregidos durante la prueba

1. **Agotamiento del pool de conexiones en la consulta de auditoría.**
   La primera corrida de `T` sobre `audit-evidence.js` produjo p95 = 30 s y
   95.23 % de errores. Causa: `AuditQueryService` era `@Transactional` y
   escribía la evidencia de acceso con `REQUIRES_NEW`; cada solicitud
   retenía una conexión mientras esperaba otra, y con el pool por defecto
   (10) las 10 VUs se bloquearon entre sí. Corrección: los métodos de
   consulta ya no abren transacción propia (cada lectura usa la transacción
   corta del repositorio) y la evidencia se escribe en su propia transacción.
   Se agregó `AuditQueryIntegrationTest.concurrentQueriesDoNotExhaustTheConnectionPool`
   (16 hilos) como regresión. Además, la consulta por orden ahora excluye los
   eventos de acceso (`AuditEvidence`) para no mezclarlos con la evidencia de
   negocio.
2. **CPU de RabbitMQ en reposo.** El healthcheck de Compose ejecutaba
   `rabbitmq-diagnostics ping` cada 5 s; cada invocación lanza una VM Erlang
   y el contenedor consumía ~36 % de un núcleo sin tráfico (medido por
   `cgroup cpu.stat`). El intervalo se subió a 30 s y el consumo en reposo
   quedó en ~0 %.

## 4. Interpretación

- Con el conjunto declarado, `T` ≈ 20 solicitudes/s y `8T` ≈ 158–159
  solicitudes/s (≈ 8×) sin degradar las latencias ni la tasa de error, con
  una sola instancia de backend.
- EaC-01 queda **Cubierto** para la consulta consolidada existente y EaC-02
  mantiene su umbral con evidencia de carga.
- EaC-03 queda **Parcial**: falta el conjunto de módulos P0 (Orders, Saga y
  sus productores, ORD-01 a ORD-05) y un ambiente con balanceo para validar
  escalabilidad horizontal real.

## 5. Limitaciones

- Host de desarrollo, una instancia y sin balanceador; no sustituye una
  prueba de capacidad productiva.
- Los datos son sintéticos y la proyección consolidada se alimenta hoy por
  API (no hay productores reales de eventos hasta ORD-03).
- Los terceros simulados no participan en estos escenarios (se cubren en
  QA-05).
- La muestra de recursos es puntual; para el release candidate debe
  repetirse con el ambiente congelado.
