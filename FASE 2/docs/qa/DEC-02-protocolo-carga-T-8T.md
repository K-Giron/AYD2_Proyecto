# DEC-02 — Protocolo de línea base `T` y carga `8T`

**Responsable:** Edson Sosa, con apoyo de Jairo Gómez
**Fecha:** 2026-09-23
**Sustento:** EaC-01, EaC-02, EaC-03, ADR-001, ADR-006 y stack tecnológico §3.4 (k6)

## 1. Objetivo

Cerrar DEC-02 ("no existe línea base `T` para demostrar 8x") con un
procedimiento reproducible que:

1. Defina `T` como una tasa estable medida en el mismo ambiente local, sin
   afirmar tráfico productivo.
2. Ejecute `8T` sobre los endpoints consolidados que ya existen.
3. Registre p50, p95 y p99, tasa de error y consumo de recursos.

## 2. Ambiente

| Elemento | Valor |
|---|---|
| Backend | Spring Boot 4.1.1, Java 25, imagen `sgo-logic-backend:0.0.1-phase2` |
| Infraestructura | Docker Compose: PostgreSQL 17.11, RabbitMQ 4.3.5, backend y frontend |
| Cliente de carga | `grafana/k6` en contenedor con `--network host` |
| Host | Máquina de desarrollo del equipo (no productiva) |
| Configuración | Valores por defecto del repositorio (`SUCCESS` en simuladores) |

La topología es de una sola instancia; no se afirma redundancia ni
capacidad productiva. `N` instancias se dimensionarán con esta misma prueba
en un ambiente equivalente.

## 3. Conjunto de datos declarado

`tests/data/seed-load.sql` carga de forma idempotente:

- 500 proyecciones en `order_summary_projections` (órdenes
  `00000000-0000-0000-0000-000000000001` a `...000500`).
- 10 000 eventos en `audit_events` (20 por orden) para la consulta de
  evidencia.

Los datos son sintéticos y se regeneran con el script; no representan
tráfico real.

## 4. Escenarios y tasas

| Escenario | Script | VUs | Duración | Descripción |
|---|---|---|---|---|
| `T` | `consult-order-summary.js` | 10 | 1 min | Consulta consolidada (EaC-01) |
| `8T` | `consult-order-summary.js` | 80 | 1 min | Ocho veces la línea base |
| `T` | `audit-evidence.js` | 10 | 1 min | Evidencia por orden (EaC-02) |
| `8T` | `audit-evidence.js` | 80 | 1 min | Ocho veces la línea base |

`T` se define como la tasa estable alcanzada con 10 VUs y 0.5 s de pausa
(≈ 20 solicitudes/s teóricas). `8T` usa 80 VUs con la misma pausa.

## 5. Métricas y umbrales

| Métrica | Umbral |
|---|---|
| `http_req_duration` p(95) consulta consolidada | < 5000 ms (EaC-01) |
| `http_req_duration` p(95) evidencia | < 1000 ms (EaC-02) |
| `http_req_failed` | < 1 % |
| p50, p95 y p99 | Se reportan siempre |
| Recursos | `docker stats` sin streaming durante la carga |

## 6. Procedimiento

```bash
cd "FASE 2"
set -a; source .env; set +a

# 1. Datos declarados
docker exec -i sgo-logic-postgres-1 psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  < tests/data/seed-load.sql

# 2. Línea base T
docker run --rm --network host -v "$PWD/tests/k6:/scripts" \
  -e BASE_URL=http://localhost:8080 -e VUS=10 -e DURATION=1m \
  grafana/k6 run --summary-trend-stats="avg,min,med,p(50),p(90),p(95),p(99),max" \
  /scripts/consult-order-summary.js

# 3. Carga 8T
docker run --rm --network host -v "$PWD/tests/k6:/scripts" \
  -e BASE_URL=http://localhost:8080 -e VUS=80 -e DURATION=1m \
  grafana/k6 run --summary-trend-stats="avg,min,med,p(50),p(90),p(95),p(99),max" \
  /scripts/consult-order-summary.js

# 4. Repetir 2 y 3 con /scripts/audit-evidence.js
```

Durante cada corrida se captura `docker stats --no-stream` para registrar
CPU y memoria de los contenedores.

## 7. Limitaciones

- EaC-03 exige el conjunto de módulos P0; Orders, Saga y sus productores
  siguen pendientes (ORD-01 a ORD-05), por lo que `8T` se mide sobre los
  endpoints consolidados disponibles y se documenta como evidencia parcial.
- Una sola instancia de backend y sin balanceador: no se evalúa
  escalabilidad horizontal real.
- El simulador de PayKlik y los demás terceros no participan en estos
  escenarios; su latencia se prueba en QA-05.
- Los resultados dependen del host de desarrollo y no sustituyen una prueba
  de capacidad productiva.
