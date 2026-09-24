# Pruebas de carga — QA-02 / DEC-02

Scripts k6 para la línea base `T` y la carga `8T` sobre los endpoints
consolidados existentes. El protocolo completo está en
`docs/qa/DEC-02-protocolo-carga-T-8T.md`.

## Requisitos

- Stack levantado con `docker compose up -d --build` desde `FASE 2`.
- Imagen `grafana/k6` (se descarga la primera vez).

## Datos declarados

```bash
cd "FASE 2"
set -a; source .env; set +a
docker exec -i sgo-logic-postgres-1 psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  < tests/data/seed-load.sql
```

Carga 500 proyecciones de orden y 10 000 eventos de auditoría (20 por orden).

## Ejecución

```bash
# T (10 VUs, 1 min) — consulta consolidada (EaC-01)
docker run --rm --network host -v "$PWD/tests/k6:/scripts" \
  -e BASE_URL=http://localhost:8080 -e VUS=10 -e DURATION=1m \
  grafana/k6 run --summary-trend-stats="avg,min,med,p(50),p(90),p(95),p(99),max" \
  /scripts/consult-order-summary.js

# 8T (80 VUs, 1 min)
docker run --rm --network host -v "$PWD/tests/k6:/scripts" \
  -e BASE_URL=http://localhost:8080 -e VUS=80 -e DURATION=1m \
  grafana/k6 run --summary-trend-stats="avg,min,med,p(50),p(90),p(95),p(99),max" \
  /scripts/consult-order-summary.js

# Evidencia (EaC-02): repetir con /scripts/audit-evidence.js
```

Variables soportadas: `BASE_URL`, `VUS`, `DURATION`, `PAUSE`, `ORDERS` y las
credenciales de cliente/finanzas (`CLIENT_*`, `FINANCE_*`).

## Umbrales

| Escenario | p(95) | Errores |
|---|---|---|
| Consulta consolidada | < 5000 ms | < 1 % |
| Evidencia | < 1000 ms | < 1 % |

Los resultados se registran en `docs/evidence/performance/QA-02-linea-base-y-8T.md`.
